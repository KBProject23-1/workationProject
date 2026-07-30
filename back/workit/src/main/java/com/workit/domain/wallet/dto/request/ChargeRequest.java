package com.workit.domain.wallet.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ChargeRequest {
    private Long accountId;
    private BigDecimal amount;
    private String pinNumber;
}