package com.workit.domain.tourism.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class TourismMerchantVO {
    private Long merchantId;
    private Long regionId;
    private String name;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String phoneNumber;
    private String thumbnailUrl;
    private TourismPlaceType category;
}
