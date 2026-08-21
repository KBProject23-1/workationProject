package com.workit.domain.settlement.service;

import com.workit.domain.notification.dto.request.NotificationCreateRequestDTO;
import com.workit.domain.notification.enums.NotificationCategory;
import com.workit.domain.notification.service.NotificationCreateService;
import com.workit.domain.settlement.mapper.SettlementAlertMapper;
import com.workit.domain.workation.vo.WorkationUncheckedCountVO;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// SettlementAlertServiceImpl 테스트
// - SETTLEMENT_OVERDUE 알림, UNCONFIRMED_EXPENSE_OVER_3 알림, 중복 방지 등을 검증한다
@ExtendWith(MockitoExtension.class)
class SettlementAlertServiceImplTest {

    @Mock
    private SettlementAlertMapper settlementAlertMapper;

    @Mock
    private NotificationCreateService notificationCreateService;

    private SettlementAlertServiceImpl settlementAlertService;

    private static final Long TEST_USER_ID = 100L;
    private static final Long TEST_WORKATION_ID = 1L;
    private static final String REFERENCE_TYPE = "WORKATION";
    private static final String NOTIFICATION_TYPE_OVERDUE = "SETTLEMENT_OVERDUE";
    private static final String NOTIFICATION_TYPE_UNCONFIRMED = "UNCONFIRMED_EXPENSE_OVER_3";

    @BeforeEach
    void setUp() {
        settlementAlertService = new SettlementAlertServiceImpl(
                settlementAlertMapper, notificationCreateService);
    }

    // ---------- 테스트 데이터 헬퍼 ----------

    /** 워케이션 VO 생성 */
    private WorkationVO createWorkation(Long userId, Long workationId,
                                         LocalDate endDate) {
        WorkationVO vo = new WorkationVO();
        vo.setId(workationId);
        vo.setUserId(userId);
        vo.setTitle("테스트 워케이션");
        vo.setStartDate(endDate.minusDays(5));
        vo.setEndDate(endDate);
        return vo;
    }

    /** 미확인 지출 건수 VO 생성 */
    private WorkationUncheckedCountVO createUncheckedCount(Long userId, Long workationId,
                                                            int uncheckedCount) {
        WorkationUncheckedCountVO vo = new WorkationUncheckedCountVO();
        vo.setUserId(userId);
        vo.setWorkationId(workationId);
        vo.setUncheckedCount(uncheckedCount);
        return vo;
    }

    // ================================================================
    // 1. SETTLEMENT_OVERDUE 알림 테스트
    // ================================================================

    @Test
    @DisplayName("정산 지연 워케이션 → SETTLEMENT_OVERDUE 알림 생성")
    void notifySettlementOverdue_overdueWorkation_createsNotification() {
        // Given
        WorkationVO workation = createWorkation(TEST_USER_ID, TEST_WORKATION_ID,
                LocalDate.now().minusDays(5));

        when(settlementAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_WORKATION_ID, NOTIFICATION_TYPE_OVERDUE))
                .thenReturn(false);

        Map<Long, Integer> unconfirmedCounts = new HashMap<>();
        unconfirmedCounts.put(TEST_WORKATION_ID, 3);

        // When
        settlementAlertService.notifySettlementOverdue(
                Collections.singletonList(workation), unconfirmedCounts);

