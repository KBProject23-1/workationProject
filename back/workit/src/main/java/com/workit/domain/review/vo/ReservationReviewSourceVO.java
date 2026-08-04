package com.workit.domain.review.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;

// 예약 기반 리뷰의 소유자·가맹점·작성 기한 검증 정보
@Getter
@Setter
@ToString
public class ReservationReviewSourceVO {

    private Long reservationId;
    private Long userId;
    private Long merchantId;
    private String merchantCategory;
    private String reservationStatus;
    private LocalDate endDate;
    private Long reviewId;
}
