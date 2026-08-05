package com.workit.domain.recommendation.activities.dto.response;

import com.workit.domain.recommendation.enums.ReferenceType;
import com.workit.domain.recommendation.activities.vo.ActivityRecommendationResultVO;
import com.workit.domain.recommendation.vo.RecommendationRequestVO;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public class ActivityRecommendationResponseDTO {

    private Long recommendationRequestId;
    private String recommendationType;
    private Reference reference;
    private List<Item> content;
    private PageInfo pageInfo;
    private LocalDateTime createdAt;

    public static ActivityRecommendationResponseDTO of(RecommendationRequestVO request,
                                                     List<ActivityRecommendationResultVO> results,
                                                     int size,
                                                     boolean hasNext,
                                                     String nextCursor) {
        List<Item> content = results.stream()
                .map(ActivityRecommendationResponseDTO.Item::from)
                .collect(Collectors.toList());
        return new ActivityRecommendationResponseDTO(
                request == null ? null : request.getId(),
                request == null || request.getRecommendationType() == null
                        ? null
                        : request.getRecommendationType().name(),
                Reference.from(request),
                content,
                new PageInfo(size, content.size(), hasNext, nextCursor),
                request == null ? null : request.getCreatedAt()
        );
    }

    @Getter
    @AllArgsConstructor
    public static class Reference {
        private ReferenceType referenceType;
        private Long primaryMerchantId;
        private String primaryMerchantName;
        private Long secondaryMerchantId;
        private String secondaryMerchantName;
        private BigDecimal latitude;
        private BigDecimal longitude;
        private String description;

        private static Reference from(RecommendationRequestVO request) {
            if (request == null) {
                return null;
            }
            String description;
            if (request.getReferenceType() == ReferenceType.REGION_ONLY) {
                description = "확정된 숙소가 없어 워케이션 지역을 기준으로 추천했어요.";
            } else if (request.getReferenceType() == ReferenceType.USER_SELECTED) {
                description = "선택된 장소를 기준으로 추천했어요.";
            } else {
                description = "확정 예약된 숙소를 기준으로 추천했어요.";
            }
            return new Reference(
                    request.getReferenceType(),
                    request.getReferenceMerchantId(),
                    request.getReferenceMerchantName(),
                    request.getSecondaryReferenceMerchantId(),
                    request.getSecondaryReferenceMerchantName(),
                    request.getReferenceLatitude(),
                    request.getReferenceLongitude(),
                    description
            );
        }
    }

    @Getter
    @AllArgsConstructor
    public static class Item {
        private Long recommendationResultId;
        private Integer ranking;
        private Long merchantId;
        private String merchantName;
        private String thumbnailUrl;
        private String address;
        private Long price;
        private BigDecimal rating;
        private Long reviewCount;
        private Long distanceMeters;
        private Integer estimatedTravelMinutes;
        private String activityType;
        private String difficulty;
        private List<String> activityTags;
        private Score score;
        private Boolean bookmarked;
        private LocalDateTime calculatedAt;

        private static Item from(ActivityRecommendationResultVO result) {
            Long distanceMeters = result.getDistance() == null ? null
                    : result.getDistance().multiply(BigDecimal.valueOf(1000))
                    .setScale(0, RoundingMode.HALF_UP).longValue();

            return new Item(
                    result.getRecommendationResultId(),
                    result.getRanking(),
                    result.getMerchantId(),
                    result.getMerchantName(),
                    result.getThumbnailUrl(),
                    result.getAddress(),
                    result.getPrice() == null ? null : result.getPrice().longValue(),
                    result.getRating(),
                    result.getReviewCount(),
                    distanceMeters,
                    estimateTravelMinutes(result.getDistance()),
                    result.getActivityType(),
                    result.getDifficulty(),
                    tags(result.getActivityType(), result.getActivityTags()),
                    Score.from(result),
                    result.isBookmarked(),
                    result.getCalculatedAt()
            );
        }

        private static Integer estimateTravelMinutes(BigDecimal distanceKm) {
            if (distanceKm == null) {
                return null;
            }
            if (distanceKm.compareTo(BigDecimal.valueOf(6)) <= 0) {
                return 15;
            }
            if (distanceKm.compareTo(BigDecimal.valueOf(12)) <= 0) {
                return 22;
            }
            if (distanceKm.compareTo(BigDecimal.valueOf(18)) <= 0) {
                return 35;
            }
            if (distanceKm.compareTo(BigDecimal.valueOf(23)) <= 0) {
                return 51;
            }
            if (distanceKm.compareTo(BigDecimal.valueOf(29)) <= 0) {
                return 68;
            }
            return 90;
        }

        private static List<String> tags(String activityType, String activityTags) {
            if (activityTags != null && !activityTags.trim().isEmpty()) {
                return Arrays.asList(activityTags.split("\\s*,\\s*"));
            }
            if (activityType == null || activityType.trim().isEmpty()) {
                return Collections.emptyList();
            }
            return new ArrayList<>(Collections.singletonList(activityType));
        }
    }

    @Getter
    @AllArgsConstructor
    public static class Score {
        private BigDecimal priceScore;
        private BigDecimal preferenceScore;
        private BigDecimal accessibilityScore;
        private BigDecimal ratingScore;
        private BigDecimal totalScore;

        private static Score from(ActivityRecommendationResultVO result) {
            return new Score(
                    result.getPriceScore(),
                    result.getPreferenceScore(),
                    result.getAccessibilityScore(),
                    result.getRatingScore(),
                    result.getTotalScore()
            );
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
