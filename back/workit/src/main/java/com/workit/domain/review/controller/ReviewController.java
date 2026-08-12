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
import com.workit.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;

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
            @CurrentUser Long userId,
            @PathVariable Long reviewId) {
        return GlobalResponseFactory.success(reviewService.findReviewDetails(userId, reviewId));
    }

    // 로그인 사용자가 작성한 활성 리뷰를 가맹점 카테고리별로 조회
    @GetMapping("/users/me/reviews")
    public ResponseEntity<CommonResponse<PageResponseDTO<MyReviewListResponseDTO>>> myReviewList(
            @CurrentUser Long userId,
            @RequestParam(value = "category", defaultValue = "ALL") String category,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        return GlobalResponseFactory.success(
                reviewService.findMyReviewList(userId, category, page, size)
        );
    }

    // 숙소·공유 오피스 예약 이용 후 리뷰 등록
    @PostMapping(value = "/reservations/{reservationId}/reviews", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<ReviewCreateResponseDTO>> reservationReviewAdd(
            @CurrentUser Long userId,
            @PathVariable Long reservationId,
            @ModelAttribute ReviewCreateRequestDTO request) {
        return GlobalResponseFactory.created(
                reviewService.addReservationReview(userId, reservationId, request)
        );
    }

    // 결제가 완료된 식당·여가 거래 리뷰 등록
    @PostMapping(value = "/transactions/{transactionId}/reviews", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<ReviewCreateResponseDTO>> transactionReviewAdd(
            @CurrentUser Long userId,
            @PathVariable Long transactionId,
            @ModelAttribute ReviewCreateRequestDTO request) {
        return GlobalResponseFactory.created(
                reviewService.addTransactionReview(userId, transactionId, request)
        );
    }

    // 작성 기한 내 본인 리뷰의 전달된 항목 수정
    @PatchMapping(value = "/reviews/{reviewId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CommonResponse<ReviewUpdateResponseDTO>> reviewModify(
            @CurrentUser Long userId,
            @PathVariable Long reviewId,
            @RequestBody ReviewUpdateRequestDTO request) {
        return GlobalResponseFactory.success(
                reviewService.modifyReview(userId, reviewId, request)
        );
    }

    // 새 이미지를 포함한 본인 리뷰 수정
    @PatchMapping(value = "/reviews/{reviewId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<ReviewUpdateResponseDTO>> reviewImageModify(
            @CurrentUser Long userId,
            @PathVariable Long reviewId,
            @ModelAttribute ReviewUpdateRequestDTO request) {
        return GlobalResponseFactory.success(
                reviewService.modifyReview(userId, reviewId, request)
        );
    }

    // 본인 리뷰를 행 삭제 없이 DELETED 상태로 변경
    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<Void> reviewRemove(
            @CurrentUser Long userId,
            @PathVariable Long reviewId) {
        reviewService.removeReview(userId, reviewId);
        return GlobalResponseFactory.noContent();
    }
}
