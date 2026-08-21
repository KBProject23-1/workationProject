package com.workit.domain.workation.service;

import com.workit.domain.notification.dto.request.NotificationCreateRequestDTO;
import com.workit.domain.notification.enums.NotificationCategory;
import com.workit.domain.notification.service.NotificationCreateService;
import com.workit.domain.workation.mapper.WorkationAlertMapper;
import com.workit.domain.workation.vo.WorkationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 워케이션 알림 판단 서비스 구현체
// - 내일 시작/종료하는 워케이션에 대해 D-1 알림을 생성한다
// - NotificationCreateService를 통해 알림을 생성하며, 워케이션 도메인에서 직접 INSERT하지 않는다
// - 중복 알림 방지: notification_histories의 userId + referenceType + referenceId + notificationType을 활용한다
@Service
@RequiredArgsConstructor
@Slf4j
public class WorkationAlertServiceImpl implements WorkationAlertService {

    private static final String REFERENCE_TYPE = "WORKATION";
    private static final String NOTIFICATION_TYPE_START = "WORKATION_START_D_MINUS_1";
    private static final String NOTIFICATION_TYPE_END = "WORKATION_END_D_MINUS_1";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final WorkationAlertMapper workationAlertMapper;
    private final NotificationCreateService notificationCreateService;

    @Override
    public void notifyWorkationStartD1(List<WorkationVO> workations) {
        if (workations == null || workations.isEmpty()) {
            return;
        }

        for (WorkationVO workation : workations) {
            createAlertIfNeeded(workation, NOTIFICATION_TYPE_START, workation.getStartDate());
        }
    }

    @Override
    public void notifyWorkationEndD1(List<WorkationVO> workations) {
        if (workations == null || workations.isEmpty()) {
            return;
        }

        for (WorkationVO workation : workations) {
            createAlertIfNeeded(workation, NOTIFICATION_TYPE_END, workation.getEndDate());
        }
    }

    /**
     * 알림 생성 여부를 판단하고, 필요시 NotificationCreateService를 통해 알림을 생성한다.
     *
     * @param workation        워케이션 VO
     * @param notificationType 알림 타입 (시작/종료)
     * @param date             표시할 날짜 (시작일 또는 종료일)
     */
    private void createAlertIfNeeded(WorkationVO workation, String notificationType, LocalDate date) {
        Long userId = workation.getUserId();
        Long workationId = workation.getId();

        // 중복 알림 방지: 동일 워케이션 + 동일 notificationType에 대해 이미 알림이 있으면 생성하지 않는다
        boolean alreadyExists = workationAlertMapper.existsNotificationByReference(
                userId, REFERENCE_TYPE, workationId, notificationType);

        if (alreadyExists) {
            log.debug("워케이션 알림 중복 - workationId={}, notificationType={}", workationId, notificationType);
            return;
        }

        // placeholder 구성
        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put("date", date.format(DATE_FORMATTER));

        // 알림 생성 요청 구성
        NotificationCreateRequestDTO request = NotificationCreateRequestDTO.builder()
                .category(NotificationCategory.WORKATION_NOTIFY)
                .notificationType(notificationType)
                .important(false)
                .placeholders(placeholders)
                .referenceType(REFERENCE_TYPE)
                .referenceId(workationId)
                .build();

        // 공통 알림 생성 서비스 호출
        notificationCreateService.createNotification(userId, request);

        log.info("워케이션 알림 생성 - userId={}, workationId={}, notificationType={}, date={}",
                userId, workationId, notificationType, date.format(DATE_FORMATTER));
    }
}
