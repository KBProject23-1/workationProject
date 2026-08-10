package com.workit.domain.recommendation.restaurant.service;

import com.workit.domain.recommendation.restaurant.vo.ReferenceType;
import com.workit.domain.recommendation.restaurant.vo.RecommendationMerchantVO;
import com.workit.domain.recommendation.restaurant.vo.RecommendationResultVO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class RestaurantScoreCalculator {

    private static final double EARTH_RADIUS_KM = 6371.0088;

    public List<RecommendationResultVO> calculate(List<RecommendationMerchantVO> candidates,
                                                   BigDecimal mealBudget,
                                                   String priority,
                                                   ReferenceType referenceType,
                                                   BigDecimal referenceLatitude,
                                                   BigDecimal referenceLongitude) {
        double[] weights = resolveWeights(priority, referenceType == ReferenceType.REGION_ONLY);
        List<RecommendationResultVO> results = new ArrayList<>();
        for (RecommendationMerchantVO candidate : candidates) {
            RecommendationResultVO result = copy(candidate);
            BigDecimal priceScore = calculatePriceScore(candidate.getPrice(), mealBudget);
            BigDecimal ratingScore = calculateRatingScore(candidate.getRating());
            BigDecimal distance = null;
            BigDecimal accessibilityScore = null;
            if (referenceType != ReferenceType.REGION_ONLY) {
                distance = calculateDistance(referenceLatitude, referenceLongitude,
                        candidate.getLatitude(), candidate.getLongitude());
                accessibilityScore = calculateAccessibilityScore(distance);
            }
            BigDecimal totalScore = priceScore.multiply(BigDecimal.valueOf(weights[0]))
                    .add(ratingScore.multiply(BigDecimal.valueOf(weights[2])));
            if (accessibilityScore != null) {
                totalScore = totalScore.add(accessibilityScore.multiply(BigDecimal.valueOf(weights[1])));
            }
            result.setPriceScore(priceScore);
            result.setAccessibilityScore(accessibilityScore);
            result.setRatingScore(ratingScore);
            result.setDistance(distance);
            result.setTotalScore(totalScore.setScale(2, RoundingMode.HALF_UP));
            results.add(result);
        }
        results.sort(Comparator.comparing(RecommendationResultVO::getTotalScore).reversed()
                .thenComparing(RecommendationResultVO::getMerchantId));
        for (int index = 0; index < results.size(); index++) {
            results.get(index).setRanking(index + 1);
        }
        return results;
    }

    public BigDecimal calculatePriceScore(BigDecimal price, BigDecimal budget) {
        if (price == null || budget == null || budget.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        if (price.compareTo(budget) <= 0) {
            return BigDecimal.valueOf(100).setScale(2);
        }
        BigDecimal excessRate = price.subtract(budget)
                .divide(budget, 8, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        return BigDecimal.valueOf(100).subtract(excessRate)
                .max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateRatingScore(BigDecimal rating) {
        BigDecimal safeRating = rating == null ? BigDecimal.ZERO : rating;
        return safeRating.divide(BigDecimal.valueOf(5), 8, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateDistance(BigDecimal fromLatitude, BigDecimal fromLongitude,
                                 BigDecimal toLatitude, BigDecimal toLongitude) {
        if (fromLatitude == null || fromLongitude == null || toLatitude == null || toLongitude == null) {
            return null;
        }
        double fromLat = Math.toRadians(fromLatitude.doubleValue());
        double toLat = Math.toRadians(toLatitude.doubleValue());
        double latitudeDelta = Math.toRadians(toLatitude.subtract(fromLatitude).doubleValue());
        double longitudeDelta = Math.toRadians(toLongitude.subtract(fromLongitude).doubleValue());
        double a = Math.sin(latitudeDelta / 2) * Math.sin(latitudeDelta / 2)
                + Math.cos(fromLat) * Math.cos(toLat)
                * Math.sin(longitudeDelta / 2) * Math.sin(longitudeDelta / 2);
        double distance = EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return BigDecimal.valueOf(distance).setScale(3, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateAccessibilityScore(BigDecimal distanceKm) {
        if (distanceKm == null) {
            return null;
        }
        double distance = distanceKm.doubleValue();
        if (distance <= 0.4) return BigDecimal.valueOf(100).setScale(2);
        if (distance <= 0.8) return BigDecimal.valueOf(90).setScale(2);
        if (distance <= 1.2) return BigDecimal.valueOf(75).setScale(2);
        if (distance <= 2.0) return BigDecimal.valueOf(55).setScale(2);
        if (distance <= 5.0) return BigDecimal.valueOf(35).setScale(2);
        return BigDecimal.valueOf(15).setScale(2);
    }

    private double[] resolveWeights(String priority, boolean regionOnly) {
        String normalized = priority == null ? "" : priority.toUpperCase();
        double[] weights;
        if (normalized.contains("BUDGET") || normalized.contains("예산")) {
            weights = new double[]{0.55, 0.30, 0.15};
        } else if (normalized.contains("MOVE") || normalized.contains("ACCESS") || normalized.contains("이동")) {
            weights = new double[]{0.35, 0.50, 0.15};
        } else if (normalized.contains("RATING") || normalized.contains("평점")) {
            weights = new double[]{0.30, 0.25, 0.45};
        } else {
            weights = new double[]{0.40, 0.30, 0.30};
        }
        if (regionOnly) {
            double available = weights[0] + weights[2];
            weights[0] = weights[0] / available;
            weights[1] = 0;
            weights[2] = weights[2] / available;
        }
        return weights;
    }

    private RecommendationResultVO copy(RecommendationMerchantVO candidate) {
        RecommendationResultVO result = new RecommendationResultVO();
        result.setMerchantId(candidate.getMerchantId());
        result.setMerchantName(candidate.getMerchantName());
        result.setCategory(candidate.getCategory());
        result.setAddress(candidate.getAddress());
        result.setThumbnailUrl(candidate.getThumbnailUrl());
        result.setPrice(candidate.getPrice());
        result.setRating(candidate.getRating());
        result.setLatitude(candidate.getLatitude());
        result.setLongitude(candidate.getLongitude());
        return result;
    }
}
