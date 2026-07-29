package com.workit.domain.card.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CardVO {

    private Long id;
    private Long userId;
    private String cardCompanyCode;
    private String cardCompanyName;
    private String cardName;
    private String cardNumber;
    private String cardClassification;  // CREDIT, DEBIT
    private String cardType;            // WORK, PERSONAL
    private Boolean isPrimary;
    private Boolean isAgreed;
    private LocalDateTime createdAt;
    private Boolean isDeleted;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}