package com.workit.domain.budget.service;

import com.workit.domain.budget.mapper.BudgetMapper;
import com.workit.domain.budget.vo.BudgetItemVO;
import com.workit.domain.notification.dto.request.NotificationCreateRequestDTO;
import com.workit.domain.notification.enums.NotificationCategory;
import com.workit.domain.notification.service.NotificationCreateService;
import com.workit.domain.workation.vo.BudgetType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// BudgetAlertServiceImpl 테스트
// - 80% 알림, 초과 알림, 카테고리별 매핑 등을 검증한다
// - 중복 알림 방지는 NotificationCreateServiceImpl 내부에서 처리하므로, 이 테스트에서는
//   BudgetAlertServiceImpl이 올바른 기준으로 알림 생성을 판단하는지만 검증한다
@ExtendWith(MockitoExtension.class)
class BudgetAlertServiceImplTest {

    @Mock
    private BudgetMapper budgetMapper;

    @Mock
    private NotificationCreateService notificationCreateService;

    private BudgetAlertServiceImpl budgetAlertService;

    private static final Long TEST_USER_ID = 100L;
    private static final Long TEST_WORKATION_ID = 1L;
    private static final Long TEST_EXPENSE_CATEGORY_ID = 5L; // 식비
    private static final Long TEST_BUDGET_ID = 29L;

    @BeforeEach
    void setUp() {
        budgetAlertService = new BudgetAlertServiceImpl(
                budgetMapper, notificationCreateService);
    }

    private BudgetItemVO createBudgetItem(BudgetType budgetType, Long categoryId,
                                           BigDecimal targetAmount, BigDecimal spentAmount,
                                           String categoryName, String categoryCode) {
        BudgetItemVO vo = new BudgetItemVO();
        vo.setBudgetId(TEST_BUDGET_ID);
        vo.setWorkationId(TEST_WORKATION_ID);
        vo.setBudgetType(budgetType);
        vo.setExpenseCategoryId(categoryId);
        vo.setTargetAmount(targetAmount);
        vo.setSpentAmount(spentAmount);
        vo.setCategoryName(categoryName);
        vo.setCategoryCode(categoryCode);
        vo.setCustomName(null);
        return vo;
    }

    // ================================================================
    // 1. 80% 소진 알림 테스트
    // ================================================================

    @Test
    @DisplayName("80% 도달 시 80% 알림 생성")
    void checkAndNotifyBudgetAlert_80Percent_createsNotification() {
        BudgetItemVO item = createBudgetItem(BudgetType.WORK, TEST_EXPENSE_CATEGORY_ID,
                BigDecimal.valueOf(100000), BigDecimal.valueOf(80000), "식비", "FOOD");

        when(budgetMapper.selectBudgetItemList(TEST_WORKATION_ID, TEST_USER_ID, BudgetType.WORK))
                .thenReturn(Collections.singletonList(item));

        budgetAlertService.checkAndNotifyBudgetAlert(
                TEST_USER_ID, TEST_WORKATION_ID, BudgetType.WORK, TEST_EXPENSE_CATEGORY_ID);

        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());

