package com.workit.domain.merchant.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MerchantDetailCommonResponseDTO {

    private final Long merchantId;
    private final String merchantName;
    private final String description;
    private final String thumbnailUrl;
    private final String address;
    private final BigDecimal price;
    private final BigDecimal rating;
    private final Integer reviewCount;
    private final Boolean bookmarked;
    private final List<MerchantDetailReviewResponseDTO> reviews;

    public static MerchantDetailCommonResponseDTO of(
            Long merchantId,
            String merchantName,
            String description,
            String thumbnailUrl,
            String address,
            BigDecimal price,
            BigDecimal rating,
            Integer reviewCount,
            Boolean bookmarked,
            List<MerchantDetailReviewResponseDTO> reviews
    ) {
        List<MerchantDetailReviewResponseDTO> safeReviews = reviews == null
                ? Collections.emptyList()
                : reviews;

        return MerchantDetailCommonResponseDTO.builder()
                .merchantId(merchantId)
                .merchantName(merchantName)
                .description(description)
                .thumbnailUrl(thumbnailUrl)
                .address(address)
                .price(price)
                .rating(rating)
                .reviewCount(reviewCount)
                .bookmarked(bookmarked)
                .reviews(safeReviews)
                .build();
    }
}
