package com.workit.domain.transaction.service;

import com.workit.domain.transaction.dto.request.PaymentRequest;
import com.workit.domain.transaction.dto.response.*;
import com.workit.global.dto.PageResponseDTO;

public interface TransactionService {

    PageResponseDTO<TransactionListItemResponse> getTransactions(Long userId, String startDate, String endDate,
                                                      String paymentSourceType, String transactionType, Long cardId,
                                                      int page, int size);

    TransactionSummaryResponse getTransactionSummary(Long userId, String startDate, String endDate,
                                                      String paymentSourceType, Long cardId);

    TransactionDetailResponse getTransactionDetail(Long userId, Long transactionId);

    ReceiptResponse getReceipt(Long userId, Long transactionId);

    PaymentResponse pay(Long userId, PaymentRequest request);

    CancelResponse cancelTransaction(Long userId, Long transactionId);

}