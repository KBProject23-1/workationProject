package com.workit.domain.review.dto.response;

import com.workit.domain.review.vo.ReviewDetailVO;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

// 리뷰와 작성 근거를 제공하는 상세 응답 DTO
@Getter
@Builder
public class ReviewDetailResponseDTO {

    private Long reviewId;
    private Boolean isMine;
    private String nickname;
    private Integer rating;
    private String content;
    private String atmosphere;
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private ReviewMerchantResponseDTO merchant;
    private Long reservationId;
    private String reservationCode;
    private LocalDate reservationStartDate;
    private LocalDate reservationEndDate;
    private Long transactionId;
    private String transactionNumber;
    private BigDecimal transactionAmount;
    private LocalDateTime transactionApprovedAt;

    public static ReviewDetailResponseDTO from(ReviewDetailVO review, Long currentUserId) {
        return ReviewDetailResponseDTO.builder()
                .reviewId(review.getReviewId())
                .isMine(currentUserId != null && currentUserId.equals(review.getUserId()))
                .nickname(review.getNickname())
                .rating(review.getRating())
                .content(review.getContent())
                .atmosphere(review.getAtmosphere())
                .imageUrl(review.getImageUrl())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .merchant(ReviewMerchantResponseDTO.from(review))
                .reservationId(review.getReservationId())
                .reservationCode(review.getReservationCode())
                .reservationStartDate(review.getReservationStartDate())
                .reservationEndDate(review.getReservationEndDate())
                .transactionId(review.getTransactionId())
                .transactionNumber(review.getTransactionNumber())
                .transactionAmount(review.getTransactionAmount())
                .transactionApprovedAt(review.getTransactionApprovedAt())
                .build();
    }
}
