package com.workit.domain.merchant.dto.response;

import com.workit.domain.merchant.vo.FoodType;
import com.workit.domain.merchant.vo.RestaurantDetailVO;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Getter
@Builder
public class RestaurantDetailResponseDTO {

    private final Long merchantId;
    private final Long regionId;
    private final String name;
    private final String address;
    private final Double latitude;
    private final Double longitude;
    private final String phoneNumber;
    private final FoodType foodType;
    private final Integer priceLevel;
    private final BigDecimal rating;
    private final String thumbnailUrl;
    private final Long price;
    private final List<String> tags;

    public static RestaurantDetailResponseDTO from(
            RestaurantDetailVO restaurant,
            List<String> tags) {

        return RestaurantDetailResponseDTO.builder()
                .merchantId(restaurant.getMerchantId())
                .regionId(restaurant.getRegionId())
                .name(restaurant.getName())
                .address(restaurant.getAddress())
                .latitude(restaurant.getLatitude())
                .longitude(restaurant.getLongitude())
                .phoneNumber(restaurant.getPhoneNumber())
                .foodType(restaurant.getFoodType())
                .priceLevel(restaurant.getPriceLevel())
                .rating(restaurant.getRating())
                .thumbnailUrl(restaurant.getThumbnailUrl())
                .price(restaurant.getPrice())
                .tags(tags == null ? Collections.emptyList() : tags)
                .build();
    }
}
