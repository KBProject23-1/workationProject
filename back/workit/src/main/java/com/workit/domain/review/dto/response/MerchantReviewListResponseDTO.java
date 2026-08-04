package com.workit.domain.review.dto.response;

import com.workit.domain.review.vo.MerchantReviewStatisticsVO;
import com.workit.global.dto.PageResponseDTO;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Map;

// 가맹점의 활성 리뷰 목록과 평점 통계 응답 DTO
@Getter
@Builder
public class MerchantReviewListResponseDTO {

    private PageResponseDTO<MerchantReviewItemResponseDTO> reviews;
    private BigDecimal averageRating;
    private Long reviewCount;
    private Map<Integer, Long> ratingDistribution;

    public static MerchantReviewListResponseDTO of(
            PageResponseDTO<MerchantReviewItemResponseDTO> reviews,
            MerchantReviewStatisticsVO statistics,
            Map<Integer, Long> ratingDistribution) {
        return MerchantReviewListResponseDTO.builder()
                .reviews(reviews)
                .averageRating(statistics.getAverageRating())
                .reviewCount(statistics.getReviewCount())
                .ratingDistribution(ratingDistribution)
                .build();
    }
}
