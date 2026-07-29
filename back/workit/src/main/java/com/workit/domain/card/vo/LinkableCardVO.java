package com.workit.domain.card.vo;

import lombok.Data;

@Data
public class LinkableCardVO {

    private Long id;
    private Long userId;
    private String cardCompanyCode;
    private String cardNumber;
    private String cardName;
    private String cardClassification;
    private String cardType;
    private Boolean isLinked;
}