package com.workit.domain.reservation.dto.response;

import com.workit.domain.reservation.vo.ReservationCancellationDetailVO;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 예약 취소 상세 화면에 필요한 정보를 반환하는 응답 DTO
 */
@Getter
@Builder
public class ReservationCancellationDetailResponseDTO {

    private Long reservationId;
    private String reservationCode;
    private String merchantName;
    private ReservationCancellationDetailProductResponseDTO reservationProduct;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalAmount;
    private LocalDateTime canceledAt;
    private BigDecimal cancelFee;
    private BigDecimal refundAmount;

    // 취소 상세 조회 결과를 응답 DTO로 변환
    public static ReservationCancellationDetailResponseDTO from(
            ReservationCancellationDetailVO vo) {

        return ReservationCancellationDetailResponseDTO.builder()
                .reservationId(vo.getReservationId())
                .reservationCode(vo.getReservationCode())
                .merchantName(vo.getMerchantName())
                .reservationProduct(ReservationCancellationDetailProductResponseDTO.from(vo))
                .startDate(vo.getStartDate())
                .endDate(vo.getEndDate())
                .totalAmount(vo.getTotalAmount())
                .canceledAt(vo.getCanceledAt())
                .cancelFee(vo.getCancelFee())
                .refundAmount(vo.getRefundAmount())
                .build();
    }
}
