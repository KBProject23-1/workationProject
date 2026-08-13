package com.workit.domain.reservation.dto.request;

import com.workit.domain.reservation.vo.ReservationPaymentSourceType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

// 예약 생성과 결제에 필요한 사용자 입력값
@Getter
@Setter
public class ReservationCreateRequestDTO {

    private Long productId;
    private Long workationId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer headcount;
    private Integer quantity;
    private ReservationPaymentSourceType paymentSourceType;
    private Long cardId;
    private String pinNumber;
    private String deviceId;
    private String idempotencyKey;
}
