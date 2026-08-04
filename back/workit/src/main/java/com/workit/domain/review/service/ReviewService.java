package com.workit.domain.review.service;

import com.workit.domain.review.dto.response.MerchantReviewListResponseDTO;
import com.workit.domain.review.dto.response.MyReviewListResponseDTO;
import com.workit.domain.review.dto.response.ReviewDetailResponseDTO;

import java.util.List;

public interface ReviewService {

    MerchantReviewListResponseDTO findMerchantReviewList(Long merchantId);

    ReviewDetailResponseDTO findReviewDetails(Long reviewId);

    List<MyReviewListResponseDTO> findMyReviewList(Long userId);
}
