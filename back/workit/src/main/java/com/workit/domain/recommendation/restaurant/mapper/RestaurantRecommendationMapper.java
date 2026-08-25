package com.workit.domain.recommendation.restaurant.mapper;

import com.workit.domain.recommendation.restaurant.vo.RecommendationMerchantVO;
import com.workit.domain.recommendation.restaurant.vo.RecommendationRequestVO;
import com.workit.domain.recommendation.restaurant.vo.RecommendationResultVO;
import com.workit.domain.recommendation.restaurant.vo.RestaurantConditionVO;
import com.workit.domain.recommendation.restaurant.vo.MealType;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface RestaurantRecommendationMapper {
    RestaurantConditionVO selectRestaurantCondition(@Param("userId") Long userId);

    List<RecommendationMerchantVO> selectRestaurants(@Param("regionId") Long regionId);

    RecommendationMerchantVO selectConfirmedAccommodation(@Param("userId") Long userId,
                                                         @Param("workationId") Long workationId);

    RecommendationMerchantVO selectConfirmedOffice(@Param("userId") Long userId,
                                                   @Param("workationId") Long workationId);

    RecommendationMerchantVO selectRestaurantReferenceMerchant(@Param("merchantId") Long merchantId,
                                                               @Param("regionId") Long regionId);

    List<RecommendationMerchantVO> selectRestaurantReferenceCandidates(@Param("userId") Long userId,
                                                                       @Param("workationId") Long workationId,
                                                                       @Param("regionId") Long regionId);

    void insertRecommendationRequest(RecommendationRequestVO request);

    int insertRecommendationResults(@Param("requestId") Long requestId,
                                  @Param("results") List<RecommendationResultVO> results);

    RecommendationRequestVO selectRecommendationRequest(@Param("requestId") Long requestId,
                                                      @Param("userId") Long userId);

    RecommendationRequestVO selectLatestRestaurantRequest(@Param("userId") Long userId,
                                                           @Param("referenceMerchantId") Long referenceMerchantId,
                                                           @Param("mealType") MealType mealType);

    List<RecommendationResultVO> selectRecommendationResults(@Param("requestId") Long requestId,
                                                             @Param("cursorRanking") Integer cursorRanking,
                                                             @Param("cursorId") Long cursorId,
                                                             @Param("limit") int limit);
}
