package com.workit.domain.reservation.dto.response;

import com.workit.domain.reservation.vo.ReservationDailyInventoryVO;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

// 예약 상품의 날짜별 남은 재고와 예약 가능 여부
@Getter
@Builder
public class ReservationProductAvailabilityResponseDTO {

    private LocalDate date;
    private Integer remainingCapacity;
    private boolean available;

    public static ReservationProductAvailabilityResponseDTO from(
            LocalDate date,
            ReservationDailyInventoryVO inventory) {

        int remainingCapacity = inventory == null || inventory.getRemainingCapacity() == null
                ? 0
                : inventory.getRemainingCapacity();
        boolean available = inventory != null
                && Boolean.TRUE.equals(inventory.getAvailable())
                && remainingCapacity > 0;

        return ReservationProductAvailabilityResponseDTO.builder()
                .date(date)
                .remainingCapacity(remainingCapacity)
                .available(available)
                .build();
    }
}
