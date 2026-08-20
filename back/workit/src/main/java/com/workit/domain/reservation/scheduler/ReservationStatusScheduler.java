package com.workit.domain.reservation.scheduler;

import com.workit.domain.reservation.service.ReservationService;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDate;
import java.time.ZoneId;

public class ReservationStatusScheduler {

    private static final ZoneId SEOUL_ZONE_ID = ZoneId.of("Asia/Seoul");

    private final ReservationService reservationService;

    public ReservationStatusScheduler(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    // 서버 시작 시 자정 스케줄 누락 예약 상태 보정
    @EventListener
    public void reservationStatusInitialize(ContextRefreshedEvent event) {
        if (event.getApplicationContext().getParent() != null) {
            return;
        }

        modifyCompletedReservationStatuses();
    }

    // 매일 자정 기준 이용 완료 예약 상태 변경
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void reservationStatusModify() {
        modifyCompletedReservationStatuses();
    }

    // 서울 날짜 기준 이용 완료 예약 상태 변경
    private void modifyCompletedReservationStatuses() {
        reservationService.modifyCompletedReservationStatuses(LocalDate.now(SEOUL_ZONE_ID));
    }
}
