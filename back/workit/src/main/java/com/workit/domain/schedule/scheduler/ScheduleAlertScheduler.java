package com.workit.domain.schedule.scheduler;

import com.workit.domain.schedule.mapper.ScheduleAlertMapper;
import com.workit.domain.schedule.service.ScheduleAlertService;
import com.workit.domain.schedule.vo.ScheduleAlertTargetVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

// 일정 알림 스케줄러
// - 매 5분마다 실행되어 시작 1시간 전 일정에 대해 알림을 생성한다
// - 기존 WorkationNotificationScheduler와 동일한 패턴으로 구현
// - Asia/Seoul 시간대 기준으로 동작한다
@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduleAlertScheduler {

    private static final ZoneId SEOUL_ZONE_ID = ZoneId.of("Asia/Seoul");

    private final ScheduleAlertMapper scheduleAlertMapper;
    private final ScheduleAlertService scheduleAlertService;

    // 매 5분마다 실행 (기존 스케줄러들과 다른 주기 사용)
    @Scheduled(cron = "0 */5 * * * *", zone = "Asia/Seoul")
    public void checkAndNotifyScheduleD1Hour() {
        LocalDateTime now = LocalDateTime.now(SEOUL_ZONE_ID);

        log.info("일정 알림 스케줄러 실행 - now={}", now);

        try {
            // 1. 시작 1시간 전 대상 일정 조회
            List<ScheduleAlertTargetVO> targets = scheduleAlertMapper.selectSchedulesStartingInOneHour(now);
            log.info("시작 1시간 전 대상 일정 수: {}", targets.size());

            // 2. 일정 알림 생성
            scheduleAlertService.notifyScheduleD1Hour(targets);

        } catch (Exception e) {
            log.error("일정 알림 스케줄러 실행 실패", e);
        }
    }
}
