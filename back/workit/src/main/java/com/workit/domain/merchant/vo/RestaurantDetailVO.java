package com.workit.domain.merchant.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class RestaurantDetailVO {

    private Long merchantId;
    private Long regionId;
    private String name;
    private String address;
    private Double latitude;
    private Double longitude;
    private String phoneNumber;
    private FoodType foodType;
    private Integer priceLevel;
    private BigDecimal rating;
    private String thumbnailUrl;
    private Long price;
}
