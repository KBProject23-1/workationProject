package com.workit.domain.reservation.scheduler;

import com.workit.domain.reservation.service.ReservationService;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDate;
import java.time.ZoneId;

public class ReservationStatusScheduler {

    private static final ZoneId SEOUL_ZONE_ID = ZoneId.of("Asia/Seoul");

    private final ReservationService reservationService;

    public ReservationStatusScheduler(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    // 매일 자정 기준 이용 완료 예약 상태 변경
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void reservationStatusModify() {
        reservationService.modifyCompletedReservationStatuses(LocalDate.now(SEOUL_ZONE_ID));
    }
}
