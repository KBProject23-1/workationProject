package com.workit.domain.settlement.service;

import com.workit.domain.notification.dto.request.NotificationCreateRequestDTO;
import com.workit.domain.notification.enums.NotificationCategory;
import com.workit.domain.notification.service.NotificationCreateService;
import com.workit.domain.settlement.mapper.SettlementAlertMapper;
import com.workit.domain.workation.vo.WorkationUncheckedCountVO;
import com.workit.domain.workation.vo.WorkationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 정산 알림 판단 서비스 구현체
// - 종료 후 3일 경과 but 정산 미완료 워케이션에 대해 SETTLEMENT_OVERDUE 알림을 생성한다
// - 미확인 지출이 3건 이상인 워케이션에 대해 UNCONFIRMED_EXPENSE_OVER_3 알림을 생성한다
// - NotificationCreateService를 통해 알림을 생성하며, 정산 도메인에서 직접 INSERT하지 않는다
// - 중복 알림 방지: notification_histories의 userId + referenceType + referenceId + notificationType을 활용한다
@Service
@RequiredArgsConstructor
@Slf4j
public class SettlementAlertServiceImpl implements SettlementAlertService {

    private static final String REFERENCE_TYPE = "WORKATION";
    private static final String NOTIFICATION_TYPE_OVERDUE = "SETTLEMENT_OVERDUE";
    private static final String NOTIFICATION_TYPE_UNCONFIRMED = "UNCONFIRMED_EXPENSE_OVER_3";

    private static final int UNCONFIRMED_EXPENSE_THRESHOLD = 3;

    private final SettlementAlertMapper settlementAlertMapper;
    private final NotificationCreateService notificationCreateService;

    @Override
    public void notifySettlementOverdue(List<WorkationVO> workations, Map<Long, Integer> unconfirmedCounts) {
        if (workations == null || workations.isEmpty()) {
            return;
        }

        for (WorkationVO workation : workations) {
            createSettlementOverdueAlertIfNeeded(workation, unconfirmedCounts);
        }
    }

    @Override
    public void notifyUnconfirmedExpenses(List<WorkationUncheckedCountVO> uncheckedCounts) {
        if (uncheckedCounts == null || uncheckedCounts.isEmpty()) {
            return;
        }

        for (WorkationUncheckedCountVO unchecked : uncheckedCounts) {
            if (unchecked.getUncheckedCount() >= UNCONFIRMED_EXPENSE_THRESHOLD) {
                createUnconfirmedExpenseAlertIfNeeded(unchecked);
            }
        }
    }

    /**
     * SETTLEMENT_OVERDUE 알림 생성 여부를 판단하고, 필요시 NotificationCreateService를 통해 알림을 생성한다.
     *
     * @param workation         정산 지연 대상 워케이션 VO
     * @param unconfirmedCounts workationId → 미확인 지출 항목 수 매핑
     */
    private void createSettlementOverdueAlertIfNeeded(WorkationVO workation, Map<Long, Integer> unconfirmedCounts) {
        Long userId = workation.getUserId();
        Long workationId = workation.getId();

        // 중복 알림 방지: 동일 워케이션 + 동일 notificationType에 대해 이미 알림이 있으면 생성하지 않는다
        boolean alreadyExists = settlementAlertMapper.existsNotificationByReference(
                userId, REFERENCE_TYPE, workationId, NOTIFICATION_TYPE_OVERDUE);

        if (alreadyExists) {
            log.debug("정산 지연 알림 중복 - workationId={}, notificationType={}",
                    workationId, NOTIFICATION_TYPE_OVERDUE);
            return;
        }

        // placeholder 구성 - unconfirmedCount는 미확인 지출 항목 수
        int unconfirmedCount = unconfirmedCounts.getOrDefault(workationId, 0);
        if (unconfirmedCount == 0) {
            log.warn("정산 지연 알림 - 미확인 지출 0건. workationId={}", workationId);
        }

        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put("unconfirmedCount", unconfirmedCount);

        // 알림 생성 요청 구성
        NotificationCreateRequestDTO request = NotificationCreateRequestDTO.builder()
                .category(NotificationCategory.SETTLEMENT_NOTIFY)
                .notificationType(NOTIFICATION_TYPE_OVERDUE)
                .important(true)
                .placeholders(placeholders)
                .referenceType(REFERENCE_TYPE)
                .referenceId(workationId)
                .build();

        // 공통 알림 생성 서비스 호출
        notificationCreateService.createNotification(userId, request);

        log.info("정산 지연 알림 생성 - userId={}, workationId={}, unconfirmedCount={}",
                userId, workationId, unconfirmedCount);
    }

    /**
     * UNCONFIRMED_EXPENSE_OVER_3 알림 생성 여부를 판단하고, 필요시 NotificationCreateService를 통해 알림을 생성한다.
     *
     * @param unchecked 미확인 지출 건수 VO (userId, workationId, uncheckedCount)
     */
    private void createUnconfirmedExpenseAlertIfNeeded(WorkationUncheckedCountVO unchecked) {
        Long userId = unchecked.getUserId();
        Long workationId = unchecked.getWorkationId();
        int unconfirmedCount = unchecked.getUncheckedCount();

        // 중복 알림 방지: 동일 워케이션 + 동일 notificationType에 대해 이미 알림이 있으면 생성하지 않는다
        boolean alreadyExists = settlementAlertMapper.existsNotificationByReference(
                userId, REFERENCE_TYPE, workationId, NOTIFICATION_TYPE_UNCONFIRMED);

        if (alreadyExists) {
            log.debug("미확인 지출 알림 중복 - workationId={}, notificationType={}",
                    workationId, NOTIFICATION_TYPE_UNCONFIRMED);
            return;
        }

        // placeholder 구성
        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put("unconfirmedCount", unconfirmedCount);

        // 알림 생성 요청 구성
        NotificationCreateRequestDTO request = NotificationCreateRequestDTO.builder()
                .category(NotificationCategory.SETTLEMENT_NOTIFY)
                .notificationType(NOTIFICATION_TYPE_UNCONFIRMED)
                .important(false)
                .placeholders(placeholders)
                .referenceType(REFERENCE_TYPE)
                .referenceId(workationId)
                .build();

        // 공통 알림 생성 서비스 호출
        notificationCreateService.createNotification(userId, request);

        log.info("미확인 지출 알림 생성 - userId={}, workationId={}, unconfirmedCount={}",
                userId, workationId, unconfirmedCount);
    }
}
