package com.workit.domain.review.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

// 가맹점 리뷰 목록 조회 결과
@Getter
@Setter
@ToString
public class MerchantReviewVO {

    private Long reviewId;
    private Long userId;
    private String nickname;
    private Integer rating;
    private String content;
    private String atmosphere;
    private LocalDateTime createdAt;
    private String imageUrl;
    private Boolean isMine;
}
