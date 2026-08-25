package com.workit.domain.reservation.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

// 예약 대상 날짜의 잠긴 재고 정보
@Getter
@Setter
public class ReservationDailyInventoryVO {

    private Long id;
    private LocalDate inventoryDate;
    private Integer remainingCapacity;
    private Boolean available;
}
