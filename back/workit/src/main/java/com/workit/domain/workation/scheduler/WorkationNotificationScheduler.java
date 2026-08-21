package com.workit.domain.workation.scheduler;

import com.workit.domain.workation.mapper.WorkationMapper;
import com.workit.domain.workation.service.WorkationAlertService;
import com.workit.domain.workation.vo.WorkationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

// 워케이션 D-1 알림 스케줄러
// - 매일 실행되어 내일 시작/종료하는 워케이션에 대해 D-1 알림을 생성한다
// - 기존 ReservationStatusScheduler와 동일한 패턴으로 구현
// - Asia/Seoul 시간대 기준으로 동작한다
@Component
@RequiredArgsConstructor
@Slf4j
public class WorkationNotificationScheduler {

    private static final ZoneId SEOUL_ZONE_ID = ZoneId.of("Asia/Seoul");

    private final WorkationMapper workationMapper;
    private final WorkationAlertService workationAlertService;

    // 매일 오전 9시에 실행 (기존 스케줄러们과 다른 시간대 사용)
    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    public void checkAndNotifyWorkationD1() {
        LocalDate tomorrow = LocalDate.now(SEOUL_ZONE_ID).plusDays(1);

        log.info("워케이션 D-1 알림 스케줄러 실행 - tomorrow={}", tomorrow);

        try {
            // 1. 내일 시작하는 워케이션 조회
            List<WorkationVO> startWorkations = workationMapper.selectWorkationsByStartDate(tomorrow);
            log.info("내일 시작하는 워케이션 수: {}", startWorkations.size());

            // 2. 시작 D-1 알림 생성
            workationAlertService.notifyWorkationStartD1(startWorkations);

            // 3. 내일 종료하는 워케이션 조회
            List<WorkationVO> endWorkations = workationMapper.selectWorkationsByEndDate(tomorrow);
            log.info("내일 종료하는 워케이션 수: {}", endWorkations.size());

            // 4. 종료 D-1 알림 생성
            workationAlertService.notifyWorkationEndD1(endWorkations);

        } catch (Exception e) {
            log.error("워케이션 D-1 알림 스케줄러 실행 실패", e);
        }
    }
}
