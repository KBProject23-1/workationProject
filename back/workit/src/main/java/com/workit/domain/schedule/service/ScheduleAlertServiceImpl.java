package com.workit.domain.schedule.service;

import com.workit.domain.notification.dto.request.NotificationCreateRequestDTO;
import com.workit.domain.notification.enums.NotificationCategory;
import com.workit.domain.notification.service.NotificationCreateService;
import com.workit.domain.schedule.mapper.ScheduleAlertMapper;
import com.workit.domain.schedule.vo.ScheduleAlertTargetVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 일정 알림 판단 서비스 구현체
// - 시작 1시간 전 일정에 대해 알림을 생성한다
// - NotificationCreateService를 통해 알림을 생성하며, 일정 도메인에서 직접 INSERT하지 않는다
// - 중복 알림 방지: notification_histories의 userId + referenceType + referenceId + notificationType을 활용한다
@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduleAlertServiceImpl implements ScheduleAlertService {

    private static final String REFERENCE_TYPE = "SCHEDULE";
    private static final String NOTIFICATION_TYPE = "SCHEDULE_D_MINUS_1_HOUR";

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ScheduleAlertMapper scheduleAlertMapper;
    private final NotificationCreateService notificationCreateService;

    @Override
    public void notifyScheduleD1Hour(List<ScheduleAlertTargetVO> targets) {
        if (targets == null || targets.isEmpty()) {
            return;
        }

        for (ScheduleAlertTargetVO target : targets) {
            createAlertIfNeeded(target);
        }
    }

    /**
     * 알림 생성 여부를 판단하고, 필요시 NotificationCreateService를 통해 알림을 생성한다.
     *
     * @param target 일정 알림 대상 VO
     */
    private void createAlertIfNeeded(ScheduleAlertTargetVO target) {
        Long userId = target.getUserId();
        Long scheduleId = target.getScheduleId();

        // 중복 알림 방지: 동일 일정 + 동일 notificationType에 대해 이미 알림이 있으면 생성하지 않는다
        boolean alreadyExists = scheduleAlertMapper.existsNotificationByReference(
                userId, REFERENCE_TYPE, scheduleId, NOTIFICATION_TYPE);

        if (alreadyExists) {
            log.debug("일정 알림 중복 - scheduleId={}, notificationType={}", scheduleId, NOTIFICATION_TYPE);
            return;
        }

        // placeholder 구성
        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put("scheduleTitle", target.getScheduleTitle());
        placeholders.put("date", target.getScheduledAt().format(DATE_TIME_FORMATTER));

        // 알림 생성 요청 구성
        NotificationCreateRequestDTO request = NotificationCreateRequestDTO.builder()
                .category(NotificationCategory.SCHEDULE_NOTIFY)
                .notificationType(NOTIFICATION_TYPE)
                .important(false)
                .placeholders(placeholders)
                .referenceType(REFERENCE_TYPE)
                .referenceId(scheduleId)
                .build();

        // 공통 알림 생성 서비스 호출
        notificationCreateService.createNotification(userId, request);

        log.info("일정 알림 생성 - userId={}, scheduleId={}, notificationType={}, scheduleTitle={}, date={}",
                userId, scheduleId, NOTIFICATION_TYPE, target.getScheduleTitle(),
                target.getScheduledAt().format(DATE_TIME_FORMATTER));
    }
}
