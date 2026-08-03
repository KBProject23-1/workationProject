package com.workit.domain.merchant.dto.request;

import com.workit.domain.merchant.vo.FoodType;
import com.workit.domain.merchant.vo.RestaurantSort;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class RestaurantListRequestDTO {

    private Long regionId;
    private FoodType foodType;
    private Long minPrice;
    private Long maxPrice;
    private BigDecimal minRating;
    private RestaurantSort sort = RestaurantSort.RATING_DESC;
    private int page = 0;
    private int size = 10;
}
