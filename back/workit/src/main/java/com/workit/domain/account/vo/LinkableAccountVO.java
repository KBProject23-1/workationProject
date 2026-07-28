package com.workit.domain.account.vo;

import lombok.Data;

@Data
public class LinkableAccountVO {

    private Long id;
    private Long userId;
    private String bankCode;
    private String accountNumber;
    private String productName;
    private Boolean isLinked;
}