package com.workit.domain.merchant.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.workit.domain.review.vo.MerchantReviewVO;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MerchantDetailReviewResponseDTO {

    private final String nickname;

    @JsonProperty("created_at")
    private final LocalDateTime createdAt;

    private final Integer rating;
    private final String content;

    @JsonProperty("is_mine")
    private final Boolean isMine;

    public static MerchantDetailReviewResponseDTO from(MerchantReviewVO vo) {
        if (vo == null) {
            return null;
        }

        return MerchantDetailReviewResponseDTO.builder()
                .nickname(vo.getNickname())
                .createdAt(vo.getCreatedAt())
                .rating(vo.getRating())
                .content(vo.getContent())
                .isMine(Boolean.TRUE.equals(vo.getIsMine()))
                .build();
    }
}

