package com.workit.domain.review.dto.response;

import com.workit.domain.review.vo.MerchantReviewVO;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// 가맹점 리뷰 목록의 개별 리뷰 응답 DTO
@Getter
@Builder
public class MerchantReviewItemResponseDTO {

    private Long reviewId;
    private String nickname;
    private Integer rating;
    private String content;
    private String atmosphere;
    private LocalDateTime createdAt;
    private String imageUrl;

    public static MerchantReviewItemResponseDTO from(MerchantReviewVO review) {
        return MerchantReviewItemResponseDTO.builder()
                .reviewId(review.getReviewId())
                .nickname(review.getNickname())
                .rating(review.getRating())
                .content(review.getContent())
                .atmosphere(review.getAtmosphere())
                .createdAt(review.getCreatedAt())
                .imageUrl(review.getImageUrl())
                .build();
    }
}
