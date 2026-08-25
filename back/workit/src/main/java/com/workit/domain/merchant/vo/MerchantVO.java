package com.workit.domain.merchant.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class MerchantVO {

    private String category;
    private Long merchantId;
    private String name;
    private String address;
    private Long price;
    private BigDecimal rating;
    private Integer reviewCount;
    private String thumbnailUrl;
    private Boolean bookmarked;
}
