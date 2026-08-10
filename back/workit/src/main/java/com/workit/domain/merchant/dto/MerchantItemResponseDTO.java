package com.workit.domain.merchant.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.workit.domain.merchant.vo.MerchantProductVO;
import com.workit.domain.merchant.vo.MerchantVO;
import com.workit.domain.reservation.vo.ReservationProductDetailType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MerchantItemResponseDTO {

    private final String category;
    private final Long merchantId;
    private final String name;
    private final String address;
    private final Integer reviewCount;
    private final BigDecimal rating;
    private final Boolean bookmarked;

    private final Long productId;
    private final String productName;
    private final String description;
    private final ReservationProductDetailType productDetailType;
    private final Integer maxHeadcount;
    private final String thumbnailUrl;
    private final BigDecimal price;

    public static MerchantItemResponseDTO from(MerchantProductVO vo) {
        if (vo == null) {
            return null;
        }

        return MerchantItemResponseDTO.builder()
                .productId(vo.getProductId())
                .productName(vo.getProductName())
                .description(vo.getDescription())
                .productDetailType(vo.getProductDetailType())
                .maxHeadcount(vo.getMaxHeadcount())
                .thumbnailUrl(vo.getThumbnailUrl())
                .price(vo.getUnitPrice())
                .build();
    }

    public static MerchantItemResponseDTO from(MerchantVO vo) {
        if (vo == null) {
            return null;
        }

        return MerchantItemResponseDTO.builder()
                .category(vo.getCategory())
                .merchantId(vo.getMerchantId())
                .name(vo.getName())
                .address(vo.getAddress())
                .price(vo.getPrice() == null ? null : BigDecimal.valueOf(vo.getPrice()))
                .rating(vo.getRating())
                .reviewCount(vo.getReviewCount())
                .thumbnailUrl(vo.getThumbnailUrl())
                .bookmarked(vo.getBookmarked())
                .build();
    }
}
