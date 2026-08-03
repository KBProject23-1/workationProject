package com.workit.domain.merchant.dto.response;

import com.workit.domain.merchant.vo.AccommodationListItemVO;
import com.workit.domain.merchant.vo.AccommodationType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class AccommodationListResponseDTO {

    private final Long merchantId;
    private final String name;
    private final String address;
    private final Double latitude;
    private final Double longitude;
    private final AccommodationType accommodationType;
    private final BigDecimal rating;
    private final String thumbnailUrl;
    private final Long price;

    public static AccommodationListResponseDTO from(AccommodationListItemVO accommodation) {
        return AccommodationListResponseDTO.builder()
                .merchantId(accommodation.getMerchantId())
                .name(accommodation.getName())
                .address(accommodation.getAddress())
                .latitude(accommodation.getLatitude())
                .longitude(accommodation.getLongitude())
                .accommodationType(accommodation.getAccommodationType())
                .rating(accommodation.getRating())
                .thumbnailUrl(accommodation.getThumbnailUrl())
                .price(accommodation.getPrice())
                .build();
    }
}
