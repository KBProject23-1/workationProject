package com.workit.domain.review.service;

import com.workit.domain.review.dto.request.ReviewCreateRequestDTO;
import com.workit.domain.review.dto.request.ReviewUpdateRequestDTO;
import com.workit.domain.review.dto.response.MerchantReviewListResponseDTO;
import com.workit.domain.review.dto.response.MyReviewListResponseDTO;
import com.workit.domain.review.dto.response.ReviewCreateResponseDTO;
import com.workit.domain.review.dto.response.ReviewDetailResponseDTO;
import com.workit.domain.review.dto.response.ReviewUpdateResponseDTO;
import com.workit.global.dto.PageResponseDTO;

public interface ReviewService {

    MerchantReviewListResponseDTO findMerchantReviewList(Long merchantId, int page, int size);

    ReviewDetailResponseDTO findReviewDetails(Long userId, Long reviewId);

    PageResponseDTO<MyReviewListResponseDTO> findMyReviewList(
            Long userId,
            String category,
            int page,
            int size
    );

    ReviewCreateResponseDTO addReservationReview(
            Long userId,
            Long reservationId,
            ReviewCreateRequestDTO request
    );

    ReviewCreateResponseDTO addTransactionReview(
            Long userId,
            Long transactionId,
            ReviewCreateRequestDTO request
    );

    ReviewUpdateResponseDTO modifyReview(
            Long userId,
            Long reviewId,
            ReviewUpdateRequestDTO request
    );

    void removeReview(Long userId, Long reviewId);
}
