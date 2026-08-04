package com.workit.domain.review.dto.response;

import com.workit.domain.review.vo.MyReviewListItemVO;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// 로그인 사용자가 작성한 리뷰 목록 응답 DTO
@Getter
@Builder
public class MyReviewListResponseDTO {

    private Long reviewId;
    private Long merchantId;
    private String merchantName;
    private String merchantCategory;
    private String merchantThumbnailUrl;
    private Integer rating;
    private String content;
    private String atmosphere;
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long reservationId;
    private Long transactionId;

    public static MyReviewListResponseDTO from(MyReviewListItemVO review) {
        return MyReviewListResponseDTO.builder()
                .reviewId(review.getReviewId())
                .merchantId(review.getMerchantId())
                .merchantName(review.getMerchantName())
                .merchantCategory(review.getMerchantCategory())
                .merchantThumbnailUrl(review.getMerchantThumbnailUrl())
                .rating(review.getRating())
                .content(review.getContent())
                .atmosphere(review.getAtmosphere())
                .imageUrl(review.getImageUrl())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .reservationId(review.getReservationId())
                .transactionId(review.getTransactionId())
                .build();
    }
}
