package com.workit.domain.review.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReviewUpdateResponseDTO {

    private Long reviewId;
    private String status;

    public static ReviewUpdateResponseDTO of(Long reviewId) {
        return ReviewUpdateResponseDTO.builder()
                .reviewId(reviewId)
                .status("ACTIVE")
                .build();
    }
}
