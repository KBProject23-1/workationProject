package com.workit.domain.wallet.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RefundResponse {
    private Long transactionId;
    private BigDecimal refundedAmount;
    private BigDecimal remainingBalance;
    private TargetAccountInfo targetAccount;
    private String paymentSourceType;
    private String transactionType;
    private LocalDateTime approvedAt;

    @lombok.Data
    public static class TargetAccountInfo {
        private String bankCode;
        private String maskedAccountNumber;
    }
}