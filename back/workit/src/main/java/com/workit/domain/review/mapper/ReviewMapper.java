package com.workit.domain.review.mapper;

import com.workit.domain.review.dto.request.ReviewUpdateRequestDTO;
import com.workit.domain.review.vo.MerchantReviewStatisticsVO;
import com.workit.domain.review.vo.MerchantReviewVO;
import com.workit.domain.review.vo.MyReviewListItemVO;
import com.workit.domain.review.vo.OwnedReviewVO;
import com.workit.domain.review.vo.ReservationReviewSourceVO;
import com.workit.domain.review.vo.ReviewDetailVO;
import com.workit.domain.review.vo.ReviewVO;
import com.workit.domain.review.vo.TransactionReviewSourceVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ReviewMapper {

    boolean selectMerchantExists(@Param("merchantId") Long merchantId);

    List<MerchantReviewVO> selectMerchantReviewList(
            @Param("merchantId") Long merchantId,
            @Param("offset") int offset,
            @Param("size") int size
    );

    MerchantReviewStatisticsVO selectMerchantReviewStatistics(
            @Param("merchantId") Long merchantId
    );

    ReviewDetailVO selectReviewDetails(@Param("reviewId") Long reviewId);

    List<MyReviewListItemVO> selectMyReviewList(
            @Param("userId") Long userId,
            @Param("offset") int offset,
            @Param("size") int size
    );

    long countMyReviewList(@Param("userId") Long userId);

    ReservationReviewSourceVO selectReservationReviewSource(
            @Param("userId") Long userId,
            @Param("reservationId") Long reservationId
    );

    TransactionReviewSourceVO selectTransactionReviewSource(
            @Param("userId") Long userId,
            @Param("transactionId") Long transactionId
    );

    void insertReview(ReviewVO review);

    OwnedReviewVO selectOwnedReview(@Param("userId") Long userId,
                                    @Param("reviewId") Long reviewId);

    int updateReview(@Param("reviewId") Long reviewId,
                     @Param("request") ReviewUpdateRequestDTO request);

    int updateReviewStatusDeleted(@Param("userId") Long userId,
                                  @Param("reviewId") Long reviewId);
}
