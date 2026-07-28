package com.workit.domain.account.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BankAccountVO {
    private Long id;
    private Long userId;
    private String bankCode;
    private String accountNumber;
    private String productName;
    private BigDecimal balance;
    private Boolean isPrimary;
    private Boolean isWithdrawlAgreed;
    private LocalDateTime withdrawlAgreedAt;
    private LocalDateTime balanceUpdatedAt;
    private Boolean isDeleted;
}
