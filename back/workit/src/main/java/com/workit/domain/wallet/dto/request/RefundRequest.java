package com.workit.domain.wallet.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RefundRequest {
    private BigDecimal amount;
    private String pinNumber;
    private String deviceId;
}