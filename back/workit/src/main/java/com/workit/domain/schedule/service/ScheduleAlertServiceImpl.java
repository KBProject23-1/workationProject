package com.workit.domain.schedule.service;

import com.workit.domain.notification.dto.request.NotificationCreateRequestDTO;
import com.workit.domain.notification.enums.NotificationCategory;
import com.workit.domain.notification.mapper.NotificationMapper;
import com.workit.domain.notification.service.NotificationCreateService;
import com.workit.domain.notification.vo.NotificationDuplicateKeyVO;
import com.workit.domain.schedule.vo.ScheduleAlertTargetVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// 일정 알림 판단 서비스 구현체
// - 시작 1시간 전 일정에 대해 알림을 생성한다
// - NotificationCreateService를 통해 알림을 생성하며, 일정 도메인에서 직접 INSERT하지 않는다
// - 중복 알림 방지는 NotificationCreateServiceImpl.createNotification() 내부에서 원자적으로 처리한다
@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduleAlertServiceImpl implements ScheduleAlertService {

    private static final String REFERENCE_TYPE = "SCHEDULE";
    private static final String NOTIFICATION_TYPE = "SCHEDULE_D_MINUS_1_HOUR";

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final NotificationMapper notificationMapper;
    private final NotificationCreateService notificationCreateService;

    @Override
    public void notifyScheduleD1Hour(List<ScheduleAlertTargetVO> targets) {
        if (targets == null || targets.isEmpty()) {
            return;
        }

        // 배치 중복 체크: 여러 대상에 대해 한 번에 기존 알림 존재 여부를 확인한다.
        // N+1 쿼리 방지: 대상 수만큼 DB 쿼리를 보내지 않고 IN 절로 한 번에 조회한다.
        Set<NotificationDuplicateKeyVO> existingKeys = findExistingNotifications(targets);

        for (ScheduleAlertTargetVO target : targets) {
            NotificationDuplicateKeyVO key = new NotificationDuplicateKeyVO(
                    REFERENCE_TYPE, target.getScheduleId(), NOTIFICATION_TYPE);
            if (existingKeys.contains(key)) {
                log.debug("일정 알림 중복 - scheduleId={}, notificationType={}",
                        target.getScheduleId(), NOTIFICATION_TYPE);
                continue;
            }

            createAndSendAlert(target);
        }
    }

    /**
     * 배치로 기존 알림 존재 여부를 확인한다.
     * - NotificationMapper.existsNotificationsByReferencesBatch를 사용하여 한 번의 DB 쿼리로 조회한다.
     * - 결과를 Set으로 변환하여 O(1) lookup이 가능하도록 한다.
     *
     * @param targets 알림 대상 목록
     * @return 이미 알림이 존재하는 (referenceType, referenceId, notificationType) 키 집합
     */
    private Set<NotificationDuplicateKeyVO> findExistingNotifications(List<ScheduleAlertTargetVO> targets) {
        List<NotificationDuplicateKeyVO> keys = new ArrayList<>();
        for (ScheduleAlertTargetVO target : targets) {
            keys.add(new NotificationDuplicateKeyVO(REFERENCE_TYPE, target.getScheduleId(), NOTIFICATION_TYPE));
        }

        // 각 대상의 userId가 다를 수 있으므로, 첫 번째 대상의 userId를 기준으로 배치 조회.
        // 스케줄러에서는 동일 사용자의 대상이 묶여서 오므로, userId별로 그룹핑하는 대신
        // NotificationCreateServiceImpl의 중복 체크가 트랜잭션 내에서 처리되므로
        //此处的 배치 체크는 1차 필터링용이다.
        Long userId = targets.get(0).getUserId();
        List<NotificationDuplicateKeyVO> existing =
                notificationMapper.existsNotificationsByReferencesBatch(userId, keys);

        return new HashSet<>(existing);
    }

    /**
     * 일정 알림을 생성하고 NotificationCreateService를 통해 전달한다.
     *
     * @param target 일정 알림 대상 VO
     */
    private void createAndSendAlert(ScheduleAlertTargetVO target) {
        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put("scheduleTitle", target.getScheduleTitle());
        placeholders.put("date", target.getScheduledAt().format(DATE_TIME_FORMATTER));

        NotificationCreateRequestDTO request = NotificationCreateRequestDTO.builder()
                .category(NotificationCategory.SCHEDULE_NOTIFY)
                .notificationType(NOTIFICATION_TYPE)
                .important(false)
                .placeholders(placeholders)
                .referenceType(REFERENCE_TYPE)
                .referenceId(target.getScheduleId())
                .build();

        notificationCreateService.createNotification(target.getUserId(), request);

        log.info("일정 알림 생성 - userId={}, scheduleId={}, notificationType={}, scheduleTitle={}, date={}",
                target.getUserId(), target.getScheduleId(), NOTIFICATION_TYPE,
                target.getScheduleTitle(), target.getScheduledAt().format(DATE_TIME_FORMATTER));
    }
}
