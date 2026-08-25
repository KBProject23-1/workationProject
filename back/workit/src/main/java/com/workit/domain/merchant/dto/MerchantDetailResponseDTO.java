package com.workit.domain.merchant.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MerchantDetailResponseDTO {

    private final Long merchantId;
    private final String name;
    private final String address;
    private final String phoneNumber;
    private final String description;
    private final LocalTime checkInTime;
    private final LocalTime checkOutTime;
    private final BigDecimal rating;
    private final Integer reviewCount;
    private final String thumbnailUrl;
    private final Boolean bookmarked;
    private final List<MerchantItemResponseDTO> products;

    public static MerchantDetailResponseDTO of(
            Long merchantId,
            String name,
            String address,
            String phoneNumber,
            String description,
            LocalTime checkInTime,
            LocalTime checkOutTime,
            BigDecimal rating,
            Integer reviewCount,
            String thumbnailUrl,
            Boolean bookmarked,
            List<MerchantItemResponseDTO> products
    ) {
        return MerchantDetailResponseDTO.builder()
                .merchantId(merchantId)
                .name(name)
                .address(address)
                .phoneNumber(phoneNumber)
                .description(description)
                .checkInTime(checkInTime)
                .checkOutTime(checkOutTime)
                .rating(rating)
                .reviewCount(reviewCount)
                .thumbnailUrl(thumbnailUrl)
                .bookmarked(bookmarked)
                .products(products)
                .build();
    }
}
