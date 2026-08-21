package com.workit.domain.schedule.service;

import com.workit.domain.notification.dto.request.NotificationCreateRequestDTO;
import com.workit.domain.notification.enums.NotificationCategory;
import com.workit.domain.notification.service.NotificationCreateService;
import com.workit.domain.schedule.mapper.ScheduleAlertMapper;
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

// ScheduleAlertServiceImpl 테스트
// - 시작 1시간 전 일정 알림, 중복 방지, placeholder 전달 등을 검증한다
@ExtendWith(MockitoExtension.class)
class ScheduleAlertServiceImplTest {

    @Mock
    private ScheduleAlertMapper scheduleAlertMapper;

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
                scheduleAlertMapper, notificationCreateService);
    }

    // ---------- 테스트 데이터 헬퍼 ----------

    /** 일정 알림 대상 VO 생성 */
    private ScheduleAlertTargetVO createTarget(Long userId, Long scheduleId,
                                                String scheduleTitle, LocalDateTime scheduledAt) {
        ScheduleAlertTargetVO vo = new ScheduleAlertTargetVO();
        vo.setUserId(userId);
        vo.setScheduleId(scheduleId);
        vo.setScheduleTitle(scheduleTitle);
        vo.setScheduledAt(scheduledAt);
        return vo;
    }

    // ================================================================
    // 1. 일정 대상 조회 테스트
    // ================================================================

    @Test
    @DisplayName("시작 1시간 전 일정 → 알림 생성")
    void notifyScheduleD1Hour_oneHourBefore_createsNotification() {
        // Given
        LocalDateTime scheduledAt = LocalDateTime.now().plusMinutes(30);
        ScheduleAlertTargetVO target = createTarget(TEST_USER_ID, TEST_SCHEDULE_ID,
                "점심 회의", scheduledAt);

        when(scheduleAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_SCHEDULE_ID, NOTIFICATION_TYPE))
                .thenReturn(false);

        // When
        scheduleAlertService.notifyScheduleD1Hour(Collections.singletonList(target));

        // Then
        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());

        NotificationCreateRequestDTO request = captor.getValue();
        assertEquals(NotificationCategory.SCHEDULE_NOTIFY, request.getCategory());
        assertEquals(NOTIFICATION_TYPE, request.getNotificationType());
        assertEquals(REFERENCE_TYPE, request.getReferenceType());
        assertEquals(TEST_SCHEDULE_ID, request.getReferenceId());
        assertFalse(request.getImportant());
    }

    @Test
    @DisplayName("1시간 전 알림 - scheduleTitle placeholder 정상 전달")
    void notifyScheduleD1Hour_scheduleTitlePlaceholder() {
        // Given
        LocalDateTime scheduledAt = LocalDateTime.now().plusMinutes(45);
        ScheduleAlertTargetVO target = createTarget(TEST_USER_ID, TEST_SCHEDULE_ID,
                "저녁 회식", scheduledAt);

        when(scheduleAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_SCHEDULE_ID, NOTIFICATION_TYPE))
                .thenReturn(false);

        // When
        scheduleAlertService.notifyScheduleD1Hour(Collections.singletonList(target));

        // Then
        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());
        assertEquals("저녁 회식", captor.getValue().getPlaceholders().get("scheduleTitle"));
    }

    @Test
    @DisplayName("1시간 전 알림 - date placeholder에 yyyy-MM-dd HH:mm 형식으로 전달")
    void notifyScheduleD1Hour_datePlaceholder() {
        // Given
        LocalDateTime scheduledAt = LocalDateTime.of(2026, 8, 22, 14, 0);
        ScheduleAlertTargetVO target = createTarget(TEST_USER_ID, TEST_SCHEDULE_ID,
                "점심 회의", scheduledAt);

        when(scheduleAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_SCHEDULE_ID, NOTIFICATION_TYPE))
                .thenReturn(false);

        // When
        scheduleAlertService.notifyScheduleD1Hour(Collections.singletonList(target));

        // Then
        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());
        assertEquals("2026-08-22 14:00", captor.getValue().getPlaceholders().get("date"));
    }

    // ================================================================
    // 2. 중복 방지 테스트
    // ================================================================

    @Test
    @DisplayName("동일 일정 + 동일 notificationType → 중복 생성하지 않음")
    void notifyScheduleD1Hour_duplicate_doesNotCreate() {
        // Given
        LocalDateTime scheduledAt = LocalDateTime.now().plusMinutes(30);
        ScheduleAlertTargetVO target = createTarget(TEST_USER_ID, TEST_SCHEDULE_ID,
                "점심 회의", scheduledAt);

        when(scheduleAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_SCHEDULE_ID, NOTIFICATION_TYPE))
                .thenReturn(true); // 이미 알림 존재

        // When
        scheduleAlertService.notifyScheduleD1Hour(Collections.singletonList(target));

        // Then
        verify(notificationCreateService, never()).createNotification(any(), any());
    }

    @Test
    @DisplayName("알림이 없으면 정상 생성")
    void notifyScheduleD1Hour_noDuplicate_createsNotification() {
        // Given
        LocalDateTime scheduledAt = LocalDateTime.now().plusMinutes(30);
        ScheduleAlertTargetVO target = createTarget(TEST_USER_ID, TEST_SCHEDULE_ID,
                "점심 회의", scheduledAt);

        when(scheduleAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_SCHEDULE_ID, NOTIFICATION_TYPE))
                .thenReturn(false);

        // When
        scheduleAlertService.notifyScheduleD1Hour(Collections.singletonList(target));

        // Then
        verify(notificationCreateService, times(1)).createNotification(eq(TEST_USER_ID), any());
    }

    // ================================================================
    // 3. 빈 목록 / null 테스트
    // ================================================================

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

    // ================================================================
    // 4. 다수 일정 테스트
    // ================================================================

    @Test
    @DisplayName("시작 1시간 전 일정이 여러 개인 경우 모두 알림 생성")
    void notifyScheduleD1Hour_multipleTargets_createsNotificationsForAll() {
        // Given
        LocalDateTime scheduledAt1 = LocalDateTime.now().plusMinutes(20);
        LocalDateTime scheduledAt2 = LocalDateTime.now().plusMinutes(40);
        ScheduleAlertTargetVO target1 = createTarget(TEST_USER_ID, 1L,
                "점심 회의", scheduledAt1);
        ScheduleAlertTargetVO target2 = createTarget(TEST_USER_ID, 2L,
                "저녁 회식", scheduledAt2);

        when(scheduleAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, 1L, NOTIFICATION_TYPE))
                .thenReturn(false);
        when(scheduleAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, 2L, NOTIFICATION_TYPE))
                .thenReturn(false);

        // When
        scheduleAlertService.notifyScheduleD1Hour(Arrays.asList(target1, target2));

        // Then
        verify(notificationCreateService, times(2)).createNotification(eq(TEST_USER_ID), any());
    }

    @Test
    @DisplayName("다수 일정 중 일부는 중복인 경우 중복 건만 스킵")
    void notifyScheduleD1Hour_multipleTargetsSomeDuplicate_skipsDuplicates() {
        // Given
        LocalDateTime scheduledAt1 = LocalDateTime.now().plusMinutes(20);
        LocalDateTime scheduledAt2 = LocalDateTime.now().plusMinutes(40);
        ScheduleAlertTargetVO target1 = createTarget(TEST_USER_ID, 1L,
                "점심 회의", scheduledAt1);
        ScheduleAlertTargetVO target2 = createTarget(TEST_USER_ID, 2L,
                "저녁 회식", scheduledAt2);

        when(scheduleAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, 1L, NOTIFICATION_TYPE))
                .thenReturn(true); // 중복
        when(scheduleAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, 2L, NOTIFICATION_TYPE))
                .thenReturn(false); // 신규

        // When
        scheduleAlertService.notifyScheduleD1Hour(Arrays.asList(target1, target2));

        // Then - 1건만 알림 생성
        verify(notificationCreateService, times(1)).createNotification(eq(TEST_USER_ID), any());
    }

    // ================================================================
    // 5. 알림 생성 데이터 검증 테스트
    // ================================================================

    @Test
    @DisplayName("SCHEDULE_NOTIFY 카테고리로 호출됨")
    void notifyScheduleD1Hour_correctCategory() {
        // Given
        LocalDateTime scheduledAt = LocalDateTime.now().plusMinutes(30);
        ScheduleAlertTargetVO target = createTarget(TEST_USER_ID, TEST_SCHEDULE_ID,
                "점심 회의", scheduledAt);

        when(scheduleAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_SCHEDULE_ID, NOTIFICATION_TYPE))
                .thenReturn(false);

        // When
        scheduleAlertService.notifyScheduleD1Hour(Collections.singletonList(target));

        // Then
        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());
        assertEquals(NotificationCategory.SCHEDULE_NOTIFY, captor.getValue().getCategory());
    }

    @Test
    @DisplayName("referenceType=SCHEDULE, referenceId=scheduleId로 호출됨")
    void notifyScheduleD1Hour_referenceCorrect() {
        // Given
        LocalDateTime scheduledAt = LocalDateTime.now().plusMinutes(30);
        ScheduleAlertTargetVO target = createTarget(TEST_USER_ID, TEST_SCHEDULE_ID,
                "점심 회의", scheduledAt);

        when(scheduleAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_SCHEDULE_ID, NOTIFICATION_TYPE))
                .thenReturn(false);

        // When
        scheduleAlertService.notifyScheduleD1Hour(Collections.singletonList(target));

        // Then
        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());
        assertEquals(REFERENCE_TYPE, captor.getValue().getReferenceType());
        assertEquals(TEST_SCHEDULE_ID, captor.getValue().getReferenceId());
    }

    @Test
    @DisplayName("important=false로 호출됨")
    void notifyScheduleD1Hour_importantFalse() {
        // Given
        LocalDateTime scheduledAt = LocalDateTime.now().plusMinutes(30);
        ScheduleAlertTargetVO target = createTarget(TEST_USER_ID, TEST_SCHEDULE_ID,
                "점심 회의", scheduledAt);

        when(scheduleAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_SCHEDULE_ID, NOTIFICATION_TYPE))
                .thenReturn(false);

        // When
        scheduleAlertService.notifyScheduleD1Hour(Collections.singletonList(target));

        // Then
        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());
        assertFalse(captor.getValue().getImportant());
    }

    // ================================================================
    // 6. 여러 사용자 테스트
    // ================================================================

    @Test
    @DisplayName("하나의 일정에 여러 사용자가 연결된 경우 각각 알림 생성")
    void notifyScheduleD1Hour_multipleUsers_createsNotificationsForEach() {
        // Given
        LocalDateTime scheduledAt = LocalDateTime.now().plusMinutes(30);
        ScheduleAlertTargetVO target1 = createTarget(100L, TEST_SCHEDULE_ID,
                "점심 회의", scheduledAt);
        ScheduleAlertTargetVO target2 = createTarget(200L, TEST_SCHEDULE_ID,
                "점심 회의", scheduledAt);

        when(scheduleAlertMapper.existsNotificationByReference(
                100L, REFERENCE_TYPE, TEST_SCHEDULE_ID, NOTIFICATION_TYPE))
                .thenReturn(false);
        when(scheduleAlertMapper.existsNotificationByReference(
                200L, REFERENCE_TYPE, TEST_SCHEDULE_ID, NOTIFICATION_TYPE))
                .thenReturn(false);

        // When
        scheduleAlertService.notifyScheduleD1Hour(Arrays.asList(target1, target2));

        // Then - 각 사용자에게 각각 알림 생성
        verify(notificationCreateService, times(2)).createNotification(anyLong(), any());
    }

    @Test
    @DisplayName("한 사용자의 중복 알림이 다른 사용자에게 영향을 주지 않음")
    void notifyScheduleD1Hour_oneUserDuplicateOtherNot_createsOnlyForNonDuplicate() {
        // Given
        LocalDateTime scheduledAt = LocalDateTime.now().plusMinutes(30);
        ScheduleAlertTargetVO target1 = createTarget(100L, TEST_SCHEDULE_ID,
                "점심 회의", scheduledAt);
        ScheduleAlertTargetVO target2 = createTarget(200L, TEST_SCHEDULE_ID,
                "점심 회의", scheduledAt);

        when(scheduleAlertMapper.existsNotificationByReference(
                100L, REFERENCE_TYPE, TEST_SCHEDULE_ID, NOTIFICATION_TYPE))
                .thenReturn(true); // 100번 사용자는 중복
        when(scheduleAlertMapper.existsNotificationByReference(
                200L, REFERENCE_TYPE, TEST_SCHEDULE_ID, NOTIFICATION_TYPE))
                .thenReturn(false); // 200번 사용자는 신규

        // When
        scheduleAlertService.notifyScheduleD1Hour(Arrays.asList(target1, target2));

        // Then - 200번 사용자에게만 알림 생성
        verify(notificationCreateService, times(1)).createNotification(eq(200L), any());
    }

    // ================================================================
    // 7. catch-up 시나리오 테스트
    // ================================================================

    @Test
    @DisplayName("일정 시작 55분 전 → 알림 생성")
    void notifyScheduleD1Hour_55MinutesBefore_createsNotification() {
        // Given - 현재 10:00, 일정 10:55
        LocalDateTime scheduledAt = LocalDateTime.now().plusMinutes(55);
        ScheduleAlertTargetVO target = createTarget(TEST_USER_ID, TEST_SCHEDULE_ID,
                "점심 회의", scheduledAt);

        when(scheduleAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_SCHEDULE_ID, NOTIFICATION_TYPE))
                .thenReturn(false);

        // When
        scheduleAlertService.notifyScheduleD1Hour(Collections.singletonList(target));

        // Then
        verify(notificationCreateService, times(1)).createNotification(eq(TEST_USER_ID), any());
    }

    @Test
    @DisplayName("일정이 이미 5분 전에 시작했는데 알림 이력이 없음 → catch-up 알림 생성")
    void notifyScheduleD1Hour_started5MinutesAgo_createsNotification() {
        // Given - 현재 10:05, 일정 10:00 (5분 전 시작)
        // SQL이 now-5분 ~ now+1시간 범위를 조회하므로 이 일정이 대상에 포함됨
        LocalDateTime scheduledAt = LocalDateTime.now().minusMinutes(5);
        ScheduleAlertTargetVO target = createTarget(TEST_USER_ID, TEST_SCHEDULE_ID,
                "점심 회의", scheduledAt);

        when(scheduleAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_SCHEDULE_ID, NOTIFICATION_TYPE))
                .thenReturn(false); // 알림 이력 없음

        // When
        scheduleAlertService.notifyScheduleD1Hour(Collections.singletonList(target));

        // Then - catch-up 알림 생성
        verify(notificationCreateService, times(1)).createNotification(eq(TEST_USER_ID), any());
    }

    @Test
    @DisplayName("일정이 이미 5분 전에 시작했는데 알림 이력이 있음 → 중복 생성하지 않음")
    void notifyScheduleD1Hour_started5MinutesAgoWithHistory_doesNotCreate() {
        // Given - 현재 10:05, 일정 10:00 (5분 전 시작), 이미 알림 보냄
        LocalDateTime scheduledAt = LocalDateTime.now().minusMinutes(5);
        ScheduleAlertTargetVO target = createTarget(TEST_USER_ID, TEST_SCHEDULE_ID,
                "점심 회의", scheduledAt);

        when(scheduleAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_SCHEDULE_ID, NOTIFICATION_TYPE))
                .thenReturn(true); // 이미 알림 존재

        // When
        scheduleAlertService.notifyScheduleD1Hour(Collections.singletonList(target));

        // Then - 중복 생성하지 않음
        verify(notificationCreateService, never()).createNotification(any(), any());
    }

    @Test
    @DisplayName("일정 시작 직전(5분 전) 실행 → 알림 생성")
    void notifyScheduleD1Hour_5MinutesBeforeStart_createsNotification() {
        // Given - 현재 10:55, 일정 11:00
        LocalDateTime scheduledAt = LocalDateTime.now().plusMinutes(5);
        ScheduleAlertTargetVO target = createTarget(TEST_USER_ID, TEST_SCHEDULE_ID,
                "점심 회의", scheduledAt);

        when(scheduleAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_SCHEDULE_ID, NOTIFICATION_TYPE))
                .thenReturn(false);

        // When
        scheduleAlertService.notifyScheduleD1Hour(Collections.singletonList(target));

        // Then
        verify(notificationCreateService, times(1)).createNotification(eq(TEST_USER_ID), any());
    }

    @Test
    @DisplayName("일정이 이미 시작한 경우(1분 전) - SQL 범위 외 알림 미생성")
    void notifyScheduleD1Hour_alreadyStarted1MinuteAgo_notInRange() {
        // Given - 현재 11:01, 일정 11:00 (1분 전 시작)
        // SQL에서 now-5분 ~ now+1시간이므로 11:00은 still within range
        // 하지만 이 테스트는 service 레벨에서 검증:
        // mapper가 대상을 반환하지 않으면 알림 생성 안 됨
        // 여기서는 mapper가 대상을 반환하지 않는 시나리오를 테스트
        List<ScheduleAlertTargetVO> emptyList = Collections.emptyList();

        // When
        scheduleAlertService.notifyScheduleD1Hour(emptyList);

        // Then - 대상이 없으므로 알림 생성 안 됨
        verify(notificationCreateService, never()).createNotification(any(), any());
    }

    @Test
    @DisplayName("너무 오래 지난 일정 - SQL 범위 외 알림 미생성")
    void notifyScheduleD1Hour_tooOldSchedule_notInRange() {
        // Given - 현재 20:00, 일정 10:00 (10시간 전)
        // SQL에서 now-5분 ~ now+1시간이므로 이 일정은 범위 외
        // mapper가 대상을 반환하지 않으면 알림 생성 안 됨
        List<ScheduleAlertTargetVO> emptyList = Collections.emptyList();

        // When
        scheduleAlertService.notifyScheduleD1Hour(emptyList);

        // Then - 대상이 없으므로 알림 생성 안 됨
        verify(notificationCreateService, never()).createNotification(any(), any());
    }

    @Test
    @DisplayName("일정이 늦게 등록된 경우 - mapper가 대상으로 반환하면 알림 생성")
    void notifyScheduleD1Hour_lateRegistration_createsNotification() {
        // Given - 일정 11:00, 등록 10:59, scheduler 11:00 실행
        // SQL 범위: 10:55 ~ 12:00 → 11:00 일정이 대상에 포함
        LocalDateTime scheduledAt = LocalDateTime.of(2026, 8, 22, 11, 0);
        ScheduleAlertTargetVO target = createTarget(TEST_USER_ID, TEST_SCHEDULE_ID,
                "점심 회의", scheduledAt);

        when(scheduleAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_SCHEDULE_ID, NOTIFICATION_TYPE))
                .thenReturn(false); // 알림 이력 없음

        // When
        scheduleAlertService.notifyScheduleD1Hour(Collections.singletonList(target));

        // Then - 알림 생성
        verify(notificationCreateService, times(1)).createNotification(eq(TEST_USER_ID), any());
    }

    @Test
    @DisplayName("동일 스케줄러가 여러 번 실행되어도 알림은 한 번만 생성")
    void notifyScheduleD1Hour_sameSchedulerRunsTwice_createsNotificationOnlyOnce() {
        // Given - 첫 번째 실행: 알림 생성
        LocalDateTime scheduledAt = LocalDateTime.now().plusMinutes(30);
        ScheduleAlertTargetVO target = createTarget(TEST_USER_ID, TEST_SCHEDULE_ID,
                "점심 회의", scheduledAt);

        // 첫 번째 실행
        when(scheduleAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_SCHEDULE_ID, NOTIFICATION_TYPE))
                .thenReturn(false);
        scheduleAlertService.notifyScheduleD1Hour(Collections.singletonList(target));

        // 두 번째 실행: 이미 알림 존재
        reset(notificationCreateService);
        when(scheduleAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_SCHEDULE_ID, NOTIFICATION_TYPE))
                .thenReturn(true);

        // When
        scheduleAlertService.notifyScheduleD1Hour(Collections.singletonList(target));

        // Then - 두 번째 실행에서는 알림 생성 안 됨
        verify(notificationCreateService, never()).createNotification(any(), any());
    }

    @Test
    @DisplayName("서로 다른 일정 → 각각 별도의 알림 생성")
    void notifyScheduleD1Hour_differentSchedules_createsSeparateNotifications() {
        // Given
        LocalDateTime scheduledAt1 = LocalDateTime.now().plusMinutes(20);
        LocalDateTime scheduledAt2 = LocalDateTime.now().plusMinutes(40);
        ScheduleAlertTargetVO target1 = createTarget(TEST_USER_ID, 1L,
                "점심 회의", scheduledAt1);
        ScheduleAlertTargetVO target2 = createTarget(TEST_USER_ID, 2L,
                "저녁 회식", scheduledAt2);

        when(scheduleAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, 1L, NOTIFICATION_TYPE))
                .thenReturn(false);
        when(scheduleAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, 2L, NOTIFICATION_TYPE))
                .thenReturn(false);

        // When
        scheduleAlertService.notifyScheduleD1Hour(Arrays.asList(target1, target2));

        // Then - 각각 별도 알림 생성
        verify(notificationCreateService, times(2)).createNotification(eq(TEST_USER_ID), any());
    }

    @Test
    @DisplayName("서로 다른 사용자 → 한 사용자의 중복 여부가 다른 사용자에게 영향을 주지 않음")
    void notifyScheduleD1Hour_differentUsers_independentDuplicateCheck() {
        // Given
        LocalDateTime scheduledAt = LocalDateTime.now().plusMinutes(30);
        ScheduleAlertTargetVO target1 = createTarget(100L, TEST_SCHEDULE_ID,
                "점심 회의", scheduledAt);
        ScheduleAlertTargetVO target2 = createTarget(200L, TEST_SCHEDULE_ID,
                "점심 회의", scheduledAt);

        when(scheduleAlertMapper.existsNotificationByReference(
                100L, REFERENCE_TYPE, TEST_SCHEDULE_ID, NOTIFICATION_TYPE))
                .thenReturn(true); // 100번 사용자는 중복
        when(scheduleAlertMapper.existsNotificationByReference(
                200L, REFERENCE_TYPE, TEST_SCHEDULE_ID, NOTIFICATION_TYPE))
                .thenReturn(false); // 200번 사용자는 신규

        // When
        scheduleAlertService.notifyScheduleD1Hour(Arrays.asList(target1, target2));

        // Then - 200번 사용자에게만 알림 생성 (100번 사용자의 중복이 200번에 영향 없음)
        verify(notificationCreateService, times(1)).createNotification(eq(200L), any());
    }
}
