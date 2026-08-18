package com.workit.domain.recommendation.accommodation.mapper;

import com.workit.domain.recommendation.accommodation.vo.AccommodationConditionVO;
import com.workit.domain.recommendation.accommodation.vo.RecommendationMerchantVO;
import com.workit.domain.recommendation.accommodation.vo.RecommendationRequestVO;
import com.workit.domain.recommendation.accommodation.vo.RecommendationResultVO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

public interface AccommodationRecommendationMapper {
    AccommodationConditionVO selectAccommodationCondition(@Param("userId") Long userId);

    List<RecommendationMerchantVO> selectAvailableAccommodations(AccommodationConditionVO condition);

    List<RecommendationMerchantVO> selectAvailableAccommodationsByMerchantIds(@Param("merchantIds") List<Long> merchantIds);

    RecommendationMerchantVO selectConfirmedOffice(@Param("userId") Long userId,
                                                 @Param("workationId") Long workationId);

    List<RecommendationMerchantVO> selectReferenceCandidates(@Param("userId") Long userId,
                                                           @Param("workationId") Long workationId,
                                                           @Param("regionId") Long regionId);

    RecommendationMerchantVO selectOfficeInRegion(@Param("merchantId") Long merchantId,
                                                 @Param("regionId") Long regionId);

    void insertRecommendationRequest(RecommendationRequestVO request);

    int insertRecommendationResults(@Param("requestId") Long requestId,
                                  @Param("results") List<RecommendationResultVO> results);

    RecommendationRequestVO selectRecommendationRequest(@Param("requestId") Long requestId,
                                                      @Param("userId") Long userId);

    RecommendationRequestVO selectLatestAccommodationRequest(@Param("userId") Long userId,
                                                           @Param("workationId") Long workationId,
                                                           @Param("referenceMerchantId") Long referenceMerchantId);

    List<RecommendationResultVO> selectRecommendationResults(@Param("requestId") Long requestId,
                                                             @Param("cursorRanking") Integer cursorRanking,
                                                             @Param("cursorId") Long cursorId,
                                                             @Param("startDate") LocalDate startDate,
                                                             @Param("endDate") LocalDate endDate,
                                                             @Param("requiredDateCount") int requiredDateCount,
                                                             @Param("headcount") int headcount,
                                                             @Param("roomCount") int roomCount,
                                                             @Param("limit") int limit);
}
