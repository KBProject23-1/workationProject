package com.workit.domain.merchant.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class RestaurantListItemVO {

    private Long merchantId;
    private String name;
    private String address;
    private Double latitude;
    private Double longitude;
    private FoodType foodType;
    private Integer priceLevel;
    private BigDecimal rating;
    private String thumbnailUrl;
    private Long price;
}
