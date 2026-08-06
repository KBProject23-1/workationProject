package com.workit.domain.reservation.dto.response;

import com.workit.domain.reservation.vo.ReservationCancelTargetVO;
import com.workit.domain.reservation.vo.ReservationStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// 예약 취소와 전액 환불 처리 결과
@Getter
@Builder
public class ReservationCancelResponseDTO {

    private Long reservationId;
    private String reservationCode;
    private ReservationStatus status;
    private BigDecimal totalAmount;
    private BigDecimal cancelFee;
    private BigDecimal refundAmount;
    private LocalDateTime canceledAt;

    // 취소 대상과 환불 처리 결과의 응답 변환
    public static ReservationCancelResponseDTO from(
            ReservationCancelTargetVO target,
            BigDecimal cancelFee,
            BigDecimal refundAmount,
            LocalDateTime canceledAt) {

        return ReservationCancelResponseDTO.builder()
                .reservationId(target.getReservationId())
                .reservationCode(target.getReservationCode())
                .status(ReservationStatus.CANCELED)
                .totalAmount(target.getTotalAmount())
                .cancelFee(cancelFee)
                .refundAmount(refundAmount)
                .canceledAt(canceledAt)
                .build();
    }
}
