package com.workit.domain.settlement.service;

import com.workit.domain.notification.dto.request.NotificationCreateRequestDTO;
import com.workit.domain.notification.enums.NotificationCategory;
import com.workit.domain.notification.service.NotificationCreateService;
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

@ExtendWith(MockitoExtension.class)
class SettlementAlertServiceImplTest {

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
        settlementAlertService = new SettlementAlertServiceImpl(notificationCreateService);
    }

    private WorkationVO createWorkation(Long userId, Long workationId) {
        WorkationVO vo = new WorkationVO();
        vo.setId(workationId);
        vo.setUserId(userId);
        vo.setTitle("테스트 워케이션");
        vo.setStartDate(LocalDate.now().minusDays(10));
        vo.setEndDate(LocalDate.now().minusDays(3));
        return vo;
    }

    @Test
    @DisplayName("정산 지연 알림 → SETTLEMENT_OVERDUE 알림 생성")
    void notifySettlementOverdue_createsNotification() {
        WorkationVO workation = createWorkation(TEST_USER_ID, TEST_WORKATION_ID);
        Map<Long, Integer> unconfirmedCounts = new HashMap<>();
        unconfirmedCounts.put(TEST_WORKATION_ID, 2);

        settlementAlertService.notifySettlementOverdue(
                Collections.singletonList(workation), unconfirmedCounts);

        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());

        NotificationCreateRequestDTO request = captor.getValue();
        assertEquals(NotificationCategory.SETTLEMENT_NOTIFY, request.getCategory());
        assertEquals(NOTIFICATION_TYPE_OVERDUE, request.getNotificationType());
        assertEquals(REFERENCE_TYPE, request.getReferenceType());
        assertEquals(TEST_WORKATION_ID, request.getReferenceId());
        assertTrue(request.getImportant());
        assertEquals(2, request.getPlaceholders().get("unconfirmedCount"));
    }

    @Test
    @DisplayName("미확인 지출 3건 이상 → UNCONFIRMED_EXPENSE_OVER_3 알림 생성")
    void notifyUnconfirmedExpenses_over3_createsNotification() {
        WorkationUncheckedCountVO unchecked = new WorkationUncheckedCountVO();
        unchecked.setUserId(TEST_USER_ID);
        unchecked.setWorkationId(TEST_WORKATION_ID);
        unchecked.setUncheckedCount(5);

        settlementAlertService.notifyUnconfirmedExpenses(Collections.singletonList(unchecked));

        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());

        NotificationCreateRequestDTO request = captor.getValue();
        assertEquals(NotificationCategory.SETTLEMENT_NOTIFY, request.getCategory());
        assertEquals(NOTIFICATION_TYPE_UNCONFIRMED, request.getNotificationType());
        assertFalse(request.getImportant());
        assertEquals(5, request.getPlaceholders().get("unconfirmedCount"));
    }

    @Test
    @DisplayName("미확인 지출 2건 → 알림 미생성 (3건 미만)")
    void notifyUnconfirmedExpenses_under3_doesNotCreate() {
        WorkationUncheckedCountVO unchecked = new WorkationUncheckedCountVO();
        unchecked.setUserId(TEST_USER_ID);
        unchecked.setWorkationId(TEST_WORKATION_ID);
        unchecked.setUncheckedCount(2);

        settlementAlertService.notifyUnconfirmedExpenses(Collections.singletonList(unchecked));

        verify(notificationCreateService, never()).createNotification(any(), any());
    }

    @Test
    @DisplayName("빈 목록 → 알림 미생성")
    void notifySettlementOverdue_emptyList_doesNotCreate() {
        settlementAlertService.notifySettlementOverdue(Collections.emptyList(), new HashMap<>());
        verify(notificationCreateService, never()).createNotification(any(), any());
    }

    @Test
    @DisplayName("null 목록 → 알림 미생성")
    void notifySettlementOverdue_nullList_doesNotCreate() {
        settlementAlertService.notifySettlementOverdue(null, null);
        verify(notificationCreateService, never()).createNotification(any(), any());
    }
}
