package com.workit.domain.merchant.offices.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class MerchantOfficeDetailVO {

    private Long merchantId;
    private String name;
    private String address;
    private Double latitude;
    private Double longitude;
    private String phoneNumber;
    private String thumbnailUrl;
    private Long price;
    private BigDecimal rating;
    private Integer reviewCount;
    private String description;
    private Boolean bookmarked;
}
