package com.workit.domain.transaction.service;

import com.workit.domain.transaction.dto.response.*;
import com.workit.domain.transaction.exception.TransactionErrorCode;
import com.workit.domain.transaction.mapper.TransactionMapper;
import com.workit.domain.transaction.vo.TransactionVO;
import com.workit.domain.transaction.vo.TransactionReviewAction;
import com.workit.domain.wallet.mapper.WalletMapper;
import com.workit.exception.BusinessException;
import com.workit.global.dto.PageResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 거래 기록·조회 담당(내역/집계/상세/영수증) + 결제 취소.
 * 충전/환불/결제 오케스트레이션은 payment 도메인(PaymentService)으로 이관됨.
 */
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    private final TransactionMapper transactionMapper;
    private final WalletMapper walletMapper;

    @Override
    public PageResponseDTO<TransactionListItemResponse> getTransactions(Long userId, String startDate, String endDate,
                                                             String paymentSourceType, String transactionType, Long cardId,
                                                             int page, int size) {
        // 잘못된 페이징 값이 들어와도 목록이 깨지지 않도록 보정
        int safePage = Math.max(page, 0);
        int safeSize = (size < 1 || size > MAX_PAGE_SIZE) ? DEFAULT_PAGE_SIZE : size;

        long totalElements = transactionMapper.countTransactions(
                userId, startDate, endDate, paymentSourceType, transactionType, cardId
        );

        if (totalElements == 0) {
            return PageResponseDTO.of(Collections.emptyList(), safePage, safeSize, 0);
        }

        List<TransactionListItemResponse> content = transactionMapper.findTransactions(
                        userId, startDate, endDate, paymentSourceType, transactionType, cardId,
                        safePage * safeSize, safeSize
                )
                .stream()
                .map(TransactionListItemResponse::from)
                .collect(Collectors.toList());

        return PageResponseDTO.of(content, safePage, safeSize, totalElements);
    }

    @Override
    public TransactionSummaryResponse getTransactionSummary(Long userId, String startDate, String endDate,
                                                             String paymentSourceType, Long cardId) {
        return TransactionSummaryResponse.from(
                transactionMapper.findTransactionSummary(userId, startDate, endDate, paymentSourceType, cardId)
        );
    }

    @Override
    public TransactionDetailResponse getTransactionDetail(Long userId, Long transactionId) {
        TransactionVO transaction = transactionMapper.findTransactionDetailById(transactionId, userId);
        if (transaction == null) {
            throw new BusinessException(TransactionErrorCode.TRANSACTION_NOT_FOUND);
        }

        boolean reviewSupported = "RESTAURANT".equals(transaction.getMerchantCategory())
                || "ACTIVITY".equals(transaction.getMerchantCategory());
        LocalDateTime reviewDeadline = reviewSupported
                ? transaction.getApprovedAt().plusDays(30)
                : null;
        TransactionReviewAction reviewAction = findReviewAction(
                transaction,
                reviewSupported,
                reviewDeadline
        );

        return TransactionDetailResponse.from(transaction, reviewAction, reviewDeadline);
    }

    private TransactionReviewAction findReviewAction(
            TransactionVO transaction,
            boolean reviewSupported,
            LocalDateTime reviewDeadline) {
        if (!reviewSupported
                || !"PAYMENT".equals(transaction.getTransactionType())
                || !"PAID".equals(transaction.getStatus())) {
            return TransactionReviewAction.NONE;
        }

        boolean deadlinePassed = LocalDateTime.now().isAfter(reviewDeadline);
        if (transaction.getReviewId() == null) {
            return deadlinePassed
                    ? TransactionReviewAction.NONE
                    : TransactionReviewAction.WRITE;
        }
        if (!"ACTIVE".equals(transaction.getReviewStatus())) {
            return TransactionReviewAction.NONE;
        }
        return deadlinePassed
                ? TransactionReviewAction.DELETE
                : TransactionReviewAction.EDIT;
    }

    @Override
    public ReceiptResponse getReceipt(Long userId, Long transactionId) {
        TransactionVO transaction = transactionMapper.findTransactionForReceipt(transactionId, userId);
        if (transaction == null) {
            throw new BusinessException(TransactionErrorCode.TRANSACTION_NOT_FOUND);
        }
        if (!"PAYMENT".equals(transaction.getTransactionType())) {
            throw new BusinessException(TransactionErrorCode.TRANSACTION_RECEIPT_NOT_AVAILABLE);
        }

        return ReceiptResponse.from(transaction);
    }

    @Override
    @Transactional
    public CancelResponse cancelTransaction(Long userId, Long transactionId) {

        TransactionVO transaction = transactionMapper.findTransactionForCancel(transactionId, userId);
        if (transaction == null) {
            throw new BusinessException(TransactionErrorCode.TRANSACTION_NOT_FOUND);
        }
        if ("CANCELED".equals(transaction.getStatus())) {
            throw new BusinessException(TransactionErrorCode.TRANSACTION_ALREADY_CANCELED);
        }
        if (!"PAYMENT".equals(transaction.getTransactionType())) {
            throw new BusinessException(TransactionErrorCode.TRANSACTION_CANCEL_NOT_ALLOWED);
        }

        BigDecimal refundedAmount = BigDecimal.ZERO;
        String refundedTo = null;

        boolean isWallet = "WALLET".equals(transaction.getPaymentSourceType());
        boolean isDebitCard = "CARD".equals(transaction.getPaymentSourceType())
                && "DEBIT".equals(transaction.getCardClassification());

        if (isWallet || isDebitCard) {
            walletMapper.increaseBalance(userId, transaction.getAmount());
            refundedAmount = transaction.getAmount();
            refundedTo = "WALLET";
        }

        transactionMapper.cancelTransaction(transactionId, userId);

        TransactionVO cancelled = transactionMapper.findTransactionForCancel(transactionId, userId);

        return CancelResponse.of(cancelled, refundedAmount, refundedTo);
    }
}
