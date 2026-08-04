package com.workit.domain.review.service;

import com.workit.domain.review.dto.response.MerchantReviewListResponseDTO;
import com.workit.domain.review.dto.response.MyReviewListResponseDTO;
import com.workit.domain.review.dto.response.ReviewDetailResponseDTO;
import com.workit.global.dto.PageResponseDTO;

public interface ReviewService {

    MerchantReviewListResponseDTO findMerchantReviewList(Long merchantId, int page, int size);

    ReviewDetailResponseDTO findReviewDetails(Long reviewId);

    PageResponseDTO<MyReviewListResponseDTO> findMyReviewList(Long userId, int page, int size);
}
