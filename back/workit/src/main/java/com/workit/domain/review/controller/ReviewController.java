package com.workit.domain.review.controller;

import com.workit.domain.review.dto.request.ReviewCreateRequestDTO;
import com.workit.domain.review.dto.request.ReviewUpdateRequestDTO;
import com.workit.domain.review.dto.response.MerchantReviewListResponseDTO;
import com.workit.domain.review.dto.response.MyReviewListResponseDTO;
import com.workit.domain.review.dto.response.ReviewCreateResponseDTO;
import com.workit.domain.review.dto.response.ReviewDetailResponseDTO;
import com.workit.domain.review.dto.response.ReviewUpdateResponseDTO;
import com.workit.domain.review.service.ReviewService;
import com.workit.global.dto.CommonResponse;
import com.workit.global.dto.PageResponseDTO;
import com.workit.global.response.GlobalResponseFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    // 숙소·공유 오피스 예약 이용 후 리뷰 등록
    @PostMapping("/reservations/{reservationId}/reviews")
    public ResponseEntity<CommonResponse<ReviewCreateResponseDTO>> reservationReviewAdd(
            @PathVariable Long reservationId,
            @RequestBody ReviewCreateRequestDTO request) {
        Long userId = 1L;
        return GlobalResponseFactory.created(
                reviewService.addReservationReview(userId, reservationId, request)
        );
    }

    // 결제가 완료된 식당·여가 거래 리뷰 등록
    @PostMapping("/transactions/{transactionId}/reviews")
    public ResponseEntity<CommonResponse<ReviewCreateResponseDTO>> transactionReviewAdd(
            @PathVariable Long transactionId,
            @RequestBody ReviewCreateRequestDTO request) {
        Long userId = 1L;
        return GlobalResponseFactory.created(
                reviewService.addTransactionReview(userId, transactionId, request)
        );
    }

    // 작성 기한 내 본인 리뷰의 전달된 항목 수정
    @PatchMapping("/reviews/{reviewId}")
    public ResponseEntity<CommonResponse<ReviewUpdateResponseDTO>> reviewModify(
            @PathVariable Long reviewId,
            @RequestBody ReviewUpdateRequestDTO request) {
        Long userId = 1L;
        return GlobalResponseFactory.success(
                reviewService.modifyReview(userId, reviewId, request)
        );
    }

    // 본인 리뷰를 행 삭제 없이 DELETED 상태로 변경
    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<Void> reviewRemove(@PathVariable Long reviewId) {
        Long userId = 1L;
        reviewService.removeReview(userId, reviewId);
        return GlobalResponseFactory.noContent();
    }
}