        NotificationCreateRequestDTO request = captor.getValue();
        assertEquals(NotificationCategory.BUDGET_NOTIFY, request.getCategory());
        assertEquals("WORK_FOOD_80_PERCENT", request.getNotificationType());
        assertEquals(80, request.getPlaceholders().get("usageRate"));
        assertEquals(80000L, request.getPlaceholders().get("amount"));
        assertEquals("BUDGET", request.getReferenceType());
        assertEquals(TEST_BUDGET_ID, request.getReferenceId());
    }

    @Test
    @DisplayName("79%에서는 알림 미생성")
    void checkAndNotifyBudgetAlert_below80Percent_doesNotCreate() {
        BudgetItemVO item = createBudgetItem(BudgetType.WORK, TEST_EXPENSE_CATEGORY_ID,
                BigDecimal.valueOf(100000), BigDecimal.valueOf(79000), "식비", "FOOD");

        when(budgetMapper.selectBudgetItemList(TEST_WORKATION_ID, TEST_USER_ID, BudgetType.WORK))
                .thenReturn(Collections.singletonList(item));

        budgetAlertService.checkAndNotifyBudgetAlert(
                TEST_USER_ID, TEST_WORKATION_ID, BudgetType.WORK, TEST_EXPENSE_CATEGORY_ID);

        verify(notificationCreateService, never()).createNotification(any(), any());
    }

    // ================================================================
    // 2. 초과 알림 테스트
    // ================================================================

    @Test
    @DisplayName("101% 초과 시 80% 알림 + 초과 알림 모두 생성")
    void checkAndNotifyBudgetAlert_exceeded_createsBothNotifications() {
        BudgetItemVO item = createBudgetItem(BudgetType.WORK, TEST_EXPENSE_CATEGORY_ID,
                BigDecimal.valueOf(100000), BigDecimal.valueOf(101000), "식비", "FOOD");

        when(budgetMapper.selectBudgetItemList(TEST_WORKATION_ID, TEST_USER_ID, BudgetType.WORK))
                .thenReturn(Collections.singletonList(item));

        budgetAlertService.checkAndNotifyBudgetAlert(
                TEST_USER_ID, TEST_WORKATION_ID, BudgetType.WORK, TEST_EXPENSE_CATEGORY_ID);

        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService, times(2)).createNotification(eq(TEST_USER_ID), captor.capture());

        List<NotificationCreateRequestDTO> requests = captor.getAllValues();
        assertEquals("WORK_FOOD_80_PERCENT", requests.get(0).getNotificationType());
        assertFalse(requests.get(0).getImportant());
        assertEquals("WORK_FOOD_EXCEEDED", requests.get(1).getNotificationType());
        assertTrue(requests.get(1).getImportant());
        assertEquals(101, requests.get(1).getPlaceholders().get("usageRate"));
        assertEquals(101000L, requests.get(1).getPlaceholders().get("amount"));
    }

    @Test
    @DisplayName("100% 정확히일 때는 초과 알림 미생성 (100% 초과만 해당)")
    void checkAndNotifyBudgetAlert_exact100Percent_doesNotCreateExceeded() {
        BudgetItemVO item = createBudgetItem(BudgetType.WORK, TEST_EXPENSE_CATEGORY_ID,
                BigDecimal.valueOf(100000), BigDecimal.valueOf(100000), "식비", "FOOD");

        when(budgetMapper.selectBudgetItemList(TEST_WORKATION_ID, TEST_USER_ID, BudgetType.WORK))
                .thenReturn(Collections.singletonList(item));

        budgetAlertService.checkAndNotifyBudgetAlert(
                TEST_USER_ID, TEST_WORKATION_ID, BudgetType.WORK, TEST_EXPENSE_CATEGORY_ID);

        // 80% 알림만 생성됨, 초과 알림은 생성되지 않음
        verify(notificationCreateService, times(1)).createNotification(eq(TEST_USER_ID), any());
    }

    // ================================================================
    // 3. 카테고리별 매핑 테스트
    // ================================================================

    @Test
    @DisplayName("법인 식비 → WORK_FOOD 알림 타입")
    void checkAndNotifyBudgetAlert_workFood_correctType() {
        BudgetItemVO item = createBudgetItem(BudgetType.WORK, TEST_EXPENSE_CATEGORY_ID,
                BigDecimal.valueOf(100000), BigDecimal.valueOf(80000), "식비", "FOOD");

        when(budgetMapper.selectBudgetItemList(TEST_WORKATION_ID, TEST_USER_ID, BudgetType.WORK))
                .thenReturn(Collections.singletonList(item));

        budgetAlertService.checkAndNotifyBudgetAlert(
                TEST_USER_ID, TEST_WORKATION_ID, BudgetType.WORK, TEST_EXPENSE_CATEGORY_ID);

        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());
        assertEquals("WORK_FOOD_80_PERCENT", captor.getValue().getNotificationType());
    }

    @Test
    @DisplayName("개인 식비 → PERSONAL_FOOD 알림 타입")
    void checkAndNotifyBudgetAlert_personalFood_correctType() {
        BudgetItemVO item = createBudgetItem(BudgetType.PERSONAL, TEST_EXPENSE_CATEGORY_ID,
                BigDecimal.valueOf(100000), BigDecimal.valueOf(80000), "식비", "FOOD");

        when(budgetMapper.selectBudgetItemList(TEST_WORKATION_ID, TEST_USER_ID, BudgetType.PERSONAL))
                .thenReturn(Collections.singletonList(item));

        budgetAlertService.checkAndNotifyBudgetAlert(
                TEST_USER_ID, TEST_WORKATION_ID, BudgetType.PERSONAL, TEST_EXPENSE_CATEGORY_ID);

        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());
        assertEquals("PERSONAL_FOOD_80_PERCENT", captor.getValue().getNotificationType());
    }

    @Test
    @DisplayName("법인 숙박비 → WORK_ACCOMMODATION 알림 타입")
    void checkAndNotifyBudgetAlert_workAccommodation_correctType() {
        BudgetItemVO item = createBudgetItem(BudgetType.WORK, 1L,
                BigDecimal.valueOf(100000), BigDecimal.valueOf(85000), "숙박비", "ACCOMMODATION");

        when(budgetMapper.selectBudgetItemList(TEST_WORKATION_ID, TEST_USER_ID, BudgetType.WORK))
                .thenReturn(Collections.singletonList(item));

        budgetAlertService.checkAndNotifyBudgetAlert(
                TEST_USER_ID, TEST_WORKATION_ID, BudgetType.WORK, 1L);

        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());
        assertEquals("WORK_ACCOMMODATION_80_PERCENT", captor.getValue().getNotificationType());
    }

    // ================================================================
    // 4. 공통 서비스 연동 테스트
    // ================================================================

    @Test
    @DisplayName("BUDGET_NOTIFY 카테고리로 호출됨")
    void checkAndNotifyBudgetAlert_correctCategory() {
        BudgetItemVO item = createBudgetItem(BudgetType.WORK, TEST_EXPENSE_CATEGORY_ID,
                BigDecimal.valueOf(100000), BigDecimal.valueOf(80000), "식비", "FOOD");

        when(budgetMapper.selectBudgetItemList(TEST_WORKATION_ID, TEST_USER_ID, BudgetType.WORK))
                .thenReturn(Collections.singletonList(item));

        budgetAlertService.checkAndNotifyBudgetAlert(
                TEST_USER_ID, TEST_WORKATION_ID, BudgetType.WORK, TEST_EXPENSE_CATEGORY_ID);

        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());
        assertEquals(NotificationCategory.BUDGET_NOTIFY, captor.getValue().getCategory());
    }

    @Test
    @DisplayName("placeholder에 usageRate와 amount가 정상 전달됨")
    void checkAndNotifyBudgetAlert_placeholdersCorrect() {
        BudgetItemVO item = createBudgetItem(BudgetType.WORK, TEST_EXPENSE_CATEGORY_ID,
                BigDecimal.valueOf(200000), BigDecimal.valueOf(170000), "식비", "FOOD");

        when(budgetMapper.selectBudgetItemList(TEST_WORKATION_ID, TEST_USER_ID, BudgetType.WORK))
                .thenReturn(Collections.singletonList(item));

        budgetAlertService.checkAndNotifyBudgetAlert(
                TEST_USER_ID, TEST_WORKATION_ID, BudgetType.WORK, TEST_EXPENSE_CATEGORY_ID);

        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());

        assertEquals(85, captor.getValue().getPlaceholders().get("usageRate"));
        assertEquals(170000L, captor.getValue().getPlaceholders().get("amount"));
    }

    // ================================================================
    // 5. 경계값 테스트
    // ================================================================

    @Test
    @DisplayName("80.9% 사용률 → 80% 알림 생성 (정수 내림)")
    void checkAndNotifyBudgetAlert_80Point9Percent_creates80Alert() {
        BudgetItemVO item = createBudgetItem(BudgetType.WORK, TEST_EXPENSE_CATEGORY_ID,
                BigDecimal.valueOf(100000), BigDecimal.valueOf(80900), "식비", "FOOD");

        when(budgetMapper.selectBudgetItemList(TEST_WORKATION_ID, TEST_USER_ID, BudgetType.WORK))
                .thenReturn(Collections.singletonList(item));

        budgetAlertService.checkAndNotifyBudgetAlert(
                TEST_USER_ID, TEST_WORKATION_ID, BudgetType.WORK, TEST_EXPENSE_CATEGORY_ID);

        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());
        assertEquals(80, captor.getValue().getPlaceholders().get("usageRate"));
    }

    @Test
    @DisplayName("사용 금액 0일 때 알림 미생성")
    void checkAndNotifyBudgetAlert_zeroSpent_doesNotCreate() {
        BudgetItemVO item = createBudgetItem(BudgetType.WORK, TEST_EXPENSE_CATEGORY_ID,
                BigDecimal.valueOf(100000), BigDecimal.ZERO, "식비", "FOOD");

        when(budgetMapper.selectBudgetItemList(TEST_WORKATION_ID, TEST_USER_ID, BudgetType.WORK))
                .thenReturn(Collections.singletonList(item));

        budgetAlertService.checkAndNotifyBudgetAlert(
                TEST_USER_ID, TEST_WORKATION_ID, BudgetType.WORK, TEST_EXPENSE_CATEGORY_ID);

        verify(notificationCreateService, never()).createNotification(any(), any());
    }

    @Test
    @DisplayName("예산 배정 금액 0일 때 알림 미생성")
    void checkAndNotifyBudgetAlert_zeroTarget_doesNotCreate() {
        BudgetItemVO item = createBudgetItem(BudgetType.WORK, TEST_EXPENSE_CATEGORY_ID,
                BigDecimal.ZERO, BigDecimal.ZERO, "식비", "FOOD");

        when(budgetMapper.selectBudgetItemList(TEST_WORKATION_ID, TEST_USER_ID, BudgetType.WORK))
                .thenReturn(Collections.singletonList(item));

        budgetAlertService.checkAndNotifyBudgetAlert(
                TEST_USER_ID, TEST_WORKATION_ID, BudgetType.WORK, TEST_EXPENSE_CATEGORY_ID);

        verify(notificationCreateService, never()).createNotification(any(), any());
    }

    @Test
    @DisplayName("예산 배정이 없는 카테고리일 때 알림 미생성")
    void checkAndNotifyBudgetAlert_noBudgetItem_doesNotCreate() {
        when(budgetMapper.selectBudgetItemList(TEST_WORKATION_ID, TEST_USER_ID, BudgetType.WORK))
                .thenReturn(Collections.emptyList());

        budgetAlertService.checkAndNotifyBudgetAlert(
                TEST_USER_ID, TEST_WORKATION_ID, BudgetType.WORK, TEST_EXPENSE_CATEGORY_ID);

        verify(notificationCreateService, never()).createNotification(any(), any());
    }

    // ================================================================
    // 6. 중요 알림 플래그 테스트
    // ================================================================

    @Test
    @DisplayName("80% 알림은 중요 알림 아님")
    void checkAndNotifyBudgetAlert_80Percent_notImportant() {
        BudgetItemVO item = createBudgetItem(BudgetType.WORK, TEST_EXPENSE_CATEGORY_ID,
                BigDecimal.valueOf(100000), BigDecimal.valueOf(80000), "식비", "FOOD");

        when(budgetMapper.selectBudgetItemList(TEST_WORKATION_ID, TEST_USER_ID, BudgetType.WORK))
                .thenReturn(Collections.singletonList(item));

        budgetAlertService.checkAndNotifyBudgetAlert(
                TEST_USER_ID, TEST_WORKATION_ID, BudgetType.WORK, TEST_EXPENSE_CATEGORY_ID);

        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());
        assertFalse(captor.getValue().getImportant());
    }

    @Test
    @DisplayName("초과 알림은 중요 알림")
    void checkAndNotifyBudgetAlert_exceeded_isImportant() {
        BudgetItemVO item = createBudgetItem(BudgetType.WORK, TEST_EXPENSE_CATEGORY_ID,
                BigDecimal.valueOf(100000), BigDecimal.valueOf(105000), "식비", "FOOD");

        when(budgetMapper.selectBudgetItemList(TEST_WORKATION_ID, TEST_USER_ID, BudgetType.WORK))
                .thenReturn(Collections.singletonList(item));

        budgetAlertService.checkAndNotifyBudgetAlert(
                TEST_USER_ID, TEST_WORKATION_ID, BudgetType.WORK, TEST_EXPENSE_CATEGORY_ID);

        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService, times(2)).createNotification(eq(TEST_USER_ID), captor.capture());

        List<NotificationCreateRequestDTO> requests = captor.getAllValues();
        assertEquals("WORK_FOOD_EXCEEDED", requests.get(1).getNotificationType());
        assertTrue(requests.get(1).getImportant());
    }
}
