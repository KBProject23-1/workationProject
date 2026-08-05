package com.workit.domain.review.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;

// 리뷰 수정·삭제 시 소유권과 원본 이용 기한을 검증하는 조회 결과
@Getter
@Setter
@ToString
public class OwnedReviewVO {

    private Long reviewId;
    private Long userId;
    private Long reservationId;
    private Long transactionId;
    private String status;
    private String merchantCategory;
    private LocalDate reservationEndDate;
    private LocalDateTime transactionApprovedAt;
}
