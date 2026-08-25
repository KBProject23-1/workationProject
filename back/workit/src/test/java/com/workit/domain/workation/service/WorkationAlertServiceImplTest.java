package com.workit.domain.workation.service;

import com.workit.domain.notification.dto.request.NotificationCreateRequestDTO;
import com.workit.domain.notification.enums.NotificationCategory;
import com.workit.domain.notification.service.NotificationCreateService;
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
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkationAlertServiceImplTest {

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
        workationAlertService = new WorkationAlertServiceImpl(notificationCreateService);
    }

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

    @Test
    @DisplayName("내일 시작하는 워케이션 → 시작 D-1 알림 생성")
    void notifyWorkationStartD1_createsNotification() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        WorkationVO workation = createWorkation(TEST_USER_ID, TEST_WORKATION_ID,
                tomorrow, tomorrow.plusDays(5));

        workationAlertService.notifyWorkationStartD1(Collections.singletonList(workation));

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
    @DisplayName("내일 종료하는 워케이션 → 종료 D-1 알림 생성")
    void notifyWorkationEndD1_createsNotification() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        WorkationVO workation = createWorkation(TEST_USER_ID, TEST_WORKATION_ID,
                tomorrow.minusDays(5), tomorrow);

        workationAlertService.notifyWorkationEndD1(Collections.singletonList(workation));

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

    @Test
    @DisplayName("시작 D-1 알림 - {date} placeholder에 start_date가 전달됨")
    void notifyWorkationStartD1_datePlaceholder_containsStartDate() {
        LocalDate startDate = LocalDate.of(2026, 8, 25);
        WorkationVO workation = createWorkation(TEST_USER_ID, TEST_WORKATION_ID,
                startDate, startDate.plusDays(5));

        workationAlertService.notifyWorkationStartD1(Collections.singletonList(workation));

        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());
        assertEquals("2026-08-25", captor.getValue().getPlaceholders().get("date"));
    }
}
