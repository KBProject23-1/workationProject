package com.workit.domain.review.dto.response;

import com.workit.domain.review.vo.ReviewAtmosphere;
import com.workit.domain.review.vo.ReviewVO;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReviewCreateResponseDTO {

    private Long reviewId;
    private Long reservationId;
    private Long transactionId;
    private Long merchantId;
    private Integer rating;
    private String content;
    private ReviewAtmosphere atmosphere;
    private String imageUrl;

    public static ReviewCreateResponseDTO from(ReviewVO review) {
        return ReviewCreateResponseDTO.builder()
                .reviewId(review.getReviewId())
                .reservationId(review.getReservationId())
                .transactionId(review.getTransactionId())
                .merchantId(review.getMerchantId())
                .rating(review.getRating())
                .content(review.getContent())
                .atmosphere(review.getAtmosphere())
                .imageUrl(review.getImageUrl())
                .build();
    }
}
