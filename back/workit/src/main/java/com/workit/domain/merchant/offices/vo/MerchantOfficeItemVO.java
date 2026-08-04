package com.workit.domain.merchant.offices.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class MerchantOfficeItemVO {

    private Long merchantId;
    private String name;
    private String address;
    private String thumbnailUrl;
    private Long price;
    private BigDecimal rating;
    private Integer reviewCount;
    private String noiseLevel;
    private Boolean bookmarked;
}
