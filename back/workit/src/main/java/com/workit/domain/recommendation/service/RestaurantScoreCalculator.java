package com.workit.domain.recommendation.service;

import com.workit.domain.recommendation.enums.ReferenceType;
import com.workit.domain.recommendation.vo.RecommendationMerchantVO;
import com.workit.domain.recommendation.vo.RecommendationResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RestaurantScoreCalculator {

    private final AccommodationScoreCalculator commonCalculator;

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
            BigDecimal priceScore = commonCalculator.calculatePriceScore(candidate.getPrice(), mealBudget);
            BigDecimal ratingScore = commonCalculator.calculateRatingScore(candidate.getRating());
            BigDecimal distance = null;
            BigDecimal accessibilityScore = null;
            if (referenceType != ReferenceType.REGION_ONLY) {
                distance = commonCalculator.calculateDistance(referenceLatitude, referenceLongitude,
                        candidate.getLatitude(), candidate.getLongitude());
                accessibilityScore = commonCalculator.calculateAccessibilityScore(distance);
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
