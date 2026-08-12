package com.workit.domain.review.dto.response;

import com.workit.domain.review.vo.ReviewDetailVO;
import lombok.Builder;
import lombok.Getter;

// 리뷰 상세 화면에 표시할 가맹점 정보 응답 DTO
@Getter
@Builder
public class ReviewMerchantResponseDTO {

    private Long merchantId;
    private String merchantName;
    private String address;
    private String category;
    private String thumbnailUrl;

    public static ReviewMerchantResponseDTO from(ReviewDetailVO review) {
        return ReviewMerchantResponseDTO.builder()
                .merchantId(review.getMerchantId())
                .merchantName(review.getMerchantName())
                .address(review.getMerchantAddress())
                .category(review.getMerchantCategory())
                .thumbnailUrl(review.getMerchantThumbnailUrl())
                .build();
    }
}
