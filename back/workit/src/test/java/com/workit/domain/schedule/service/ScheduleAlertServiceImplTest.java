package com.workit.domain.schedule.service;

import com.workit.domain.notification.dto.request.NotificationCreateRequestDTO;
import com.workit.domain.notification.enums.NotificationCategory;
import com.workit.domain.notification.mapper.NotificationMapper;
import com.workit.domain.notification.service.NotificationCreateService;
import com.workit.domain.notification.vo.NotificationDuplicateKeyVO;
import com.workit.domain.schedule.vo.ScheduleAlertTargetVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleAlertServiceImplTest {

    @Mock
    private NotificationMapper notificationMapper;

    @Mock
    private NotificationCreateService notificationCreateService;

    private ScheduleAlertServiceImpl scheduleAlertService;

    private static final Long TEST_USER_ID = 100L;
    private static final Long TEST_SCHEDULE_ID = 1L;
    private static final String REFERENCE_TYPE = "SCHEDULE";
    private static final String NOTIFICATION_TYPE = "SCHEDULE_D_MINUS_1_HOUR";

    @BeforeEach
    void setUp() {
        scheduleAlertService = new ScheduleAlertServiceImpl(
                notificationMapper, notificationCreateService);
    }

    private ScheduleAlertTargetVO createTarget(Long userId, Long scheduleId,
                                                String title, LocalDateTime scheduledAt) {
        ScheduleAlertTargetVO target = new ScheduleAlertTargetVO();
        target.setUserId(userId);
        target.setScheduleId(scheduleId);
        target.setScheduleTitle(title);
        target.setScheduledAt(scheduledAt);
        return target;
    }

    @Test
    @DisplayName("시작 1시간 전 일정 → 알림 생성")
    void notifyScheduleD1Hour_createsNotification() {
        ScheduleAlertTargetVO target = createTarget(TEST_USER_ID, TEST_SCHEDULE_ID,
                "점심 미팅", LocalDateTime.now().plusMinutes(30));

        // 배치 중복 체크 결과: 중복 없음
        when(notificationMapper.existsNotificationsByReferencesBatch(
                eq(TEST_USER_ID), anyList()))
                .thenReturn(Collections.emptyList());

        scheduleAlertService.notifyScheduleD1Hour(Collections.singletonList(target));

        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());

        NotificationCreateRequestDTO request = captor.getValue();
        assertEquals(NotificationCategory.SCHEDULE_NOTIFY, request.getCategory());
        assertEquals(NOTIFICATION_TYPE, request.getNotificationType());
        assertEquals(REFERENCE_TYPE, request.getReferenceType());
        assertEquals(TEST_SCHEDULE_ID, request.getReferenceId());
        assertFalse(request.getImportant());
        assertEquals("점심 미팅", request.getPlaceholders().get("scheduleTitle"));
    }

    @Test
    @DisplayName("이미 알림이 존재하면 알림 미생성 (배치 중복 체크)")
    void notifyScheduleD1Hour_duplicate_doesNotCreate() {
        ScheduleAlertTargetVO target = createTarget(TEST_USER_ID, TEST_SCHEDULE_ID,
                "점심 미팅", LocalDateTime.now().plusMinutes(30));

        // 배치 중복 체크 결과: 중복 존재
        NotificationDuplicateKeyVO existingKey = new NotificationDuplicateKeyVO(
                REFERENCE_TYPE, TEST_SCHEDULE_ID, NOTIFICATION_TYPE);
        when(notificationMapper.existsNotificationsByReferencesBatch(
                eq(TEST_USER_ID), anyList()))
                .thenReturn(Collections.singletonList(existingKey));

        scheduleAlertService.notifyScheduleD1Hour(Collections.singletonList(target));

        verify(notificationCreateService, never()).createNotification(any(), any());
    }

    @Test
    @DisplayName("빈 목록 → 알림 미생성")
    void notifyScheduleD1Hour_emptyList_doesNotCreate() {
        scheduleAlertService.notifyScheduleD1Hour(Collections.emptyList());
        verify(notificationCreateService, never()).createNotification(any(), any());
    }

    @Test
    @DisplayName("null 목록 → 알림 미생성")
    void notifyScheduleD1Hour_nullList_doesNotCreate() {
        scheduleAlertService.notifyScheduleD1Hour(null);
        verify(notificationCreateService, never()).createNotification(any(), any());
    }

    @Test
    @DisplayName("여러 대상 중 일부만 중복일 때 나머지만 알림 생성")
    void notifyScheduleD1Hour_partialDuplicate_createsOnlyNonDuplicates() {
        ScheduleAlertTargetVO target1 = createTarget(TEST_USER_ID, 1L,
                "미팅 A", LocalDateTime.now().plusMinutes(30));
        ScheduleAlertTargetVO target2 = createTarget(TEST_USER_ID, 2L,
                "미팅 B", LocalDateTime.now().plusMinutes(45));

        // target1은 중복, target2는 신규
        NotificationDuplicateKeyVO existingKey = new NotificationDuplicateKeyVO(
                REFERENCE_TYPE, 1L, NOTIFICATION_TYPE);
        when(notificationMapper.existsNotificationsByReferencesBatch(
                eq(TEST_USER_ID), anyList()))
                .thenReturn(Collections.singletonList(existingKey));

        scheduleAlertService.notifyScheduleD1Hour(Arrays.asList(target1, target2));

        // target2만 알림 생성됨
        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService, times(1)).createNotification(eq(TEST_USER_ID), captor.capture());
        assertEquals(2L, captor.getValue().getReferenceId());
    }
}
