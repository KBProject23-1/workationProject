package com.workit.domain.review.service;

import com.workit.domain.review.dto.response.MerchantReviewListResponseDTO;
import com.workit.domain.review.dto.response.ReviewDetailResponseDTO;

public interface ReviewService {

    MerchantReviewListResponseDTO findMerchantReviewList(Long merchantId);

    ReviewDetailResponseDTO findReviewDetails(Long reviewId);
}
