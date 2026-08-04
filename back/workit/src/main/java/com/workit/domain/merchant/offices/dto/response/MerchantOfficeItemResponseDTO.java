package com.workit.domain.merchant.offices.dto.response;

import com.workit.domain.merchant.offices.vo.MerchantOfficeItemVO;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class MerchantOfficeItemResponseDTO {

    private final Long merchantId;
    private final String name;
    private final String address;
    private final String thumbnailUrl;
    private final Long price;
    private final BigDecimal rating;
    private final Integer reviewCount;
    private final String noiseLevel;
    private final Boolean bookmarked;

    public static MerchantOfficeItemResponseDTO from(MerchantOfficeItemVO vo) {
        return MerchantOfficeItemResponseDTO.builder()
                .merchantId(vo.getMerchantId())
                .name(vo.getName())
                .address(vo.getAddress())
                .thumbnailUrl(vo.getThumbnailUrl())
                .price(vo.getPrice())
                .rating(vo.getRating())
                .reviewCount(vo.getReviewCount())
                .noiseLevel(vo.getNoiseLevel())
                .bookmarked(vo.getBookmarked())
                .build();
    }
}
