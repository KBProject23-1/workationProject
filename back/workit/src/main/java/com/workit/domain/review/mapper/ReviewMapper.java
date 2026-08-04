package com.workit.domain.review.mapper;

import com.workit.domain.review.vo.MerchantReviewStatisticsVO;
import com.workit.domain.review.vo.MerchantReviewVO;
import com.workit.domain.review.vo.ReviewDetailVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ReviewMapper {

    boolean selectMerchantExists(@Param("merchantId") Long merchantId);

    List<MerchantReviewVO> selectMerchantReviewList(@Param("merchantId") Long merchantId);

    MerchantReviewStatisticsVO selectMerchantReviewStatistics(
            @Param("merchantId") Long merchantId
    );

    ReviewDetailVO selectReviewDetails(@Param("reviewId") Long reviewId);
}
