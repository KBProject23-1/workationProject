package com.workit.domain.recommendation.service;

import com.workit.domain.recommendation.accommodation.dto.response.AccommodationRecommendationResponseDTO;
import com.workit.domain.recommendation.accommodation.mapper.AccommodationRecommendationMapper;
import com.workit.domain.recommendation.accommodation.service.AccommodationRecommendationServiceImpl;
import com.workit.domain.recommendation.accommodation.service.AccommodationScoreCalculator;
import com.workit.domain.recommendation.accommodation.vo.AccommodationConditionVO;
import com.workit.domain.recommendation.accommodation.vo.RecommendationMerchantVO;
import com.workit.domain.recommendation.accommodation.vo.RecommendationRequestVO;
import com.workit.domain.recommendation.accommodation.vo.RecommendationResultVO;
import com.workit.domain.recommendation.accommodation.vo.RecommendationType;
import com.workit.domain.recommendation.accommodation.vo.ReferenceType;
import com.workit.domain.recommendation.common.dto.response.RecommendationListResponseDTO;
import com.workit.domain.recommendation.common.mapper.ReservationProductAvailabilityMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccommodationRecommendationServiceImplTest {

    @Test
    void 조회할추천이없으면지역기준추천을생성한다() {
        AccommodationRecommendationMapper recommendationMapper = mock(AccommodationRecommendationMapper.class);
        ReservationProductAvailabilityMapper availabilityMapper = mock(ReservationProductAvailabilityMapper.class);
        AccommodationRecommendationServiceImpl service = new AccommodationRecommendationServiceImpl(
                recommendationMapper,
                availabilityMapper,
                new AccommodationScoreCalculator()
        );

        AccommodationConditionVO condition = new AccommodationConditionVO();
        condition.setWorkationId(5L);
        condition.setRegionId(4L);
        condition.setStartDate(LocalDate.of(2026, 8, 11));
        condition.setEndDate(LocalDate.of(2026, 8, 21));
        condition.setAccommodationBudget(new BigDecimal("1000000"));
        condition.setPriorityOptionCode("ACCESSIBILITY");
        condition.setPriorityOptionName("이동 편의");

        RecommendationMerchantVO candidate = new RecommendationMerchantVO();
        candidate.setMerchantId(2L);
        candidate.setMerchantName("서귀포 힐링 리조트");
        candidate.setCategory("ACCOMMODATION");
        candidate.setPrice(new BigDecimal("120000"));
        candidate.setRating(new BigDecimal("4.8"));

        RecommendationRequestVO savedRequest = new RecommendationRequestVO();
        savedRequest.setId(101L);
        savedRequest.setUserId(1L);
        savedRequest.setWorkationId(5L);
        savedRequest.setRecommendationType(RecommendationType.ACCOMMODATION);
        savedRequest.setReferenceType(ReferenceType.REGION_ONLY);
        savedRequest.setCreatedAt(LocalDateTime.of(2026, 8, 13, 12, 0));

        RecommendationResultVO savedResult = new RecommendationResultVO();
        savedResult.setRecommendationResultId(201L);
        savedResult.setMerchantId(2L);
        savedResult.setMerchantName("서귀포 힐링 리조트");
        savedResult.setPrice(new BigDecimal("120000"));
        savedResult.setRating(new BigDecimal("4.8"));
        savedResult.setPriceScore(new BigDecimal("80.00"));
        savedResult.setRatingScore(new BigDecimal("96.00"));
        savedResult.setTotalScore(new BigDecimal("86.15"));
        savedResult.setRanking(1);

        when(recommendationMapper.selectLatestAccommodationRequest(1L, null)).thenReturn(null);
        when(recommendationMapper.selectAccommodationCondition(1L)).thenReturn(condition);
        when(recommendationMapper.selectConfirmedOffice(1L, 5L)).thenReturn(null);
        when(availabilityMapper.selectAvailableMerchantIdsByPeriod(
                4L, "ACCOMMODATION", "ROOM", condition.getStartDate(), condition.getEndDate(), false, 10
        )).thenReturn(Collections.singletonList(2L));
        when(recommendationMapper.selectAvailableAccommodationsByMerchantIds(Collections.singletonList(2L)))
                .thenReturn(Collections.singletonList(candidate));
        doAnswer(invocation -> {
            RecommendationRequestVO request = invocation.getArgument(0);
            request.setId(101L);
            return null;
        }).when(recommendationMapper).insertRecommendationRequest(any(RecommendationRequestVO.class));
        when(recommendationMapper.selectRecommendationRequest(101L, 1L)).thenReturn(savedRequest);
        when(recommendationMapper.selectRecommendationResults(101L, null, null, 21))
                .thenReturn(Collections.singletonList(savedResult));

        RecommendationListResponseDTO<AccommodationRecommendationResponseDTO.Item> response =
                service.findAccommodationRecommendation(1L, null, null, 20);

        assertEquals(101L, response.getRecommendationRequestId());
        assertEquals("ACCOMMODATION", response.getRecommendationType());
        assertEquals("REGION_ONLY", response.getReference().getReferenceType());
        assertEquals(1, response.getContent().size());
        assertEquals(2L, response.getContent().get(0).getMerchantId());
        verify(recommendationMapper).insertRecommendationRequest(any(RecommendationRequestVO.class));
        verify(recommendationMapper).insertRecommendationResults(eq(101L), anyList());
        verify(recommendationMapper, never()).selectOfficeInRegion(any(), any());
    }
}
