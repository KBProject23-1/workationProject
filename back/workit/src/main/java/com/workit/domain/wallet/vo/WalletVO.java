package com.workit.domain.wallet.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class WalletVO {

    private Long id;
    private Long userId;
    private BigDecimal balance;
    private LocalDateTime updatedAt;
}