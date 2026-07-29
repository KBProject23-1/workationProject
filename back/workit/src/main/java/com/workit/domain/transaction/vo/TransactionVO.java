package com.workit.domain.transaction.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TransactionVO {

    private Long id;
    private Long userId;
    private Long walletId;
    private Long bankAccountId;
    private Long cardId;
    private Long workationId;
    private Long reservationId;
    private Long merchantId;
    private String paymentSourceType;   // CARD, WALLET
    private String merchantName;
    private BigDecimal amount;
    private String transactionType;     // DEPOSIT, WITHDRAWAL, PAYMENT
    private String categoryAssigned;
    private Boolean isBusinessExpense;
    private String approvedNumber;
    private String transactionNumber;
    private String status;              // PAID, FAILED, CANCELED
    private LocalDateTime approvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime cancelledAt;
}