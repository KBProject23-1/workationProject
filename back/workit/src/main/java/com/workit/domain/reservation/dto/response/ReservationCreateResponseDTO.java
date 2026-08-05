package com.workit.domain.reservation.dto.response;

import com.workit.domain.reservation.vo.ReservationCreateProductVO;
import com.workit.domain.reservation.vo.ReservationCreateVO;
import com.workit.domain.reservation.vo.ReservationStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

// 결제가 완료되어 확정된 예약 정보
@Getter
@Builder
public class ReservationCreateResponseDTO {

    private Long reservationId;
    private String reservationCode;
    private ReservationStatus status;
    private String merchantName;
    private Long productId;
    private String productName;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer headcount;
    private Integer quantity;
    private Long totalAmount;

    public static ReservationCreateResponseDTO from(
            ReservationCreateVO reservation,
            ReservationCreateProductVO product) {

        return ReservationCreateResponseDTO.builder()
                .reservationId(reservation.getId())
                .reservationCode(reservation.getReservationCode())
                .status(reservation.getStatus())
                .merchantName(product.getMerchantName())
                .productId(product.getId())
                .productName(product.getProductName())
                .startDate(reservation.getStartDate())
                .endDate(reservation.getEndDate())
                .headcount(reservation.getHeadcount())
                .quantity(reservation.getQuantity())
                .totalAmount(reservation.getTotalAmount().longValueExact())
                .build();
    }
}
