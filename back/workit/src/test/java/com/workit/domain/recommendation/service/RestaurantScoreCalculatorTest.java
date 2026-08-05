package com.workit.domain.recommendation.service;

import com.workit.domain.recommendation.enums.ReferenceType;
import com.workit.domain.recommendation.vo.RecommendationMerchantVO;
import com.workit.domain.recommendation.vo.RecommendationResultVO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RestaurantScoreCalculatorTest {

    private final RestaurantScoreCalculator calculator =
            new RestaurantScoreCalculator(new AccommodationScoreCalculator());

    @Test
    void 예산_중심_지역_추천은_접근성_가중치를_재분배한다() {
        List<RecommendationResultVO> results = calculator.calculate(
                Arrays.asList(merchant(1L, "18000", "4.5")), new BigDecimal("20000"),
                "BUDGET", ReferenceType.REGION_ONLY, null, null);

        assertEquals(new BigDecimal("97.86"), results.get(0).getTotalScore());
        assertNull(results.get(0).getAccessibilityScore());
        assertNull(results.get(0).getDistance());
    }

    @Test
    void 기준_좌표가_있으면_음식점_거리와_접근성을_계산한다() {
        RecommendationMerchantVO restaurant = merchant(1L, "18000", "4.5");
        restaurant.setLatitude(new BigDecimal("33.49170000"));
        restaurant.setLongitude(new BigDecimal("126.51240000"));

        List<RecommendationResultVO> results = calculator.calculate(Arrays.asList(restaurant),
                new BigDecimal("20000"), "BALANCE", ReferenceType.AUTO_MERCHANT,
                new BigDecimal("33.48810000"), new BigDecimal("126.51240000"));

        assertTrue(results.get(0).getDistance().compareTo(BigDecimal.ZERO) > 0);
        assertEquals(new BigDecimal("100.00"), results.get(0).getAccessibilityScore());
    }

    @Test
    void 최종_점수_내림차순으로_순위를_저장한다() {
        List<RecommendationResultVO> results = calculator.calculate(Arrays.asList(
                        merchant(1L, "40000", "3.0"), merchant(2L, "18000", "4.8")),
                new BigDecimal("20000"), "RATING", ReferenceType.REGION_ONLY, null, null);

        assertEquals(Long.valueOf(2L), results.get(0).getMerchantId());
        assertEquals(Integer.valueOf(1), results.get(0).getRanking());
    }

    private RecommendationMerchantVO merchant(Long id, String price, String rating) {
        RecommendationMerchantVO merchant = new RecommendationMerchantVO();
        merchant.setMerchantId(id);
        merchant.setPrice(new BigDecimal(price));
        merchant.setRating(new BigDecimal(rating));
        merchant.setLatitude(new BigDecimal("33.48810000"));
        merchant.setLongitude(new BigDecimal("126.51240000"));
        return merchant;
    }
}
