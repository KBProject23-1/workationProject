package com.workit.domain.recommendation.accommodation.dto.response;

import com.workit.domain.recommendation.accommodation.vo.RecommendationRequestVO;
import com.workit.domain.recommendation.accommodation.vo.RecommendationResultVO;
import com.workit.domain.recommendation.accommodation.vo.RecommendationType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public class AccommodationRecommendationResponseDTO {
    private Long recommendationRequestId;
    private RecommendationType recommendationType;
    private Object mealType;
    private RecommendationReferenceResponseDTO reference;
    private List<Item> content;
    private PageInfo pageInfo;
    private LocalDateTime createdAt;

    public static AccommodationRecommendationResponseDTO of(RecommendationRequestVO request,
                                                             List<RecommendationResultVO> results,
                                                             int size,
                                                             boolean hasNext,
                                                             String nextCursor) {
        List<Item> content = results.stream().map(Item::from).collect(Collectors.toList());
        return new AccommodationRecommendationResponseDTO(request.getId(), request.getRecommendationType(), null,
                RecommendationReferenceResponseDTO.from(request), content,
                new PageInfo(size, content.size(), hasNext, nextCursor), request.getCreatedAt());
    }

    @Getter
    @AllArgsConstructor
    public static class Item {
        private Long recommendationResultId;
        private Integer ranking;
        private Long merchantId;
        private String name;
        private String thumbnailUrl;
        private String address;
        private BigDecimal price;
        private BigDecimal rating;
        private Long reviewCount;
        private Long distanceMeters;
        private Score score;
        private String recommendationReason;
        private boolean bookmarked;

        static Item from(RecommendationResultVO result) {
            Long distanceMeters = result.getDistance() == null ? null
                    : result.getDistance().multiply(BigDecimal.valueOf(1000))
                    .setScale(0, RoundingMode.HALF_UP).longValue();
            String reason = distanceMeters == null
                    ? "1박 예산과 평점을 반영한 숙소예요."
                    : "1박 예산을 반영했으며 기준 공유오피스에서 " + distanceMeters + "m 떨어진 숙소예요.";
            return new Item(result.getRecommendationResultId(), result.getRanking(), result.getMerchantId(),
                    result.getMerchantName(), result.getThumbnailUrl(), result.getAddress(), result.getPrice(),
                    result.getRating(), result.getReviewCount(), distanceMeters, Score.from(result), reason,
                    result.isBookmarked());
        }
    }

    @Getter
    @AllArgsConstructor
    public static class Score {
        private BigDecimal priceScore;
        private Object preferenceScore;
        private BigDecimal accessibilityScore;
        private BigDecimal ratingScore;
        private BigDecimal totalScore;

        static Score from(RecommendationResultVO result) {
            return new Score(result.getPriceScore(), null, result.getAccessibilityScore(),
                    result.getRatingScore(), result.getTotalScore());
        }
    }

    @Getter
    @AllArgsConstructor
    public static class PageInfo {
        private int size;
        private int numberOfElements;
        private boolean hasNext;
        private String nextCursor;
    }
}
