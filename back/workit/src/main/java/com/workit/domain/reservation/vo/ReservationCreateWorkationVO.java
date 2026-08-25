package com.workit.domain.reservation.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

// 예약 생성 시 잠근 워케이션 정보
@Getter
@Setter
public class ReservationCreateWorkationVO {

    private Long id;
    private Long userId;
    private Long regionId;
    private LocalDate startDate;
    private LocalDate endDate;
}
