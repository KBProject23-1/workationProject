package com.workit.domain.transaction.controller;

import com.workit.domain.transaction.dto.request.PaymentRequest;
import com.workit.domain.transaction.dto.response.*;
import com.workit.domain.transaction.service.TransactionService;
import com.workit.global.dto.CommonResponse;
import com.workit.global.dto.PageResponseDTO;
import com.workit.global.response.GlobalResponseFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequiredArgsConstructor
@Slf4j
public class TransactionController {

    private final TransactionService transactionService;

    /** 거래 내역 전체 목록 조회 (필터링, 페이징) */
    @GetMapping("/api/v1/transactions")
    public ResponseEntity<CommonResponse<PageResponseDTO<TransactionListItemResponse>>> getTransactions(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String paymentSourceType,
            @RequestParam(required = false) String transactionType,
            @RequestParam(required = false) Long cardId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request
    ) {
        // JWT 토큰에서 userId 추출로 교체 필요(추후 삭제)
        Long userId = 1L;
        return GlobalResponseFactory.success(transactionService.getTransactions(
                userId, startDate, endDate, paymentSourceType, transactionType, cardId, page, size
        ));
    }

    /** 거래 내역 조회 기간 전체 집계 (결제 합계 / 충전 합계, 페이징과 무관) */
    @GetMapping("/api/v1/transactions/summary")
    public ResponseEntity<CommonResponse<TransactionSummaryResponse>> getTransactionSummary(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String paymentSourceType,
            @RequestParam(required = false) Long cardId,
            HttpServletRequest request
    ) {
        // JWT 토큰에서 userId 추출로 교체 필요(추후 삭제)
        Long userId = 1L;
        return GlobalResponseFactory.success(transactionService.getTransactionSummary(
                userId, startDate, endDate, paymentSourceType, cardId
        ));
    }

    /** 거래 내역 단건 상세 조회 */
    @GetMapping("/api/v1/transactions/{transactionId}")
    public ResponseEntity<CommonResponse<TransactionDetailResponse>> getTransactionDetail(
            @PathVariable Long transactionId,
            HttpServletRequest request
    ) {
        // JWT 토큰에서 userId 추출로 교체 필요(추후 삭제)
        Long userId = 1L;
        return GlobalResponseFactory.success(transactionService.getTransactionDetail(userId, transactionId));
    }

    /** 매출전표 조회 */
    @GetMapping("/api/v1/transactions/{transactionId}/receipts")
    public ResponseEntity<CommonResponse<ReceiptResponse>> getReceipt(
            @PathVariable Long transactionId,
            HttpServletRequest request
    ) {
        // JWT 토큰에서 userId 추출로 교체 필요(추후 삭제)
        Long userId = 1L;
        return GlobalResponseFactory.success(transactionService.getReceipt(userId, transactionId));
    }

    /** 통합 결제 (지갑/카드) */
    @PostMapping("/api/v1/payments")
    public ResponseEntity<CommonResponse<PaymentResponse>> pay(
            @RequestBody PaymentRequest requestBody,
            HttpServletRequest request
    ) {
        // JWT 토큰에서 userId 추출로 교체 필요(추후 삭제)
        Long userId = 1L;
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponse.success(transactionService.pay(userId, requestBody)));
    }

    /** 거래 내역 취소(환불) */
    @PatchMapping("/api/v1/transactions/{transactionId}/cancel")
    public ResponseEntity<CommonResponse<CancelResponse>> cancelTransaction(
            @PathVariable Long transactionId,
            HttpServletRequest request
    ) {
        // JWT 토큰에서 userId 추출로 교체 필요(추후 삭제)
        Long userId = 1L;
        return GlobalResponseFactory.success(transactionService.cancelTransaction(userId, transactionId));
    }
}