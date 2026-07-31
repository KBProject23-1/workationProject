package com.workit.domain.transaction.service;

import com.workit.domain.account.mapper.AccountMapper;
import com.workit.domain.account.vo.BankAccountVO;
import com.workit.domain.card.mapper.CardMapper;
import com.workit.domain.card.vo.CardVO;
import com.workit.domain.transaction.dto.request.PaymentRequest;
import com.workit.domain.transaction.dto.response.*;
import com.workit.domain.transaction.exception.TransactionErrorCode;
import com.workit.domain.transaction.mapper.TransactionMapper;
import com.workit.domain.transaction.util.TransactionNumberGenerator;
import com.workit.domain.transaction.vo.TransactionVO;
import com.workit.domain.wallet.mapper.WalletMapper;
import com.workit.domain.wallet.vo.WalletVO;
import com.workit.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.workit.global.constant.PaymentPolicy.MIN_CHARGE_AMOUNT;
import static com.workit.global.constant.PaymentPolicy.MAX_TRANSACTION_AMOUNT;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionMapper transactionMapper;
    private final WalletMapper walletMapper;
    private final AccountMapper accountMapper;
    private final CardMapper cardMapper;

    @Override
    public List<TransactionListItemResponse> getTransactions(Long userId, String startDate, String endDate,
                                                             String paymentSourceType, String transactionType, Long cardId) {
        List<TransactionVO> transactions = transactionMapper.findTransactions(
                userId, startDate, endDate, paymentSourceType, transactionType, cardId
        );
        return transactions.stream()
                .map(TransactionListItemResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    public TransactionDetailResponse getTransactionDetail(Long userId, Long transactionId) {
        TransactionVO transaction = transactionMapper.findTransactionDetailById(transactionId, userId);
        if (transaction == null) {
            throw new BusinessException(TransactionErrorCode.TRANSACTION_NOT_FOUND);
        }
        return TransactionDetailResponse.from(transaction);
    }

    @Override
    public ReceiptResponse getReceipt(Long userId, Long transactionId) {
        TransactionVO transaction = transactionMapper.findTransactionForReceipt(transactionId, userId);
        if (transaction == null) {
            throw new BusinessException(TransactionErrorCode.TRANSACTION_NOT_FOUND);
        }
        if (!"PAYMENT".equals(transaction.getTransactionType())) {
            throw new BusinessException(TransactionErrorCode.RECEIPT_NOT_AVAILABLE);
        }

        return ReceiptResponse.from(transaction);
    }

    @Override
    @Transactional
    public PaymentResponse pay(Long userId, PaymentRequest request) {

        validatePaymentRequest(request);
        validatePin(userId, request.getPinNumber());

        if ("WALLET".equals(request.getPaymentSourceType())) {
            return payWithWallet(userId, request);
        } else {
            return payWithCard(userId, request);
        }
    }

    private PaymentResponse payWithWallet(Long userId, PaymentRequest request) {
        BigDecimal amount = request.getAmount();

        WalletVO wallet = walletMapper.findByUserId(userId);
        if (wallet == null) {
            throw new BusinessException(TransactionErrorCode.WALLET_NOT_FOUND);
        }

        BigDecimal shortage = amount.subtract(wallet.getBalance());
        boolean isAutoCharged = shortage.compareTo(BigDecimal.ZERO) > 0;
        BigDecimal autoChargedAmount = null;

        if (isAutoCharged) {
            // 부족분이 최소 충전금액(1만원)보다 적으면 1만원으로 채움
            BigDecimal actualChargeAmount = shortage.compareTo(MIN_CHARGE_AMOUNT) < 0
                    ? MIN_CHARGE_AMOUNT
                    : shortage;

            BankAccountVO primaryAccount = accountMapper.findPrimaryAccount(userId);
            if (primaryAccount == null) {
                throw new BusinessException(TransactionErrorCode.PRIMARY_ACCOUNT_NOT_FOUND_FOR_AUTO_CHARGE);
            }

            int accountUpdatedRows = accountMapper.decreaseBalance(primaryAccount.getId(), actualChargeAmount);
            if (accountUpdatedRows == 0) {
                throw new BusinessException(TransactionErrorCode.INSUFFICIENT_ACCOUNT_BALANCE);
            }

            walletMapper.increaseBalance(userId, actualChargeAmount);
            autoChargedAmount = actualChargeAmount;

            TransactionVO depositTx = TransactionVO.forAutoCharge(userId, wallet, primaryAccount, actualChargeAmount);
            transactionMapper.insertTransaction(depositTx);
        }

        int walletUpdatedRows = walletMapper.decreaseBalance(userId, amount);
        if (walletUpdatedRows == 0) {
            throw new BusinessException(TransactionErrorCode.INSUFFICIENT_WALLET_BALANCE);
        }

        WalletVO updatedWallet = walletMapper.findByUserId(userId);

        TransactionVO paymentTx = TransactionVO.forWalletPayment(userId, wallet, request);
        transactionMapper.insertTransaction(paymentTx);

        return PaymentResponse.ofWallet(paymentTx, updatedWallet.getBalance(), isAutoCharged, autoChargedAmount);
    }

    private PaymentResponse payWithCard(Long userId, PaymentRequest request) {
        if (request.getCardId() == null) {
            throw new BusinessException(TransactionErrorCode.CARD_ID_REQUIRED);
        }

        CardVO card = cardMapper.findCardById(request.getCardId(), userId);
        if (card == null) {
            throw new BusinessException(TransactionErrorCode.CARD_NOT_FOUND);
        }

        boolean isBusinessExpense = "WORK".equals(card.getCardType());
        String approvalNumber = TransactionNumberGenerator.generateApprovalNumber();

        TransactionVO paymentTx = TransactionVO.forCardPayment(userId, card, request, isBusinessExpense, approvalNumber);
        transactionMapper.insertTransaction(paymentTx);

        return PaymentResponse.ofCard(paymentTx);
    }

    private void validatePaymentRequest(PaymentRequest request) {
        if (request.getMerchantName() == null || request.getMerchantName().trim().isEmpty()) {
            throw new BusinessException(TransactionErrorCode.MERCHANT_NAME_REQUIRED);
        }
        if (request.getAmount() == null) {
            throw new BusinessException(TransactionErrorCode.AMOUNT_REQUIRED);
        }
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(TransactionErrorCode.INVALID_AMOUNT);
        }
        if (request.getAmount().compareTo(MAX_TRANSACTION_AMOUNT) > 0) {
            throw new BusinessException(TransactionErrorCode.MAX_AMOUNT_EXCEEDED);
        }
        if (request.getPaymentSourceType() == null) {
            throw new BusinessException(TransactionErrorCode.PAYMENT_SOURCE_TYPE_REQUIRED);
        }
        if (request.getPinNumber() == null) {
            throw new BusinessException(TransactionErrorCode.PIN_REQUIRED);
        }
    }

    private void validatePin(Long userId, String pinNumber) {
        // TODO: user_device.pin_number 검증 로직 — 담당자 확인 후 구현
        if (pinNumber == null || pinNumber.length() != 6) {
            throw new BusinessException(TransactionErrorCode.PIN_INVALID);
        }
    }

    @Override
    @Transactional
    public CancelResponse cancelTransaction(Long userId, Long transactionId) {

        TransactionVO transaction = transactionMapper.findTransactionForCancel(transactionId, userId);
        if (transaction == null) {
            throw new BusinessException(TransactionErrorCode.TRANSACTION_NOT_FOUND);
        }
        if ("CANCELED".equals(transaction.getStatus())) {
            throw new BusinessException(TransactionErrorCode.ALREADY_CANCELED);
        }
        if (!"PAYMENT".equals(transaction.getTransactionType())) {
            throw new BusinessException(TransactionErrorCode.CANCEL_NOT_ALLOWED);
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

        transactionMapper.cancelTransaction(transactionId);

        TransactionVO cancelled = transactionMapper.findTransactionForCancel(transactionId, userId);

        return CancelResponse.of(cancelled, refundedAmount, refundedTo);
    }
}