        // Then
        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());

        NotificationCreateRequestDTO request = captor.getValue();
        assertEquals(NotificationCategory.SETTLEMENT_NOTIFY, request.getCategory());
        assertEquals(NOTIFICATION_TYPE_OVERDUE, request.getNotificationType());
        assertEquals(REFERENCE_TYPE, request.getReferenceType());
        assertEquals(TEST_WORKATION_ID, request.getReferenceId());
        assertTrue(request.getImportant());
        assertEquals(3, request.getPlaceholders().get("unconfirmedCount"));
    }

    @Test
    @DisplayName("SETTLEMENT_OVERDUE - {unconfirmedCount} placeholder에 미확인 지출 건수가 전달됨")
    void notifySettlementOverdue_unconfirmedCountPlaceholder_correct() {
        // Given
        WorkationVO workation = createWorkation(TEST_USER_ID, TEST_WORKATION_ID,
                LocalDate.now().minusDays(5));

        when(settlementAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_WORKATION_ID, NOTIFICATION_TYPE_OVERDUE))
                .thenReturn(false);

        Map<Long, Integer> unconfirmedCounts = new HashMap<>();
        unconfirmedCounts.put(TEST_WORKATION_ID, 7);

        // When
        settlementAlertService.notifySettlementOverdue(
                Collections.singletonList(workation), unconfirmedCounts);

        // Then
        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());
        assertEquals(7, captor.getValue().getPlaceholders().get("unconfirmedCount"));
    }

    @Test
    @DisplayName("SETTLEMENT_OVERDUE - unconfirmedCounts에 해당 workationId가 없으면 0 전달")
    void notifySettlementOverdue_noUnconfirmedCount_passesZero() {
        // Given
        WorkationVO workation = createWorkation(TEST_USER_ID, TEST_WORKATION_ID,
                LocalDate.now().minusDays(5));

        when(settlementAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_WORKATION_ID, NOTIFICATION_TYPE_OVERDUE))
                .thenReturn(false);

        Map<Long, Integer> unconfirmedCounts = new HashMap<>();

        // When
        settlementAlertService.notifySettlementOverdue(
                Collections.singletonList(workation), unconfirmedCounts);

        // Then
        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());
        assertEquals(0, captor.getValue().getPlaceholders().get("unconfirmedCount"));
    }

    // ================================================================
    // 2. UNCONFIRMED_EXPENSE_OVER_3 알림 테스트
    // ================================================================

    @Test
    @DisplayName("미확인 지출 3건 → UNCONFIRMED_EXPENSE_OVER_3 알림 생성")
    void notifyUnconfirmedExpenses_3Expenses_createsNotification() {
        // Given
        WorkationUncheckedCountVO unchecked = createUncheckedCount(TEST_USER_ID, TEST_WORKATION_ID, 3);

        when(settlementAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_WORKATION_ID, NOTIFICATION_TYPE_UNCONFIRMED))
                .thenReturn(false);

        // When
        settlementAlertService.notifyUnconfirmedExpenses(Collections.singletonList(unchecked));

        // Then
        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());

        NotificationCreateRequestDTO request = captor.getValue();
        assertEquals(NotificationCategory.SETTLEMENT_NOTIFY, request.getCategory());
        assertEquals(NOTIFICATION_TYPE_UNCONFIRMED, request.getNotificationType());
        assertEquals(REFERENCE_TYPE, request.getReferenceType());
        assertEquals(TEST_WORKATION_ID, request.getReferenceId());
        assertFalse(request.getImportant());
        assertEquals(3, request.getPlaceholders().get("unconfirmedCount"));
    }

    @Test
    @DisplayName("미확인 지출 4건 → UNCONFIRMED_EXPENSE_OVER_3 알림 생성")
    void notifyUnconfirmedExpenses_4Expenses_createsNotification() {
        // Given
        WorkationUncheckedCountVO unchecked = createUncheckedCount(TEST_USER_ID, TEST_WORKATION_ID, 4);

        when(settlementAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_WORKATION_ID, NOTIFICATION_TYPE_UNCONFIRMED))
                .thenReturn(false);

        // When
        settlementAlertService.notifyUnconfirmedExpenses(Collections.singletonList(unchecked));

        // Then
        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());
        assertEquals(4, captor.getValue().getPlaceholders().get("unconfirmedCount"));
    }

    @Test
    @DisplayName("미확인 지출 2건 → 알림 미생성 (3건 미만)")
    void notifyUnconfirmedExpenses_2Expenses_doesNotCreate() {
        // Given
        WorkationUncheckedCountVO unchecked = createUncheckedCount(TEST_USER_ID, TEST_WORKATION_ID, 2);

        // When
        settlementAlertService.notifyUnconfirmedExpenses(Collections.singletonList(unchecked));

        // Then
        verify(notificationCreateService, never()).createNotification(any(), any());
    }

    @Test
    @DisplayName("UNCONFIRMED_EXPENSE_OVER_3 - {unconfirmedCount} placeholder에 미확인 건수가 전달됨")
    void notifyUnconfirmedExpenses_placeholderCorrect() {
        // Given
        WorkationUncheckedCountVO unchecked = createUncheckedCount(TEST_USER_ID, TEST_WORKATION_ID, 5);

        when(settlementAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_WORKATION_ID, NOTIFICATION_TYPE_UNCONFIRMED))
                .thenReturn(false);

        // When
        settlementAlertService.notifyUnconfirmedExpenses(Collections.singletonList(unchecked));

        // Then
        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());
        assertEquals(5, captor.getValue().getPlaceholders().get("unconfirmedCount"));
    }

    // ================================================================
    // 3. 중복 방지 테스트
    // ================================================================

    @Test
    @DisplayName("동일 워케이션 + 동일 notificationType → 중복 생성하지 않음 (SETTLEMENT_OVERDUE)")
    void notifySettlementOverdue_duplicate_doesNotCreate() {
        // Given
        WorkationVO workation = createWorkation(TEST_USER_ID, TEST_WORKATION_ID,
                LocalDate.now().minusDays(5));

        when(settlementAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_WORKATION_ID, NOTIFICATION_TYPE_OVERDUE))
                .thenReturn(true);

        Map<Long, Integer> unconfirmedCounts = new HashMap<>();
        unconfirmedCounts.put(TEST_WORKATION_ID, 3);

        // When
        settlementAlertService.notifySettlementOverdue(
                Collections.singletonList(workation), unconfirmedCounts);

        // Then
        verify(notificationCreateService, never()).createNotification(any(), any());
    }

    @Test
    @DisplayName("동일 워케이션 + 동일 notificationType → 중복 생성하지 않음 (UNCONFIRMED_EXPENSE_OVER_3)")
    void notifyUnconfirmedExpenses_duplicate_doesNotCreate() {
        // Given
        WorkationUncheckedCountVO unchecked = createUncheckedCount(TEST_USER_ID, TEST_WORKATION_ID, 3);

        when(settlementAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_WORKATION_ID, NOTIFICATION_TYPE_UNCONFIRMED))
                .thenReturn(true);

        // When
        settlementAlertService.notifyUnconfirmedExpenses(Collections.singletonList(unchecked));

        // Then
        verify(notificationCreateService, never()).createNotification(any(), any());
    }

    @Test
    @DisplayName("SETTLEMENT_OVERDUE 알림이 존재해도 UNCONFIRMED_EXPENSE_OVER_3는 별도 알림으로 생성")
    void notifyUnconfirmedExpenses_overdueExistsButUnconfirmedNot_createsNotification() {
        // Given
        WorkationUncheckedCountVO unchecked = createUncheckedCount(TEST_USER_ID, TEST_WORKATION_ID, 3);

        // UNCONFIRMED_EXPENSE_OVER_3은 존재하지 않음
        when(settlementAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_WORKATION_ID, NOTIFICATION_TYPE_UNCONFIRMED))
                .thenReturn(false);

        // When
        settlementAlertService.notifyUnconfirmedExpenses(Collections.singletonList(unchecked));

        // Then
        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService, times(1)).createNotification(eq(TEST_USER_ID), captor.capture());
        assertEquals(NOTIFICATION_TYPE_UNCONFIRMED, captor.getValue().getNotificationType());
    }

    @Test
    @DisplayName("UNCONFIRMED_EXPENSE_OVER_3 알림이 존재해도 SETTLEMENT_OVERDUE는 별도 알림으로 생성")
    void notifySettlementOverdue_unconfirmedExistsButOverdueNot_createsNotification() {
        // Given
        WorkationVO workation = createWorkation(TEST_USER_ID, TEST_WORKATION_ID,
                LocalDate.now().minusDays(5));

        // SETTLEMENT_OVERDUE은 존재하지 않음
        when(settlementAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_WORKATION_ID, NOTIFICATION_TYPE_OVERDUE))
                .thenReturn(false);

        Map<Long, Integer> unconfirmedCounts = new HashMap<>();
        unconfirmedCounts.put(TEST_WORKATION_ID, 3);

        // When
        settlementAlertService.notifySettlementOverdue(
                Collections.singletonList(workation), unconfirmedCounts);

        // Then
        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService, times(1)).createNotification(eq(TEST_USER_ID), captor.capture());
        assertEquals(NOTIFICATION_TYPE_OVERDUE, captor.getValue().getNotificationType());
    }

    // ================================================================
    // 4. 빈 목록 / null 테스트
    // ================================================================

    @Test
    @DisplayName("빈 목록 → 알림 미생성 (SETTLEMENT_OVERDUE)")
    void notifySettlementOverdue_emptyList_doesNotCreate() {
        settlementAlertService.notifySettlementOverdue(Collections.emptyList(), new HashMap<>());
        verify(notificationCreateService, never()).createNotification(any(), any());
    }

    @Test
    @DisplayName("null 목록 → 알림 미생성 (SETTLEMENT_OVERDUE)")
    void notifySettlementOverdue_nullList_doesNotCreate() {
        settlementAlertService.notifySettlementOverdue(null, null);
        verify(notificationCreateService, never()).createNotification(any(), any());
    }

    @Test
    @DisplayName("빈 목록 → 알림 미생성 (UNCONFIRMED_EXPENSE_OVER_3)")
    void notifyUnconfirmedExpenses_emptyList_doesNotCreate() {
        settlementAlertService.notifyUnconfirmedExpenses(Collections.emptyList());
        verify(notificationCreateService, never()).createNotification(any(), any());
    }

    @Test
    @DisplayName("null 목록 → 알림 미생성 (UNCONFIRMED_EXPENSE_OVER_3)")
    void notifyUnconfirmedExpenses_nullList_doesNotCreate() {
        settlementAlertService.notifyUnconfirmedExpenses(null);
        verify(notificationCreateService, never()).createNotification(any(), any());
    }

    // ================================================================
    // 5. 다수 워케이션 테스트
    // ================================================================

    @Test
    @DisplayName("정산 지연 워케이션이 여러 개인 경우 모두 알림 생성")
    void notifySettlementOverdue_multipleWorkations_createsNotificationsForAll() {
        // Given
        WorkationVO workation1 = createWorkation(TEST_USER_ID, 1L, LocalDate.now().minusDays(5));
        WorkationVO workation2 = createWorkation(TEST_USER_ID, 2L, LocalDate.now().minusDays(4));

        when(settlementAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, 1L, NOTIFICATION_TYPE_OVERDUE))
                .thenReturn(false);
        when(settlementAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, 2L, NOTIFICATION_TYPE_OVERDUE))
                .thenReturn(false);

        Map<Long, Integer> unconfirmedCounts = new HashMap<>();
        unconfirmedCounts.put(1L, 3);
        unconfirmedCounts.put(2L, 5);

        // When
        settlementAlertService.notifySettlementOverdue(
                Arrays.asList(workation1, workation2), unconfirmedCounts);

        // Then
        verify(notificationCreateService, times(2)).createNotification(eq(TEST_USER_ID), any());
    }

    @Test
    @DisplayName("다수 미확인 지출 중 일부는 중복인 경우 중복 건만 스킵")
    void notifyUnconfirmedExpenses_multipleSomeDuplicate_skipsDuplicates() {
        // Given
        WorkationUncheckedCountVO unchecked1 = createUncheckedCount(TEST_USER_ID, 1L, 3);
        WorkationUncheckedCountVO unchecked2 = createUncheckedCount(TEST_USER_ID, 2L, 4);

        when(settlementAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, 1L, NOTIFICATION_TYPE_UNCONFIRMED))
                .thenReturn(true); // 중복
        when(settlementAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, 2L, NOTIFICATION_TYPE_UNCONFIRMED))
                .thenReturn(false); // 신규

        // When
        settlementAlertService.notifyUnconfirmedExpenses(Arrays.asList(unchecked1, unchecked2));

        // Then - 1건만 알림 생성
        verify(notificationCreateService, times(1)).createNotification(eq(TEST_USER_ID), any());
    }

    // ================================================================
    // 6. 공통 서비스 연동 테스트
    // ================================================================

    @Test
    @DisplayName("SETTLEMENT_NOTIFY 카테고리로 호출됨 (SETTLEMENT_OVERDUE)")
    void notifySettlementOverdue_correctCategory() {
        // Given
        WorkationVO workation = createWorkation(TEST_USER_ID, TEST_WORKATION_ID,
                LocalDate.now().minusDays(5));

        when(settlementAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_WORKATION_ID, NOTIFICATION_TYPE_OVERDUE))
                .thenReturn(false);

        Map<Long, Integer> unconfirmedCounts = new HashMap<>();
        unconfirmedCounts.put(TEST_WORKATION_ID, 3);

        // When
        settlementAlertService.notifySettlementOverdue(
                Collections.singletonList(workation), unconfirmedCounts);

        // Then
        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());
        assertEquals(NotificationCategory.SETTLEMENT_NOTIFY, captor.getValue().getCategory());
    }

    @Test
    @DisplayName("SETTLEMENT_NOTIFY 카테고리로 호출됨 (UNCONFIRMED_EXPENSE_OVER_3)")
    void notifyUnconfirmedExpenses_correctCategory() {
        // Given
        WorkationUncheckedCountVO unchecked = createUncheckedCount(TEST_USER_ID, TEST_WORKATION_ID, 3);

        when(settlementAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_WORKATION_ID, NOTIFICATION_TYPE_UNCONFIRMED))
                .thenReturn(false);

        // When
        settlementAlertService.notifyUnconfirmedExpenses(Collections.singletonList(unchecked));

        // Then
        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());
        assertEquals(NotificationCategory.SETTLEMENT_NOTIFY, captor.getValue().getCategory());
    }

    @Test
    @DisplayName("referenceType=WORKATION, referenceId=workation.id로 호출됨")
    void notifySettlementOverdue_referenceCorrect() {
        // Given
        WorkationVO workation = createWorkation(TEST_USER_ID, TEST_WORKATION_ID,
                LocalDate.now().minusDays(5));

        when(settlementAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_WORKATION_ID, NOTIFICATION_TYPE_OVERDUE))
                .thenReturn(false);

        Map<Long, Integer> unconfirmedCounts = new HashMap<>();
        unconfirmedCounts.put(TEST_WORKATION_ID, 3);

        // When
        settlementAlertService.notifySettlementOverdue(
                Collections.singletonList(workation), unconfirmedCounts);

        // Then
        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());
        assertEquals(REFERENCE_TYPE, captor.getValue().getReferenceType());
        assertEquals(TEST_WORKATION_ID, captor.getValue().getReferenceId());
    }

    // ================================================================
    // 7. 중요 알림 플래그 테스트
    // ================================================================

    @Test
    @DisplayName("SETTLEMENT_OVERDUE 알림은 중요 알림")
    void notifySettlementOverdue_isImportant() {
        // Given
        WorkationVO workation = createWorkation(TEST_USER_ID, TEST_WORKATION_ID,
                LocalDate.now().minusDays(5));

        when(settlementAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_WORKATION_ID, NOTIFICATION_TYPE_OVERDUE))
                .thenReturn(false);

        Map<Long, Integer> unconfirmedCounts = new HashMap<>();
        unconfirmedCounts.put(TEST_WORKATION_ID, 3);

        // When
        settlementAlertService.notifySettlementOverdue(
                Collections.singletonList(workation), unconfirmedCounts);

        // Then
        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());
        assertTrue(captor.getValue().getImportant());
    }

    @Test
    @DisplayName("UNCONFIRMED_EXPENSE_OVER_3 알림은 중요 알림 아님")
    void notifyUnconfirmedExpenses_notImportant() {
        // Given
        WorkationUncheckedCountVO unchecked = createUncheckedCount(TEST_USER_ID, TEST_WORKATION_ID, 3);

        when(settlementAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_WORKATION_ID, NOTIFICATION_TYPE_UNCONFIRMED))
                .thenReturn(false);

        // When
        settlementAlertService.notifyUnconfirmedExpenses(Collections.singletonList(unchecked));

        // Then
        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());
        assertFalse(captor.getValue().getImportant());
    }
}
