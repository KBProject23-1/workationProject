package com.workit.domain.recommendation.activities.mapper;

import com.workit.domain.recommendation.activities.vo.ActivityCandidateVO;
import com.workit.domain.recommendation.activities.vo.ActivityConditionVO;
import com.workit.domain.recommendation.activities.vo.ActivityRecommendationResultVO;
import com.workit.domain.recommendation.vo.RecommendationMerchantVO;
import com.workit.domain.recommendation.vo.RecommendationRequestVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ActivityRecommendationMapper {

    ActivityConditionVO selectActivityCondition(@Param("userId") Long userId,
                                               @Param("workationId") Long workationId);

    List<ActivityCandidateVO> selectActivities(@Param("regionId") Long regionId);

    RecommendationMerchantVO selectActivityReferenceMerchant(@Param("merchantId") Long merchantId,
                                                           @Param("regionId") Long regionId);

    RecommendationRequestVO selectLatestActivityRequest(@Param("userId") Long userId,
                                                       @Param("workationId") Long workationId,
                                                       @Param("referenceMerchantId") Long referenceMerchantId);

    int insertRecommendationResults(@Param("requestId") Long requestId,
                                   @Param("results") List<ActivityRecommendationResultVO> results);

    List<ActivityRecommendationResultVO> selectRecommendationResults(@Param("requestId") Long requestId,
                                                                    @Param("cursorRanking") Integer cursorRanking,
                                                                    @Param("cursorId") Long cursorId,
                                                                    @Param("limit") int limit);
}
