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
    private String idempotencyKey;
    // 결제 화면에서 고른 업무/개인 구분. 1 업무 / 0 개인 / null 미선택
    private Boolean isBusinessExpense;
}