package com.workit.domain.review.controller;

import com.workit.domain.review.dto.response.MerchantReviewListResponseDTO;
import com.workit.domain.review.dto.response.MyReviewListResponseDTO;
import com.workit.domain.review.dto.response.ReviewDetailResponseDTO;
import com.workit.domain.review.service.ReviewService;
import com.workit.global.dto.CommonResponse;
import com.workit.global.dto.PageResponseDTO;
import com.workit.global.response.GlobalResponseFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    // 특정 가맹점의 활성 리뷰 목록과 평점 통계 조회
    @GetMapping("/merchants/{merchantId}/reviews")
    public ResponseEntity<CommonResponse<MerchantReviewListResponseDTO>> reviewList(
            @PathVariable Long merchantId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        return GlobalResponseFactory.success(
                reviewService.findMerchantReviewList(merchantId, page, size)
        );
    }

    // 리뷰와 작성자, 가맹점, 작성 근거 상세 조회
    @GetMapping("/reviews/{reviewId}")
    public ResponseEntity<CommonResponse<ReviewDetailResponseDTO>> reviewDetails(
            @PathVariable Long reviewId) {
        return GlobalResponseFactory.success(reviewService.findReviewDetails(reviewId));
    }

    // 로그인 사용자가 작성한 활성 리뷰를 가맹점 카테고리별로 조회
    @GetMapping("/users/me/reviews")
    public ResponseEntity<CommonResponse<PageResponseDTO<MyReviewListResponseDTO>>> myReviewList(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        // 인증 기능 연결 전까지 1번 사용자를 사용하며, 이후 JWT 인증 객체에서 추출하도록 교체
        Long userId = 1L;

        return GlobalResponseFactory.success(reviewService.findMyReviewList(userId, page, size));
    }
}
