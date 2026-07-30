package com.workit.domain.wallet.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ChargeResponse {
    private Long transactionId;
    private BigDecimal chargedAmount;
    private BigDecimal currentBalance;
    private String paymentSourceType;
    private String transactionType;
    private LocalDateTime approvedAt;
}