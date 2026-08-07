package com.workit.domain.transaction.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentRequest {
    private Long reservationId;
    private Long merchantId;
    private String merchantName;
    private BigDecimal amount;
    private String paymentSourceType;  // WALLET, CARD
    private Long cardId;
    private String pinNumber;
    private String deviceId;
}