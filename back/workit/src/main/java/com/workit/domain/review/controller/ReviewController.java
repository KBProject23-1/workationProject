package com.workit.domain.review.controller;

import com.workit.domain.review.dto.response.MerchantReviewListResponseDTO;
import com.workit.domain.review.dto.response.ReviewDetailResponseDTO;
import com.workit.domain.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    // 특정 가맹점의 활성 리뷰 목록과 평점 통계 조회
    @GetMapping("/merchants/{merchantId}/reviews")
    public ResponseEntity<MerchantReviewListResponseDTO> reviewList(
            @PathVariable Long merchantId) {
        return ResponseEntity.ok(reviewService.findMerchantReviewList(merchantId));
    }

    // 리뷰와 작성자, 가맹점, 작성 근거 상세 조회
    @GetMapping("/reviews/{reviewId}")
    public ResponseEntity<ReviewDetailResponseDTO> reviewDetails(
            @PathVariable Long reviewId) {
        return ResponseEntity.ok(reviewService.findReviewDetails(reviewId));
    }
}
