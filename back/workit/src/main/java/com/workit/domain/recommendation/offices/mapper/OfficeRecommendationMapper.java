package com.workit.domain.recommendation.offices.mapper;

import com.workit.domain.recommendation.offices.vo.OfficeCandidateVO;
import com.workit.domain.recommendation.offices.vo.OfficeReferenceMerchantVO;
import com.workit.domain.recommendation.offices.vo.OfficeRecommendationRequestVO;
import com.workit.domain.recommendation.offices.vo.OfficeRecommendationResultVO;
import com.workit.domain.recommendation.offices.vo.OfficeSurveyAnswerVO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

public interface OfficeRecommendationMapper {
    Long selectLatestSurveyIdByWorkation(@Param("userId") Long userId,
                                         @Param("workationId") Long workationId);

    List<OfficeSurveyAnswerVO> selectSurveyAnswersBySurveyId(@Param("surveyId") Long surveyId);

    OfficeReferenceMerchantVO selectAccommodationReference(@Param("userId") Long userId,
                                                           @Param("workationId") Long workationId);

    List<OfficeCandidateVO> selectOfficeCandidates(@Param("userId") Long userId,
                                                   @Param("regionId") Long regionId,
                                                   @Param("startDate") LocalDate startDate,
                                                   @Param("endDate") LocalDate endDate);

    List<OfficeCandidateVO> selectAccommodationReferenceCandidates(@Param("userId") Long userId,
                                                                  @Param("workationId") Long workationId,
                                                                  @Param("regionId") Long regionId,
                                                                  @Param("workationStartDate") LocalDate workationStartDate);

    OfficeReferenceMerchantVO selectReferenceMerchant(@Param("merchantId") Long merchantId,
                                                      @Param("regionId") Long regionId);

    List<OfficeCandidateVO> selectOfficeCandidatesByMerchantIds(@Param("userId") Long userId,
                                                                @Param("merchantIds") List<Long> merchantIds);

    void insertRecommendationRequest(OfficeRecommendationRequestVO requestVO);

    OfficeRecommendationRequestVO selectLatestRecommendationRequestByUser(@Param("userId") Long userId,
                                                                         @Param("workationId") Long workationId,
                                                                         @Param("referenceMerchantId") Long referenceMerchantId);

    OfficeRecommendationRequestVO selectRecommendationRequestById(@Param("userId") Long userId,
                                                                @Param("recommendationRequestId") Long recommendationRequestId);

    OfficeRecommendationRequestVO selectTodayLatestRecommendationRequestByUser(@Param("userId") Long userId,
                                                                              @Param("workationId") Long workationId,
                                                                              @Param("referenceMerchantId") Long referenceMerchantId,
                                                                              @Param("startAt") java.time.LocalDateTime startAt,
                                                                              @Param("endAt") java.time.LocalDateTime endAt);

    int insertRecommendationResults(@Param("requestId") Long requestId,
                                  @Param("results") List<OfficeRecommendationResultVO> results);

    int selectRecommendationResultCountByRequestId(@Param("recommendationRequestId") Long recommendationRequestId);

    List<OfficeRecommendationResultVO> selectRecommendationResultsByCursor(@Param("recommendationRequestId") Long recommendationRequestId,
                                                                           @Param("cursorRanking") Integer cursorRanking,
                                                                           @Param("cursorId") Long cursorId,
                                                                           @Param("userId") Long userId,
                                                                           @Param("size") int size);
}
