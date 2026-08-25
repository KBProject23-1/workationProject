package com.workit.domain.reservation.vo;

/**
 * 예약 진행 상태
 */
public enum ReservationStatus {
    // 결제가 완료되어 예약이 확정된 상태
    CONFIRMED,
    // 예약 이용이 완료된 상태
    COMPLETED,
    // 예약이 취소된 상태
    CANCELED
}
