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

class AccommodationScoreCalculatorTest {

    private final AccommodationScoreCalculator calculator = new AccommodationScoreCalculator();

    @Test
    void 예산_이하_가격은_백점이다() {
        assertEquals(new BigDecimal("100.00"), calculator.calculatePriceScore(
                new BigDecimal("90000"), new BigDecimal("100000")));
    }

    @Test
    void 예산을_초과한_가격은_초과율만큼_감점한다() {
        assertEquals(new BigDecimal("50.00"), calculator.calculatePriceScore(
                new BigDecimal("150000"), new BigDecimal("100000")));
    }

    @Test
    void 접근성_경계값을_적용한다() {
        assertEquals(new BigDecimal("100.00"), calculator.calculateAccessibilityScore(new BigDecimal("0.400")));
        assertEquals(new BigDecimal("90.00"), calculator.calculateAccessibilityScore(new BigDecimal("0.401")));
        assertEquals(new BigDecimal("15.00"), calculator.calculateAccessibilityScore(new BigDecimal("5.001")));
    }

    @Test
    void 지역_기반은_거리와_접근성_점수를_저장하지_않는다() {
        List<RecommendationResultVO> results = calculator.calculate(
                Arrays.asList(merchant(1L, "90000", "4.5")), new BigDecimal("100000"),
                "BUDGET", ReferenceType.REGION_ONLY, null, null);

        assertNull(results.get(0).getDistance());
        assertNull(results.get(0).getAccessibilityScore());
        assertEquals(new BigDecimal("97.69"), results.get(0).getTotalScore());
    }

    @Test
    void 최종_점수_내림차순으로_순위를_정한다() {
        List<RecommendationResultVO> results = calculator.calculate(
                Arrays.asList(merchant(1L, "150000", "3.0"), merchant(2L, "90000", "4.5")),
                new BigDecimal("100000"), "BALANCE", ReferenceType.REGION_ONLY, null, null);

        assertEquals(Long.valueOf(2L), results.get(0).getMerchantId());
        assertEquals(Integer.valueOf(1), results.get(0).getRanking());
        assertTrue(results.get(0).getTotalScore().compareTo(results.get(1).getTotalScore()) > 0);
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
