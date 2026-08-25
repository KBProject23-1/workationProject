package com.workit.domain.recommendation.activities.service;

import com.workit.domain.recommendation.activities.vo.ActivityCandidateVO;
import com.workit.domain.recommendation.activities.vo.ActivityRecommendationResultVO;
import com.workit.domain.recommendation.activities.vo.RecommendationMerchantVO;
import com.workit.domain.recommendation.activities.vo.ReferenceType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ActivityScoreCalculator {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    public List<ActivityRecommendationResultVO> calculate(List<RecommendationMerchantVO> candidates,
                                                 BigDecimal leisureBudget,
                                                 String priority,
                                                 ReferenceType referenceType,
                                                  BigDecimal referenceLatitude,
                                                  BigDecimal referenceLongitude,
                                                  Set<String> selectedActivities,
                                                  boolean hasPreference) {
        if (candidates == null || candidates.isEmpty()) {
            return new ArrayList<>();
        }

        BigDecimal[] weights = resolveWeights(priority, referenceType != ReferenceType.REGION_ONLY, hasPreference);
        BigDecimal priceWeight = weights[0];
        BigDecimal preferenceWeight = weights[1];
        BigDecimal accessibilityWeight = weights[2];
        BigDecimal ratingWeight = weights[3];

        List<ActivityRecommendationResultVO> results = candidates.stream()
                .map(candidate -> {
                    ActivityCandidateVO activityCandidate = candidate instanceof ActivityCandidateVO
                            ? (ActivityCandidateVO) candidate
                            : null;
                    ActivityRecommendationResultVO result = new ActivityRecommendationResultVO();
                    result.setMerchantId(candidate.getMerchantId());
                    result.setMerchantName(candidate.getMerchantName());
                    result.setAddress(candidate.getAddress());
                    result.setThumbnailUrl(candidate.getThumbnailUrl());
                    result.setPrice(candidate.getPrice());
                    result.setRating(candidate.getRating());
                    result.setActivityType(activityCandidate == null ? null : activityCandidate.getActivityType());
                    result.setReviewCount(activityCandidate == null ? null : activityCandidate.getReviewCount());
                    result.setDifficulty(activityCandidate == null ? null : activityCandidate.getDifficulty());

                    BigDecimal priceScore = calculatePriceScore(candidate.getPrice(), leisureBudget);
                    BigDecimal preferenceScore = hasPreference
                            ? calculatePreferenceScore(
                                    activityCandidate == null ? null : activityCandidate.getActivityType(),
                                    selectedActivities)
                            : null;
                    BigDecimal accessibilityScore = calculateAccessibilityScore(
                            referenceType,
                            candidate.getLatitude(),
                            candidate.getLongitude(),
                            referenceLatitude,
                            referenceLongitude
                    );
                    BigDecimal ratingScore = calculateRatingScore(candidate.getRating());
                    BigDecimal distance = calculateDistance(referenceType, candidate, referenceLatitude, referenceLongitude);

                    BigDecimal totalScore = priceScore.multiply(priceWeight)
                            .add(getSafe(preferenceScore).multiply(preferenceWeight))
                            .add(getSafe(accessibilityScore).multiply(accessibilityWeight))
                            .add(ratingScore.multiply(ratingWeight));

                    result.setPriceScore(priceScore);
                    result.setPreferenceScore(preferenceScore);
                    result.setAccessibilityScore(accessibilityScore);
                    result.setRatingScore(ratingScore);
                    result.setTotalScore(totalScore.setScale(2, RoundingMode.HALF_UP));
                    result.setDistance(distance);
                    result.setLatitude(candidate.getLatitude());
                    result.setLongitude(candidate.getLongitude());
                    return result;
                })
                .collect(Collectors.toList());

        results.sort(Comparator.comparing(ActivityRecommendationResultVO::getTotalScore).reversed()
                .thenComparing(ActivityRecommendationResultVO::getMerchantId));
        for (int index = 0; index < results.size(); index++) {
            results.get(index).setRanking(index + 1);
        }
        return results;
    }

    private BigDecimal calculatePriceScore(BigDecimal price, BigDecimal budget) {
        if (price == null || budget == null || budget.compareTo(ZERO) <= 0) {
            return ZERO;
        }
        if (price.compareTo(budget) <= 0) {
            return HUNDRED;
        }

        BigDecimal over = price.subtract(budget);
        BigDecimal discountRate = over.multiply(HUNDRED).divide(budget, 4, RoundingMode.HALF_UP);
        BigDecimal score = HUNDRED.subtract(discountRate);
        if (score.compareTo(ZERO) < 0) {
            return ZERO;
        }
        return score.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculatePreferenceScore(String activityType, Set<String> selectedActivities) {
        if (activityType == null || selectedActivities == null || selectedActivities.isEmpty()) {
            return HUNDRED;
        }

        for (String selected : selectedActivities) {
            if (selected == null) {
                continue;
            }
            if (isExactMatch(activityType, selected)) {
                return HUNDRED;
            }
        }

        for (String selected : selectedActivities) {
            if (selected == null) {
                continue;
            }
            if (isRelatedMatch(activityType, selected)) {
                return BigDecimal.valueOf(70);
            }
        }

        return BigDecimal.valueOf(30);
    }

    private boolean isExactMatch(String activityType, String selected) {
        String normalizedActivity = normalize(selected);
        return normalizedActivity.equals(normalize(activityType));
    }

    private boolean isRelatedMatch(String activityType, String selected) {
        String normalizedActivity = normalize(activityType);
        String normalizedSelected = normalize(selected);

        if (normalizedActivity == null || normalizedSelected == null) {
            return false;
        }

        if ("ACTIVITY".equals(normalizedSelected) || "LEISURE".equals(normalizedSelected)) {
            return true;
        }

        return false;
    }

    private BigDecimal calculateAccessibilityScore(ReferenceType referenceType,
                                                 BigDecimal candidateLatitude,
                                                 BigDecimal candidateLongitude,
                                                 BigDecimal referenceLatitude,
                                                 BigDecimal referenceLongitude) {
        if (referenceType == ReferenceType.REGION_ONLY) {
            return null;
        }
        if (candidateLatitude == null || candidateLongitude == null
                || referenceLatitude == null || referenceLongitude == null) {
            return null;
        }

        double distanceKm = calculateDistanceKm(
                referenceLatitude.doubleValue(),
                referenceLongitude.doubleValue(),
                candidateLatitude.doubleValue(),
                candidateLongitude.doubleValue()
        );

        if (distanceKm <= 6) {
            return HUNDRED;
        }
        if (distanceKm <= 12) {
            return BigDecimal.valueOf(90);
        }
        if (distanceKm <= 18) {
            return BigDecimal.valueOf(75);
        }
        if (distanceKm <= 23) {
            return BigDecimal.valueOf(55);
        }
        if (distanceKm <= 29) {
            return BigDecimal.valueOf(30);
        }
        return BigDecimal.valueOf(10);
    }

    private BigDecimal calculateDistance(ReferenceType referenceType,
                                        RecommendationMerchantVO candidate,
                                        BigDecimal referenceLatitude,
                                        BigDecimal referenceLongitude) {
        if (referenceType == ReferenceType.REGION_ONLY
                || candidate.getLatitude() == null || candidate.getLongitude() == null
                || referenceLatitude == null || referenceLongitude == null) {
            return null;
        }

        double km = calculateDistanceKm(
                referenceLatitude.doubleValue(),
                referenceLongitude.doubleValue(),
                candidate.getLatitude().doubleValue(),
                candidate.getLongitude().doubleValue()
        );
        return BigDecimal.valueOf(km).setScale(3, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateRatingScore(BigDecimal rating) {
        if (rating == null || rating.compareTo(ZERO) < 0) {
            return ZERO;
        }
        return rating.multiply(HUNDRED)
                .divide(BigDecimal.valueOf(5), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal[] resolveWeights(String priority,
                                       boolean accessibilityUsed,
                                       boolean preferenceUsed) {
        String normalized = normalize(priority);

        BigDecimal price;
        BigDecimal preference;
        BigDecimal accessibility;
        BigDecimal rating;

        if ("이동편의".equals(normalized) || "이동".equals(normalized) || "MOVING_CONVENIENCE".equals(normalized)
                || "편의".equals(normalized) || "ACCESSIBILITY".equals(normalized)) {
            price = BigDecimal.valueOf(0.20);
            preference = BigDecimal.valueOf(0.25);
            accessibility = BigDecimal.valueOf(0.45);
            rating = BigDecimal.valueOf(0.10);
        } else if ("높은평점".equals(normalized) || "HIGH_RATING".equals(normalized)
                || "RATING".equals(normalized)) {
            price = BigDecimal.valueOf(0.20);
            preference = BigDecimal.valueOf(0.25);
            accessibility = BigDecimal.valueOf(0.20);
            rating = BigDecimal.valueOf(0.35);
        } else if ("균형".equals(normalized) || "균형있게".equals(normalized)
                || "BALANCED".equals(normalized) || "BALANCE".equals(normalized)) {
            price = BigDecimal.valueOf(0.20);
            preference = BigDecimal.valueOf(0.45);
            accessibility = BigDecimal.valueOf(0.25);
            rating = BigDecimal.valueOf(0.10);
        } else {
            price = BigDecimal.valueOf(0.45);
            preference = BigDecimal.valueOf(0.25);
            accessibility = BigDecimal.valueOf(0.20);
            rating = BigDecimal.valueOf(0.10);
        }

        return new BigDecimal[]{price, preferenceUsed ? preference : ZERO,
                accessibilityUsed ? accessibility : ZERO, rating};
    }

    private double calculateDistanceKm(double fromLatitude,
                                     double fromLongitude,
                                     double toLatitude,
                                     double toLongitude) {
        double earthRadius = 6371.0;
        double latDelta = Math.toRadians(toLatitude - fromLatitude);
        double lonDelta = Math.toRadians(toLongitude - fromLongitude);
        double a = Math.sin(latDelta / 2) * Math.sin(latDelta / 2)
                + Math.cos(Math.toRadians(fromLatitude)) * Math.cos(Math.toRadians(toLatitude))
                * Math.sin(lonDelta / 2) * Math.sin(lonDelta / 2);
        return earthRadius * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private BigDecimal getSafe(BigDecimal value) {
        return value == null ? BigDecimal.valueOf(0) : value;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase().replaceAll("\\s+", "");
    }
}
