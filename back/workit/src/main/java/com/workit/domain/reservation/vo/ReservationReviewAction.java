package com.workit.domain.reservation.vo;

/**
 * 예약 상세 화면에서 노출할 리뷰 관련 버튼 상태
 */
public enum ReservationReviewAction {
    // 리뷰 작성 버튼을 노출
    WRITE,
    // 리뷰 수정·삭제 버튼을 노출
    EDIT,
    // 수정 기한이 지난 리뷰의 삭제 버튼만 노출
    DELETE,
    // 리뷰 관련 버튼을 노출하지 않음
    NONE
}
