package com.workit.domain.settlement.scheduler;

import com.workit.domain.settlement.service.SettlementAlertService;
import com.workit.domain.workation.mapper.WorkationMapper;
import com.workit.domain.workation.vo.WorkationUncheckedCountVO;
import com.workit.domain.workation.vo.WorkationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 정산 알림 스케줄러
// - 매일 실행되어 정산 지연 대상 및 미확인 지출 대상에게 알림을 생성한다
// - 기존 WorkationNotificationScheduler와 동일한 패턴으로 구현
// - Asia/Seoul 시간대 기준으로 동작한다
@Component
@RequiredArgsConstructor
@Slf4j
public class SettlementAlertScheduler {

    private static final ZoneId SEOUL_ZONE_ID = ZoneId.of("Asia/Seoul");

    /** 정산 지연 기준 일수 (종료 후 3일) */
    private static final int SETTLEMENT_OVERDUE_DAYS = 3;

    private final WorkationMapper workationMapper;
    private final SettlementAlertService settlementAlertService;

    // 매일 오전 10시에 실행 (기존 워케이션 스케줄러 오전 9시와 다른 시간대 사용)
    @Scheduled(cron = "0 0 10 * * *", zone = "Asia/Seoul")
    public void checkAndNotifySettlementAlerts() {
        LocalDate today = LocalDate.now(SEOUL_ZONE_ID);

        log.info("정산 알림 스케줄러 실행 - today={}", today);

        try {
            // 1. 정산 지연 대상 조회 (종료 후 3일 경과, 정산 미완료)
            LocalDate cutoffDate = today.minusDays(SETTLEMENT_OVERDUE_DAYS);
            List<WorkationVO> overdueWorkations = workationMapper.selectUnsettledOverdueWorkations(cutoffDate);
            log.info("정산 지연 워케이션 수: {}", overdueWorkations.size());

            // 2. 각 정산 지연 워케이션의 미확인 지출 항목 수 조회
            Map<Long, Integer> unconfirmedCounts = new HashMap<>();
            for (WorkationVO workation : overdueWorkations) {
                int unconfirmedCount = workationMapper.countUncheckedExpenses(workation.getId());
                unconfirmedCounts.put(workation.getId(), unconfirmedCount);
            }

            // 3. SETTLEMENT_OVERDUE 알림 생성
            settlementAlertService.notifySettlementOverdue(overdueWorkations, unconfirmedCounts);

            // 4. 미확인 지출 대상 조회 (전체 ACTIVE 워케이션 중 미확인 지출 3건 이상)
            List<WorkationUncheckedCountVO> uncheckedCounts =
                    workationMapper.selectActiveWorkationsWithUncheckedExpenses();
            log.info("미확인 지출 3건 이상 워케이션 수: {}", uncheckedCounts.size());

            // 5. UNCONFIRMED_EXPENSE_OVER_3 알림 생성
            settlementAlertService.notifyUnconfirmedExpenses(uncheckedCounts);

        } catch (Exception e) {
            log.error("정산 알림 스케줄러 실행 실패", e);
        }
    }
}
