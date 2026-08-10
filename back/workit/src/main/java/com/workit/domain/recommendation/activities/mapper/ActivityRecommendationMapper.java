package com.workit.domain.recommendation.activities.mapper;

import com.workit.domain.recommendation.activities.vo.ActivityCandidateVO;
import com.workit.domain.recommendation.activities.vo.ActivityConditionVO;
import com.workit.domain.recommendation.activities.vo.ActivityRecommendationResultVO;
import com.workit.domain.recommendation.activities.vo.RecommendationMerchantVO;
import com.workit.domain.recommendation.activities.vo.RecommendationRequestVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ActivityRecommendationMapper {

    ActivityConditionVO selectActivityCondition(@Param("userId") Long userId,
                                               @Param("workationId") Long workationId);

    List<ActivityCandidateVO> selectActivities(@Param("regionId") Long regionId);

    RecommendationMerchantVO selectActivityReferenceMerchant(@Param("merchantId") Long merchantId,
                                                           @Param("regionId") Long regionId);

    RecommendationMerchantVO selectConfirmedAccommodation(@Param("userId") Long userId,
                                                         @Param("workationId") Long workationId);

    RecommendationRequestVO selectLatestActivityRequest(@Param("userId") Long userId,
                                                       @Param("workationId") Long workationId,
                                                       @Param("referenceMerchantId") Long referenceMerchantId);

    int insertRecommendationResults(@Param("requestId") Long requestId,
                                   @Param("results") List<ActivityRecommendationResultVO> results);

    void insertRecommendationRequest(RecommendationRequestVO request);

    RecommendationRequestVO selectRecommendationRequest(@Param("requestId") Long requestId,
                                                      @Param("userId") Long userId);

    List<ActivityRecommendationResultVO> selectRecommendationResults(@Param("requestId") Long requestId,
                                                                    @Param("cursorRanking") Integer cursorRanking,
                                                                    @Param("cursorId") Long cursorId,
                                                                    @Param("limit") int limit);
}
