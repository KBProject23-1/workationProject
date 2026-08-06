package com.workit.domain.recommendation.dto.response;

import com.workit.domain.recommendation.enums.MealType;
import com.workit.domain.recommendation.enums.RecommendationType;
import com.workit.domain.recommendation.enums.ReferenceType;
import com.workit.domain.recommendation.vo.RecommendationRequestVO;
import com.workit.domain.recommendation.vo.RecommendationResultVO;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public class RestaurantRecommendationResponseDTO {
    private Long recommendationRequestId;
    private RecommendationType recommendationType;
    private MealType mealType;
    private Reference reference;
    private List<Item> content;
    private PageInfo pageInfo;
    private LocalDateTime createdAt;

    public static RestaurantRecommendationResponseDTO of(RecommendationRequestVO request,
                                                           List<RecommendationResultVO> results,
                                                           int size, boolean hasNext, String nextCursor) {
        List<Item> content = results.stream().map(item -> Item.from(item, request.getMealType()))
                .collect(Collectors.toList());
        return new RestaurantRecommendationResponseDTO(request.getId(), request.getRecommendationType(),
                request.getMealType(), Reference.from(request), content,
                new PageInfo(size, content.size(), hasNext, nextCursor), request.getCreatedAt());
    }

    @Getter
    @AllArgsConstructor
    public static class Reference {
        private ReferenceType referenceType;
        private Long merchantId;
        private Long secondaryMerchantId;
        private String merchantName;
        private String secondaryMerchantName;
        private BigDecimal latitude;
        private BigDecimal longitude;
        private String description;

        static Reference from(RecommendationRequestVO request) {
            String description;
            if (request.getReferenceType() == ReferenceType.AUTO_MIDPOINT) {
                description = "확정 숙소와 공유오피스의 중간 지점을 기준으로 추천했어요.";
            } else if (request.getReferenceType() == ReferenceType.REGION_ONLY) {
                description = "기준 장소가 없어 워케이션 지역을 기준으로 추천했어요.";
            } else {
                description = "선택된 장소를 기준으로 추천했어요.";
            }
            return new Reference(request.getReferenceType(), request.getReferenceMerchantId(),
                    request.getSecondaryReferenceMerchantId(), request.getReferenceMerchantName(),
                    request.getSecondaryReferenceMerchantName(), request.getReferenceLatitude(),
                    request.getReferenceLongitude(), description);
        }
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
        private Long distanceMeters;
        private Score score;
        private String recommendationReason;
        private boolean bookmarked;

        static Item from(RecommendationResultVO result, MealType mealType) {
            Long meters = result.getDistance() == null ? null : result.getDistance()
                    .multiply(BigDecimal.valueOf(1000)).setScale(0, RoundingMode.HALF_UP).longValue();
            String meal = mealType == MealType.BREAKFAST ? "아침" : mealType == MealType.LUNCH ? "점심" : "저녁";
            String reason = meters == null ? meal + " 예산과 평점을 반영한 음식점이에요."
                    : meal + " 예산을 반영했으며 기준 지점에서 " + meters + "m 떨어진 음식점이에요.";
            return new Item(result.getRecommendationResultId(), result.getRanking(), result.getMerchantId(),
                    result.getMerchantName(), result.getThumbnailUrl(), result.getAddress(), result.getPrice(),
                    result.getRating(), meters, Score.from(result), reason, result.isBookmarked());
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
