package com.workit.domain.wallet.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RefundRequest {
    private Long accountId;
    private BigDecimal amount;
    private String pinNumber;
    private String deviceId;
    private String idempotencyKey;
}