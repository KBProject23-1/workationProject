package com.workit.domain.merchant.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class AccommodationListItemVO {

    private Long merchantId;
    private String name;
    private String address;
    private Double latitude;
    private Double longitude;
    private AccommodationType accommodationType;
    private BigDecimal rating;
    private String thumbnailUrl;
    private Long price;
}
