package com.workit.domain.merchant.dto.response;

import com.workit.domain.merchant.vo.AccommodationDetailVO;
import com.workit.domain.merchant.vo.AccommodationType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

@Getter
@Builder
public class AccommodationDetailResponseDTO {

    private final Long merchantId;
    private final Long regionId;
    private final String name;
    private final String address;
    private final Double latitude;
    private final Double longitude;
    private final String phoneNumber;
    private final AccommodationType accommodationType;
    private final String description;
    private final LocalTime checkInTime;
    private final LocalTime checkOutTime;
    private final BigDecimal rating;
    private final String thumbnailUrl;
    private final Long price;
    private final List<String> tags;

    public static AccommodationDetailResponseDTO from(
            AccommodationDetailVO accommodation,
            List<String> tags) {

        return AccommodationDetailResponseDTO.builder()
                .merchantId(accommodation.getMerchantId())
                .regionId(accommodation.getRegionId())
                .name(accommodation.getName())
                .address(accommodation.getAddress())
                .latitude(accommodation.getLatitude())
                .longitude(accommodation.getLongitude())
                .phoneNumber(accommodation.getPhoneNumber())
                .accommodationType(accommodation.getAccommodationType())
                .description(accommodation.getDescription())
                .checkInTime(accommodation.getCheckInTime())
                .checkOutTime(accommodation.getCheckOutTime())
                .rating(accommodation.getRating())
                .thumbnailUrl(accommodation.getThumbnailUrl())
                .price(accommodation.getPrice())
                .tags(tags == null ? Collections.emptyList() : tags)
                .build();
    }
}
