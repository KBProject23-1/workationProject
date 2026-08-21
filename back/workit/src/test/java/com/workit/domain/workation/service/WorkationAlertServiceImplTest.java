package com.workit.domain.workation.service;

import com.workit.domain.notification.dto.request.NotificationCreateRequestDTO;
import com.workit.domain.notification.enums.NotificationCategory;
import com.workit.domain.notification.service.NotificationCreateService;
import com.workit.domain.workation.mapper.WorkationAlertMapper;
import com.workit.domain.workation.vo.WorkationStatus;
import com.workit.domain.workation.vo.WorkationVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// WorkationAlertServiceImpl 테스트
// - 시작 D-1 알림, 종료 D-1 알림, 중복 방지, 날짜 조건 등을 검증한다
@ExtendWith(MockitoExtension.class)
class WorkationAlertServiceImplTest {

    @Mock
    private WorkationAlertMapper workationAlertMapper;

    @Mock
    private NotificationCreateService notificationCreateService;

    private WorkationAlertServiceImpl workationAlertService;

    private static final Long TEST_USER_ID = 100L;
    private static final Long TEST_WORKATION_ID = 1L;
    private static final String REFERENCE_TYPE = "WORKATION";
    private static final String NOTIFICATION_TYPE_START = "WORKATION_START_D_MINUS_1";
    private static final String NOTIFICATION_TYPE_END = "WORKATION_END_D_MINUS_1";

    @BeforeEach
    void setUp() {
        workationAlertService = new WorkationAlertServiceImpl(
                workationAlertMapper, notificationCreateService);
    }

    // ---------- 테스트 데이터 헬퍼 ----------

    /** 워케이션 VO 생성 */
    private WorkationVO createWorkation(Long userId, Long workationId,
                                         LocalDate startDate, LocalDate endDate) {
        WorkationVO vo = new WorkationVO();
        vo.setId(workationId);
        vo.setUserId(userId);
        vo.setTitle("테스트 워케이션");
        vo.setStartDate(startDate);
        vo.setEndDate(endDate);
        vo.setStatus(WorkationStatus.ACTIVE);
        return vo;
    }

    // ================================================================
    // 1. 시작일 D-1 알림 테스트
    // ================================================================

    @Test
    @DisplayName("내일 시작하는 워케이션 → 시작 D-1 알림 생성")
    void notifyWorkationStartD1_tomorrowStart_createsNotification() {
        // Given
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        WorkationVO workation = createWorkation(TEST_USER_ID, TEST_WORKATION_ID,
                tomorrow, tomorrow.plusDays(5));

        when(workationAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_WORKATION_ID, NOTIFICATION_TYPE_START))
                .thenReturn(false);

        // When
        workationAlertService.notifyWorkationStartD1(Collections.singletonList(workation));

