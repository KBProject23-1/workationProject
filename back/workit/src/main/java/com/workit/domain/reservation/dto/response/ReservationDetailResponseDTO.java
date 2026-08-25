package com.workit.domain.reservation.dto.response;

import com.workit.domain.reservation.vo.ReservationDetailVO;
import com.workit.domain.reservation.vo.ReservationReviewAction;
import com.workit.domain.reservation.vo.ReservationStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 예약 확정·이용 완료 상세 화면에 필요한 정보를 반환하는 응답 DTO
 */
@Getter
@Builder
public class ReservationDetailResponseDTO {

    private Long reservationId;
    private String reservationCode;
    private Long workationId;
    private ReservationDetailMerchantResponseDTO merchant;
    private ReservationDetailProductResponseDTO reservationProduct;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer headcount;
    private Integer quantity;
    private BigDecimal totalAmount;
    private String paymentMethod;
    private ReservationStatus status;
    private Boolean cancelable;
    private LocalDateTime createdAt;
    private Long reviewId;
    private ReservationReviewAction reviewAction;
    private LocalDateTime reviewDeadline;


//    조회 결과와 서비스에서 계산한 취소·리뷰 상태를 상세 응답으로 변환
    public static ReservationDetailResponseDTO from(
            ReservationDetailVO vo,
            boolean cancelable,
            Long reviewId,
            ReservationReviewAction reviewAction,
            LocalDateTime reviewDeadline) {

        return ReservationDetailResponseDTO.builder()
                .reservationId(vo.getReservationId())
                .reservationCode(vo.getReservationCode())
                .workationId(vo.getWorkationId())
                .merchant(ReservationDetailMerchantResponseDTO.from(vo))
                .reservationProduct(ReservationDetailProductResponseDTO.from(vo))
                .startDate(vo.getStartDate())
                .endDate(vo.getEndDate())
                .headcount(vo.getHeadcount())
                .quantity(vo.getQuantity())
                .totalAmount(vo.getTotalAmount())
                .paymentMethod(vo.getPaymentMethod())
                .status(vo.getStatus())
                .cancelable(cancelable)
                .createdAt(vo.getCreatedAt())
                .reviewId(reviewId)
                .reviewAction(reviewAction)
                .reviewDeadline(reviewDeadline)
                .build();
    }
}
