package com.workit.domain.merchant.dto.response;

import com.workit.domain.merchant.vo.FoodType;
import com.workit.domain.merchant.vo.RestaurantListItemVO;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class RestaurantListResponseDTO {

    private final Long merchantId;
    private final String name;
    private final String address;
    private final Double latitude;
    private final Double longitude;
    private final FoodType foodType;
    private final Integer priceLevel;
    private final BigDecimal rating;
    private final String thumbnailUrl;
    private final Long price;

    public static RestaurantListResponseDTO from(RestaurantListItemVO restaurant) {
        return RestaurantListResponseDTO.builder()
                .merchantId(restaurant.getMerchantId())
                .name(restaurant.getName())
                .address(restaurant.getAddress())
                .latitude(restaurant.getLatitude())
                .longitude(restaurant.getLongitude())
                .foodType(restaurant.getFoodType())
                .priceLevel(restaurant.getPriceLevel())
                .rating(restaurant.getRating())
                .thumbnailUrl(restaurant.getThumbnailUrl())
                .price(restaurant.getPrice())
                .build();
    }
}