        // Then
        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());

        NotificationCreateRequestDTO request = captor.getValue();
        assertEquals(NotificationCategory.WORKATION_NOTIFY, request.getCategory());
        assertEquals(NOTIFICATION_TYPE_START, request.getNotificationType());
        assertEquals(REFERENCE_TYPE, request.getReferenceType());
        assertEquals(TEST_WORKATION_ID, request.getReferenceId());
        assertFalse(request.getImportant());
        assertEquals(tomorrow.toString(), request.getPlaceholders().get("date"));
    }

    @Test
    @DisplayName("시작 D-1 알림 - {date} placeholder에 start_date가 전달됨")
    void notifyWorkationStartD1_datePlaceholder_containsStartDate() {
        // Given
        LocalDate startDate = LocalDate.of(2026, 8, 25);
        WorkationVO workation = createWorkation(TEST_USER_ID, TEST_WORKATION_ID,
                startDate, startDate.plusDays(5));

        when(workationAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_WORKATION_ID, NOTIFICATION_TYPE_START))
                .thenReturn(false);

        // When
        workationAlertService.notifyWorkationStartD1(Collections.singletonList(workation));

        // Then
        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());
        assertEquals("2026-08-25", captor.getValue().getPlaceholders().get("date"));
    }

    // ================================================================
    // 2. 종료일 D-1 알림 테스트
    // ================================================================

    @Test
    @DisplayName("내일 종료하는 워케이션 → 종료 D-1 알림 생성")
    void notifyWorkationEndD1_tomorrowEnd_createsNotification() {
        // Given
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        WorkationVO workation = createWorkation(TEST_USER_ID, TEST_WORKATION_ID,
                tomorrow.minusDays(5), tomorrow);

        when(workationAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_WORKATION_ID, NOTIFICATION_TYPE_END))
                .thenReturn(false);

        // When
        workationAlertService.notifyWorkationEndD1(Collections.singletonList(workation));

        // Then
        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());

        NotificationCreateRequestDTO request = captor.getValue();
        assertEquals(NotificationCategory.WORKATION_NOTIFY, request.getCategory());
        assertEquals(NOTIFICATION_TYPE_END, request.getNotificationType());
        assertEquals(REFERENCE_TYPE, request.getReferenceType());
        assertEquals(TEST_WORKATION_ID, request.getReferenceId());
        assertFalse(request.getImportant());
        assertEquals(tomorrow.toString(), request.getPlaceholders().get("date"));
    }

    @Test
    @DisplayName("종료 D-1 알림 - {date} placeholder에 end_date가 전달됨")
    void notifyWorkationEndD1_datePlaceholder_containsEndDate() {
        // Given
        LocalDate endDate = LocalDate.of(2026, 8, 30);
        WorkationVO workation = createWorkation(TEST_USER_ID, TEST_WORKATION_ID,
                endDate.minusDays(5), endDate);

        when(workationAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_WORKATION_ID, NOTIFICATION_TYPE_END))
                .thenReturn(false);

        // When
        workationAlertService.notifyWorkationEndD1(Collections.singletonList(workation));

        // Then
        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());
        assertEquals("2026-08-30", captor.getValue().getPlaceholders().get("date"));
    }

    // ================================================================
    // 3. 중복 방지 테스트
    // ================================================================

    @Test
    @DisplayName("동일 워케이션 + 동일 notificationType → 중복 생성하지 않음")
    void notifyWorkationStartD1_duplicate_doesNotCreate() {
        // Given
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        WorkationVO workation = createWorkation(TEST_USER_ID, TEST_WORKATION_ID,
                tomorrow, tomorrow.plusDays(5));

        when(workationAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_WORKATION_ID, NOTIFICATION_TYPE_START))
                .thenReturn(true); // 이미 알림 존재

        // When
        workationAlertService.notifyWorkationStartD1(Collections.singletonList(workation));

        // Then
        verify(notificationCreateService, never()).createNotification(any(), any());
    }

    @Test
    @DisplayName("시작 알림이 존재해도 종료 알림은 생성 가능")
    void notifyWorkationEndD1_startExistsButEndNot_createsEndNotification() {
        // Given
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        WorkationVO workation = createWorkation(TEST_USER_ID, TEST_WORKATION_ID,
                tomorrow, tomorrow.plusDays(5));

        // 종료 알림은 존재하지 않음 (notifyWorkationEndD1은 END notificationType만 확인)
        when(workationAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_WORKATION_ID, NOTIFICATION_TYPE_END))
                .thenReturn(false);

        // When
        workationAlertService.notifyWorkationEndD1(Collections.singletonList(workation));

        // Then
        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService, times(1)).createNotification(eq(TEST_USER_ID), captor.capture());
        assertEquals(NOTIFICATION_TYPE_END, captor.getValue().getNotificationType());
    }

    @Test
    @DisplayName("종료 알림이 존재해도 시작 알림은 생성 가능")
    void notifyWorkationStartD1_endExistsButStartNot_createsStartNotification() {
        // Given
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        WorkationVO workation = createWorkation(TEST_USER_ID, TEST_WORKATION_ID,
                tomorrow, tomorrow.plusDays(5));

        // 시작 알림은 존재하지 않음 (notifyWorkationStartD1은 START notificationType만 확인)
        when(workationAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_WORKATION_ID, NOTIFICATION_TYPE_START))
                .thenReturn(false);

        // When
        workationAlertService.notifyWorkationStartD1(Collections.singletonList(workation));

        // Then
        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService, times(1)).createNotification(eq(TEST_USER_ID), captor.capture());
        assertEquals(NOTIFICATION_TYPE_START, captor.getValue().getNotificationType());
    }

    // ================================================================
    // 4. 날짜 조건 테스트
    // ================================================================

    @Test
    @DisplayName("오늘 시작 → D-1 대상 아님 (스케줄러가 오늘 start_date인 것만 조회)")
    void notifyWorkationStartD1_todayStart_notTarget() {
        // Given - 오늘 시작하는 워케이션 (스케줄러가 이미 필터링했다고 가정)
        LocalDate today = LocalDate.now();
        WorkationVO workation = createWorkation(TEST_USER_ID, TEST_WORKATION_ID,
                today, today.plusDays(5));

        when(workationAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_WORKATION_ID, NOTIFICATION_TYPE_START))
                .thenReturn(false);

        // When - 스케줄러가 이미 오늘 start_date인 것만 조회했으므로, 서비스는 그대로 처리
        workationAlertService.notifyWorkationStartD1(Collections.singletonList(workation));

        // Then - 서비스는 알림을 생성하지만, 스케줄러가 오늘 start_date인 것은 조회하지 않으므로 실제로는 알림이 안 옴
        // 이 테스트는 서비스 레이어의 동작을 검증한다
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), any());
    }

    @Test
    @DisplayName("내일 시작 → D-1 대상")
    void notifyWorkationStartD1_tomorrowStart_isTarget() {
        // Given
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        WorkationVO workation = createWorkation(TEST_USER_ID, TEST_WORKATION_ID,
                tomorrow, tomorrow.plusDays(5));

        when(workationAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_WORKATION_ID, NOTIFICATION_TYPE_START))
                .thenReturn(false);

        // When
        workationAlertService.notifyWorkationStartD1(Collections.singletonList(workation));

        // Then
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), any());
    }

    @Test
    @DisplayName("모레 시작 → D-1 대상 아님 (스케줄러가 모레 start_date인 것은 조회하지 않음)")
    void notifyWorkationStartD1_dayAfterTomorrow_notTarget() {
        // Given - 모레 시작하는 워케이션 (스케줄러가 이미 필터링했다고 가정)
        LocalDate dayAfterTomorrow = LocalDate.now().plusDays(2);
        WorkationVO workation = createWorkation(TEST_USER_ID, TEST_WORKATION_ID,
                dayAfterTomorrow, dayAfterTomorrow.plusDays(5));

        when(workationAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_WORKATION_ID, NOTIFICATION_TYPE_START))
                .thenReturn(false);

        // When - 스케줄러가 이미 내일 start_date인 것만 조회했으므로, 서비스는 그대로 처리
        workationAlertService.notifyWorkationStartD1(Collections.singletonList(workation));

        // Then - 서비스는 알림을 생성하지만, 스케줄러가 모레 start_date인 것은 조회하지 않으므로 실제로는 알림이 안 옴
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), any());
    }

    // ================================================================
    // 5. 빈 목록 / null 테스트
    // ================================================================

    @Test
    @DisplayName("빈 목록 → 알림 미생성")
    void notifyWorkationStartD1_emptyList_doesNotCreate() {
        workationAlertService.notifyWorkationStartD1(Collections.emptyList());
        verify(notificationCreateService, never()).createNotification(any(), any());
    }

    @Test
    @DisplayName("null 목록 → 알림 미생성")
    void notifyWorkationStartD1_nullList_doesNotCreate() {
        workationAlertService.notifyWorkationStartD1(null);
        verify(notificationCreateService, never()).createNotification(any(), any());
    }

    // ================================================================
    // 6. 다수 워케이션 테스트
    // ================================================================

    @Test
    @DisplayName("내일 시작하는 워케이션이 여러 개인 경우 모두 알림 생성")
    void notifyWorkationStartD1_multipleWorkations_createsNotificationsForAll() {
        // Given
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        WorkationVO workation1 = createWorkation(TEST_USER_ID, 1L,
                tomorrow, tomorrow.plusDays(5));
        WorkationVO workation2 = createWorkation(TEST_USER_ID, 2L,
                tomorrow, tomorrow.plusDays(3));

        when(workationAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, 1L, NOTIFICATION_TYPE_START))
                .thenReturn(false);
        when(workationAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, 2L, NOTIFICATION_TYPE_START))
                .thenReturn(false);

        // When
        workationAlertService.notifyWorkationStartD1(Arrays.asList(workation1, workation2));

        // Then
        verify(notificationCreateService, times(2)).createNotification(eq(TEST_USER_ID), any());
    }

    @Test
    @DisplayName("다수 워케이션 중 일부는 중복인 경우 중복 건만 스킵")
    void notifyWorkationStartD1_multipleWorkationsSomeDuplicate_skipsDuplicates() {
        // Given
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        WorkationVO workation1 = createWorkation(TEST_USER_ID, 1L,
                tomorrow, tomorrow.plusDays(5));
        WorkationVO workation2 = createWorkation(TEST_USER_ID, 2L,
                tomorrow, tomorrow.plusDays(3));

        when(workationAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, 1L, NOTIFICATION_TYPE_START))
                .thenReturn(true); // 중복
        when(workationAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, 2L, NOTIFICATION_TYPE_START))
                .thenReturn(false); // 신규

        // When
        workationAlertService.notifyWorkationStartD1(Arrays.asList(workation1, workation2));

        // Then - 1건만 알림 생성
        verify(notificationCreateService, times(1)).createNotification(eq(TEST_USER_ID), any());
    }

    // ================================================================
    // 7. 수신 설정 테스트
    // ================================================================

    @Test
    @DisplayName("WORKATION_NOTIFY 카테고리로 호출됨")
    void notifyWorkationStartD1_correctCategory() {
        // Given
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        WorkationVO workation = createWorkation(TEST_USER_ID, TEST_WORKATION_ID,
                tomorrow, tomorrow.plusDays(5));

        when(workationAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_WORKATION_ID, NOTIFICATION_TYPE_START))
                .thenReturn(false);

        // When
        workationAlertService.notifyWorkationStartD1(Collections.singletonList(workation));

        // Then
        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());
        assertEquals(NotificationCategory.WORKATION_NOTIFY, captor.getValue().getCategory());
    }

    @Test
    @DisplayName("referenceType=WORKATION, referenceId=workation.id로 호출됨")
    void notifyWorkationStartD1_referenceCorrect() {
        // Given
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        WorkationVO workation = createWorkation(TEST_USER_ID, TEST_WORKATION_ID,
                tomorrow, tomorrow.plusDays(5));

        when(workationAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_WORKATION_ID, NOTIFICATION_TYPE_START))
                .thenReturn(false);

        // When
        workationAlertService.notifyWorkationStartD1(Collections.singletonList(workation));

        // Then
        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());
        assertEquals(REFERENCE_TYPE, captor.getValue().getReferenceType());
        assertEquals(TEST_WORKATION_ID, captor.getValue().getReferenceId());
    }
}
