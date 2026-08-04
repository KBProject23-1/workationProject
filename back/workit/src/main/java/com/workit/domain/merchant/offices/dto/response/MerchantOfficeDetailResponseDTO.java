package com.workit.domain.merchant.offices.dto.response;

import com.workit.domain.merchant.offices.vo.MerchantOfficeDetailTagVO;
import com.workit.domain.merchant.offices.vo.MerchantOfficeDetailVO;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class MerchantOfficeDetailResponseDTO {

    private final Long merchantId;
    private final String name;
    private final String address;
    private final Double latitude;
    private final Double longitude;
    private final String phoneNumber;
    private final String thumbnailUrl;
    private final Long price;
    private final BigDecimal rating;
    private final Integer reviewCount;
    private final String description;
    private final List<TagInfo> tags;
    private final Boolean bookmarked;

    public static MerchantOfficeDetailResponseDTO of(
            MerchantOfficeDetailVO vo,
            List<MerchantOfficeDetailTagVO> tags
    ) {
        return MerchantOfficeDetailResponseDTO.builder()
                .merchantId(vo.getMerchantId())
                .name(vo.getName())
                .address(vo.getAddress())
                .latitude(vo.getLatitude())
                .longitude(vo.getLongitude())
                .phoneNumber(vo.getPhoneNumber())
                .thumbnailUrl(vo.getThumbnailUrl())
                .price(vo.getPrice())
                .rating(vo.getRating())
                .reviewCount(vo.getReviewCount())
                .description(vo.getDescription())
                .tags(tags == null
                        ? java.util.Collections.emptyList()
                        : tags.stream().map(TagInfo::from).collect(Collectors.toList()))
                .bookmarked(vo.getBookmarked())
                .build();
    }

    @Getter
    @Builder
    public static class TagInfo {

        private final Long tagId;
        private final String name;

        public static TagInfo from(MerchantOfficeDetailTagVO vo) {
            return TagInfo.builder()
                    .tagId(vo.getTagId())
                    .name(vo.getName())
                    .build();
        }
    }
}
