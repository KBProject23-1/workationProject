package com.workit.domain.reservation.vo;

/**
 * 예약 상세 화면에서 노출할 리뷰 관련 버튼 상태
 */
public enum ReservationReviewAction {
    // 리뷰 작성 버튼을 노출한다.
    WRITE,
    // 리뷰 수정·삭제 버튼을 노출한다.
    EDIT,
    // 리뷰 관련 버튼을 노출하지 않는다.
    NONE
}
