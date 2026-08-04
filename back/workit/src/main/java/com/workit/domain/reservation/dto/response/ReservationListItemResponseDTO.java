package com.workit.domain.reservation.dto.response;

import com.workit.domain.reservation.vo.ReservationListItemVO;
import com.workit.domain.reservation.vo.ReservationProductDetailType;
import com.workit.domain.reservation.vo.ReservationStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

//예약 목록의 예약 확정·이용 완료·취소 항목을 표현하는 응답 DTO
@Getter
@Builder
public class ReservationListItemResponseDTO {

    private Long reservationId;
    private String reservationCode;
    private String merchantName;
    private String productName;
    private ReservationProductDetailType productDetailType;
    private String thumbnailUrl;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer headcount;
    private Integer quantity;
    private BigDecimal totalAmount;
    private ReservationStatus status;
    private LocalDateTime canceledAt;
    private BigDecimal refundAmount;

    /**
     * 목록 조회용 VO를 API 응답으로 변환
     * 취소 일시, 환불금액은 취소된 예약에만 노출하고 나머지 상태에서는 null로 반환
     */
    public static ReservationListItemResponseDTO from(ReservationListItemVO vo) {
        return ReservationListItemResponseDTO.builder()
                .reservationId(vo.getReservationId())
                .reservationCode(vo.getReservationCode())
                .merchantName(vo.getMerchantName())
                .productName(vo.getProductName())
                .productDetailType(vo.getProductDetailType())
                .thumbnailUrl(vo.getThumbnailUrl())
                .startDate(vo.getStartDate())
                .endDate(vo.getEndDate())
                .headcount(vo.getHeadcount())
                .quantity(vo.getQuantity())
                .totalAmount(vo.getTotalAmount())
                .status(vo.getStatus())
                .canceledAt(vo.getStatus() == ReservationStatus.CANCELED ? vo.getCanceledAt() : null)
                .refundAmount(vo.getStatus() == ReservationStatus.CANCELED ? vo.getRefundAmount() : null)
                .build();
    }
}
