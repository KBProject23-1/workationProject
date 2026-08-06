package com.workit.domain.reservation.vo;

import lombok.Getter;
import lombok.Setter;

// 예약 취소 시 복구할 날짜별 재고 정보
@Getter
@Setter
public class ReservationCancelInventoryVO {

    private Long dailyInventoryId;
    private Integer reservedCount;
}
