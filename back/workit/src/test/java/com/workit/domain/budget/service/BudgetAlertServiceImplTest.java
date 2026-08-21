package com.workit.domain.budget.service;

import com.workit.domain.budget.mapper.BudgetAlertMapper;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

//BudgetAlertServiceImpl 테스트
// - 80% 알림, 초과 알림, 중복 방지, 카테고리별 매핑 등을 검증한다
@ExtendWith(MockitoExtension.class)
class BudgetAlertServiceImplTest {

    @Mock
    private BudgetMapper budgetMapper;

    @Mock
    private BudgetAlertMapper budgetAlertMapper;

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
                budgetMapper, budgetAlertMapper, notificationCreateService);
    }

    // ---------- 테스트 데이터 헬퍼 ----------

    /**预算项目 VO 生成 (식비, WORK) */
    private BudgetItemVO createBudgetItem(BudgetType budgetType, Long categoryId,
                                           BigDecimal targetAmount, BigDecimal spentAmount,
                                           String categoryName) {
        BudgetItemVO vo = new BudgetItemVO();
        vo.setBudgetId(TEST_BUDGET_ID);
        vo.setWorkationId(TEST_WORKATION_ID);
        vo.setBudgetType(budgetType);
        vo.setExpenseCategoryId(categoryId);
        vo.setTargetAmount(targetAmount);
        vo.setSpentAmount(spentAmount);
        vo.setCategoryName(categoryName);
        vo.setCustomName(null);
        return vo;
    }

    // ================================================================
    // 1. 80% 소진 알림 테스트
    // ================================================================

    @Test
    @DisplayName("79% → 80% 도달 시 80% 알림 생성")
    void checkAndNotifyBudgetAlert_80PercentThreshold_createsNotification() {
        // Given - 사용률 80% (80,000 / 100,000)
        BudgetItemVO item = createBudgetItem(BudgetType.WORK, TEST_EXPENSE_CATEGORY_ID,
                BigDecimal.valueOf(100000), BigDecimal.valueOf(80000), "식비");

        when(budgetMapper.selectBudgetItemList(TEST_WORKATION_ID, TEST_USER_ID, BudgetType.WORK))
                .thenReturn(Collections.singletonList(item));
        when(budgetAlertMapper.existsNotificationByReference(
                TEST_USER_ID, "BUDGET", TEST_BUDGET_ID, "WORK_FOOD_80_PERCENT"))
                .thenReturn(false);

        // When
        budgetAlertService.checkAndNotifyBudgetAlert(
                TEST_USER_ID, TEST_WORKATION_ID, BudgetType.WORK, TEST_EXPENSE_CATEGORY_ID);

        // Then - 80% 알림이 생성됨
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
    @DisplayName("80%에서 추가 지출 → 알림 미생성 (중복 방지)")
    void checkAndNotifyBudgetAlert_alreadyHasAlert_doesNotCreate() {
        // Given - 사용률 90% (90,000 / 100,000), 이미 80% 알림 존재
        BudgetItemVO item = createBudgetItem(BudgetType.WORK, TEST_EXPENSE_CATEGORY_ID,
                BigDecimal.valueOf(100000), BigDecimal.valueOf(90000), "식비");

        when(budgetMapper.selectBudgetItemList(TEST_WORKATION_ID, TEST_USER_ID, BudgetType.WORK))
                .thenReturn(Collections.singletonList(item));
        when(budgetAlertMapper.existsNotificationByReference(
                TEST_USER_ID, "BUDGET", TEST_BUDGET_ID, "WORK_FOOD_80_PERCENT"))
                .thenReturn(true); // 이미 알림 존재

        // When
        budgetAlertService.checkAndNotifyBudgetAlert(
                TEST_USER_ID, TEST_WORKATION_ID, BudgetType.WORK, TEST_EXPENSE_CATEGORY_ID);

        // Then - 알림이 생성되지 않음
        verify(notificationCreateService, never()).createNotification(any(), any());
    }

    @Test
    @DisplayName("79%에서는 알림 미생성")
    void checkAndNotifyBudgetAlert_below80Percent_doesNotCreate() {
        // Given - 사용률 79% (79,000 / 100,000)
        BudgetItemVO item = createBudgetItem(BudgetType.WORK, TEST_EXPENSE_CATEGORY_ID,
                BigDecimal.valueOf(100000), BigDecimal.valueOf(79000), "식비");

        when(budgetMapper.selectBudgetItemList(TEST_WORKATION_ID, TEST_USER_ID, BudgetType.WORK))
                .thenReturn(Collections.singletonList(item));

        // When
        budgetAlertService.checkAndNotifyBudgetAlert(
                TEST_USER_ID, TEST_WORKATION_ID, BudgetType.WORK, TEST_EXPENSE_CATEGORY_ID);

        // Then - 80% 알림도 초과 알림도 생성되지 않음
        verify(notificationCreateService, never()).createNotification(any(), any());
    }

    // ================================================================
    // 2. 초과 알림 테스트
    // ================================================================

    @Test
    @DisplayName("99% → 101% 초과 시 80% 알림 + 초과 알림 모두 생성")
    void checkAndNotifyBudgetAlert_exceeded_createsBothNotifications() {
        // Given - 사용률 101% (101,000 / 100,000)
        BudgetItemVO item = createBudgetItem(BudgetType.WORK, TEST_EXPENSE_CATEGORY_ID,
                BigDecimal.valueOf(100000), BigDecimal.valueOf(101000), "식비");

        when(budgetMapper.selectBudgetItemList(TEST_WORKATION_ID, TEST_USER_ID, BudgetType.WORK))
                .thenReturn(Collections.singletonList(item));
        when(budgetAlertMapper.existsNotificationByReference(
                TEST_USER_ID, "BUDGET", TEST_BUDGET_ID, "WORK_FOOD_80_PERCENT"))
                .thenReturn(false);
        when(budgetAlertMapper.existsNotificationByReference(
                TEST_USER_ID, "BUDGET", TEST_BUDGET_ID, "WORK_FOOD_EXCEEDED"))
                .thenReturn(false);

        // When
        budgetAlertService.checkAndNotifyBudgetAlert(
                TEST_USER_ID, TEST_WORKATION_ID, BudgetType.WORK, TEST_EXPENSE_CATEGORY_ID);

        // Then - 80% 알림 + 초과 알림 모두 생성됨
        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService, times(2)).createNotification(eq(TEST_USER_ID), captor.capture());

        List<NotificationCreateRequestDTO> requests = captor.getAllValues();
        // 첫 번째: 80% 알림
        assertEquals("WORK_FOOD_80_PERCENT", requests.get(0).getNotificationType());
        assertFalse(requests.get(0).getImportant());
        // 두 번째: 초과 알림
        assertEquals("WORK_FOOD_EXCEEDED", requests.get(1).getNotificationType());
        assertTrue(requests.get(1).getImportant());
        assertEquals(101, requests.get(1).getPlaceholders().get("usageRate"));
        assertEquals(101000L, requests.get(1).getPlaceholders().get("amount"));
    }

    @Test
    @DisplayName("초과 이후 추가 지출 → 중복 알림 미생성")
    void checkAndNotifyBudgetAlert_alreadyExceeded_doesNotCreate() {
        // Given - 사용률 110% (110,000 / 100,000), 이미 초과 알림 존재
        BudgetItemVO item = createBudgetItem(BudgetType.WORK, TEST_EXPENSE_CATEGORY_ID,
                BigDecimal.valueOf(100000), BigDecimal.valueOf(110000), "식비");

        when(budgetMapper.selectBudgetItemList(TEST_WORKATION_ID, TEST_USER_ID, BudgetType.WORK))
                .thenReturn(Collections.singletonList(item));
        when(budgetAlertMapper.existsNotificationByReference(
                TEST_USER_ID, "BUDGET", TEST_BUDGET_ID, "WORK_FOOD_80_PERCENT"))
                .thenReturn(true);
        when(budgetAlertMapper.existsNotificationByReference(
                TEST_USER_ID, "BUDGET", TEST_BUDGET_ID, "WORK_FOOD_EXCEEDED"))
                .thenReturn(true);

        // When
        budgetAlertService.checkAndNotifyBudgetAlert(
                TEST_USER_ID, TEST_WORKATION_ID, BudgetType.WORK, TEST_EXPENSE_CATEGORY_ID);

        // Then - 알림이 생성되지 않음
        verify(notificationCreateService, never()).createNotification(any(), any());
    }

    @Test
    @DisplayName("100% 정확히일 때는 초과 알림 미생성 (100% 초과만 해당)")
    void checkAndNotifyBudgetAlert_exact100Percent_doesNotCreateExceeded() {
        // Given - 사용률 100% (100,000 / 100,000)
        BudgetItemVO item = createBudgetItem(BudgetType.WORK, TEST_EXPENSE_CATEGORY_ID,
                BigDecimal.valueOf(100000), BigDecimal.valueOf(100000), "식비");

        when(budgetMapper.selectBudgetItemList(TEST_WORKATION_ID, TEST_USER_ID, BudgetType.WORK))
                .thenReturn(Collections.singletonList(item));
        when(budgetAlertMapper.existsNotificationByReference(
                TEST_USER_ID, "BUDGET", TEST_BUDGET_ID, "WORK_FOOD_80_PERCENT"))
                .thenReturn(false);

        // When
        budgetAlertService.checkAndNotifyBudgetAlert(
                TEST_USER_ID, TEST_WORKATION_ID, BudgetType.WORK, TEST_EXPENSE_CATEGORY_ID);

        // Then - 80% 알림만 생성됨, 초과 알림은 생성되지 않음
        verify(notificationCreateService, times(1)).createNotification(eq(TEST_USER_ID), any());
    }

    // ================================================================
    // 3. notificationType별 중복 방지 테스트
    // ================================================================

    @Test
    @DisplayName("동일 예산 + 동일 notificationType → 중복으로 판단")
    void checkAndNotifyBudgetAlert_sameNotificationType_duplicateDetected() {
        // Given - 사용률 85%, 이미 WORK_FOOD_80_PERCENT 알림 존재
        BudgetItemVO item = createBudgetItem(BudgetType.WORK, TEST_EXPENSE_CATEGORY_ID,
                BigDecimal.valueOf(100000), BigDecimal.valueOf(85000), "식비");

        when(budgetMapper.selectBudgetItemList(TEST_WORKATION_ID, TEST_USER_ID, BudgetType.WORK))
                .thenReturn(Collections.singletonList(item));
        when(budgetAlertMapper.existsNotificationByReference(
                TEST_USER_ID, "BUDGET", TEST_BUDGET_ID, "WORK_FOOD_80_PERCENT"))
                .thenReturn(true);

        // When
        budgetAlertService.checkAndNotifyBudgetAlert(
                TEST_USER_ID, TEST_WORKATION_ID, BudgetType.WORK, TEST_EXPENSE_CATEGORY_ID);

        // Then - 동일 notificationType이므로 중복으로 판단, 알림 미생성
        verify(notificationCreateService, never()).createNotification(any(), any());
    }

    @Test
    @DisplayName("동일 예산 + 다른 notificationType → 중복으로 판단하지 않음")
    void checkAndNotifyBudgetAlert_differentNotificationType_notDuplicate() {
        // Given - 사용률 105%, WORK_FOOD_80_PERCENT 알림은 이미 존재하지만 WORK_FOOD_EXCEEDED는 없음
        BudgetItemVO item = createBudgetItem(BudgetType.WORK, TEST_EXPENSE_CATEGORY_ID,
                BigDecimal.valueOf(100000), BigDecimal.valueOf(105000), "식비");

        when(budgetMapper.selectBudgetItemList(TEST_WORKATION_ID, TEST_USER_ID, BudgetType.WORK))
                .thenReturn(Collections.singletonList(item));
        when(budgetAlertMapper.existsNotificationByReference(
                TEST_USER_ID, "BUDGET", TEST_BUDGET_ID, "WORK_FOOD_80_PERCENT"))
                .thenReturn(true);  // 80% 알림은 이미 존재
        when(budgetAlertMapper.existsNotificationByReference(
                TEST_USER_ID, "BUDGET", TEST_BUDGET_ID, "WORK_FOOD_EXCEEDED"))
                .thenReturn(false); // 초과 알림은 존재하지 않음

        // When
        budgetAlertService.checkAndNotifyBudgetAlert(
                TEST_USER_ID, TEST_WORKATION_ID, BudgetType.WORK, TEST_EXPENSE_CATEGORY_ID);

        // Then - 80% 알림은 중복으로 스킵, 초과 알림은 생성됨
        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService, times(1)).createNotification(eq(TEST_USER_ID), captor.capture());
        assertEquals("WORK_FOOD_EXCEEDED", captor.getValue().getNotificationType());
        assertTrue(captor.getValue().getImportant());
    }

    @Test
    @DisplayName("80% 알림 생성 후 초과 알림도 정상 생성")
    void checkAndNotifyBudgetAlert_80PercentThenExceeded_bothCreated() {
        // Given - 사용률 105%, 80% 알림도 없고 초과 알림도 없는 상태
        BudgetItemVO item = createBudgetItem(BudgetType.WORK, TEST_EXPENSE_CATEGORY_ID,
                BigDecimal.valueOf(100000), BigDecimal.valueOf(105000), "식비");

        when(budgetMapper.selectBudgetItemList(TEST_WORKATION_ID, TEST_USER_ID, BudgetType.WORK))
                .thenReturn(Collections.singletonList(item));
        when(budgetAlertMapper.existsNotificationByReference(
                TEST_USER_ID, "BUDGET", TEST_BUDGET_ID, "WORK_FOOD_80_PERCENT"))
                .thenReturn(false);
        when(budgetAlertMapper.existsNotificationByReference(
                TEST_USER_ID, "BUDGET", TEST_BUDGET_ID, "WORK_FOOD_EXCEEDED"))
                .thenReturn(false);

        // When
        budgetAlertService.checkAndNotifyBudgetAlert(
                TEST_USER_ID, TEST_WORKATION_ID, BudgetType.WORK, TEST_EXPENSE_CATEGORY_ID);

        // Then - 80% 알림과 초과 알림 모두 생성됨
        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService, times(2)).createNotification(eq(TEST_USER_ID), captor.capture());

        List<NotificationCreateRequestDTO> requests = captor.getAllValues();
        assertEquals("WORK_FOOD_80_PERCENT", requests.get(0).getNotificationType());
        assertEquals("WORK_FOOD_EXCEEDED", requests.get(1).getNotificationType());
    }

    @Test
    @DisplayName("80% 알림은 한 번만 생성")
    void checkAndNotifyBudgetAlert_80PercentAlert_createdOnlyOnce() {
        // Given - 사용률 85%, 80% 알림이 이미 존재
        BudgetItemVO item = createBudgetItem(BudgetType.WORK, TEST_EXPENSE_CATEGORY_ID,
                BigDecimal.valueOf(100000), BigDecimal.valueOf(85000), "식비");

        when(budgetMapper.selectBudgetItemList(TEST_WORKATION_ID, TEST_USER_ID, BudgetType.WORK))
                .thenReturn(Collections.singletonList(item));
        when(budgetAlertMapper.existsNotificationByReference(
                TEST_USER_ID, "BUDGET", TEST_BUDGET_ID, "WORK_FOOD_80_PERCENT"))
                .thenReturn(true);

        // When
        budgetAlertService.checkAndNotifyBudgetAlert(
                TEST_USER_ID, TEST_WORKATION_ID, BudgetType.WORK, TEST_EXPENSE_CATEGORY_ID);

        // Then - 80% 알림은 중복으로 인해 한 번도 생성되지 않음
        verify(notificationCreateService, never()).createNotification(any(), any());
    }

    @Test
    @DisplayName("초과 알림은 한 번만 생성")
    void checkAndNotifyBudgetAlert_exceededAlert_createdOnlyOnce() {
        // Given - 사용률 105%, 이미 80% 알림과 초과 알림 모두 존재
        BudgetItemVO item = createBudgetItem(BudgetType.WORK, TEST_EXPENSE_CATEGORY_ID,
                BigDecimal.valueOf(100000), BigDecimal.valueOf(105000), "식비");

        when(budgetMapper.selectBudgetItemList(TEST_WORKATION_ID, TEST_USER_ID, BudgetType.WORK))
                .thenReturn(Collections.singletonList(item));
        when(budgetAlertMapper.existsNotificationByReference(
                TEST_USER_ID, "BUDGET", TEST_BUDGET_ID, "WORK_FOOD_80_PERCENT"))
                .thenReturn(true);
        when(budgetAlertMapper.existsNotificationByReference(
                TEST_USER_ID, "BUDGET", TEST_BUDGET_ID, "WORK_FOOD_EXCEEDED"))
                .thenReturn(true);

        // When
        budgetAlertService.checkAndNotifyBudgetAlert(
                TEST_USER_ID, TEST_WORKATION_ID, BudgetType.WORK, TEST_EXPENSE_CATEGORY_ID);

        // Then - 초과 알림은 중복으로 인해 한 번도 생성되지 않음
        verify(notificationCreateService, never()).createNotification(any(), any());
    }

    // ================================================================
    // 4. 카테고리별 매핑 테스트
    // ================================================================

    @Test
    @DisplayName("법인 식비 → WORK_FOOD 알림 타입")
    void checkAndNotifyBudgetAlert_workFood_correctType() {
        BudgetItemVO item = createBudgetItem(BudgetType.WORK, TEST_EXPENSE_CATEGORY_ID,
                BigDecimal.valueOf(100000), BigDecimal.valueOf(80000), "식비");

        when(budgetMapper.selectBudgetItemList(TEST_WORKATION_ID, TEST_USER_ID, BudgetType.WORK))
                .thenReturn(Collections.singletonList(item));
        when(budgetAlertMapper.existsNotificationByReference(
                TEST_USER_ID, "BUDGET", TEST_BUDGET_ID, "WORK_FOOD_80_PERCENT"))
                .thenReturn(false);

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
                BigDecimal.valueOf(100000), BigDecimal.valueOf(80000), "식비");

        when(budgetMapper.selectBudgetItemList(TEST_WORKATION_ID, TEST_USER_ID, BudgetType.PERSONAL))
                .thenReturn(Collections.singletonList(item));
        when(budgetAlertMapper.existsNotificationByReference(
                TEST_USER_ID, "BUDGET", TEST_BUDGET_ID, "PERSONAL_FOOD_80_PERCENT"))
                .thenReturn(false);

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
                BigDecimal.valueOf(100000), BigDecimal.valueOf(85000), "숙박비");

        when(budgetMapper.selectBudgetItemList(TEST_WORKATION_ID, TEST_USER_ID, BudgetType.WORK))
                .thenReturn(Collections.singletonList(item));
        when(budgetAlertMapper.existsNotificationByReference(
                TEST_USER_ID, "BUDGET", TEST_BUDGET_ID, "WORK_ACCOMMODATION_80_PERCENT"))
                .thenReturn(false);

        budgetAlertService.checkAndNotifyBudgetAlert(
                TEST_USER_ID, TEST_WORKATION_ID, BudgetType.WORK, 1L);

        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());
        assertEquals("WORK_ACCOMMODATION_80_PERCENT", captor.getValue().getNotificationType());
    }

    @Test
    @DisplayName("개인 숙박비 → PERSONAL_ACCOMMODATION 알림 타입")
    void checkAndNotifyBudgetAlert_personalAccommodation_correctType() {
        BudgetItemVO item = createBudgetItem(BudgetType.PERSONAL, 13L,
                BigDecimal.valueOf(100000), BigDecimal.valueOf(85000), "숙박비");

        when(budgetMapper.selectBudgetItemList(TEST_WORKATION_ID, TEST_USER_ID, BudgetType.PERSONAL))
                .thenReturn(Collections.singletonList(item));
        when(budgetAlertMapper.existsNotificationByReference(
                TEST_USER_ID, "BUDGET", TEST_BUDGET_ID, "PERSONAL_ACCOMMODATION_80_PERCENT"))
                .thenReturn(false);

        budgetAlertService.checkAndNotifyBudgetAlert(
                TEST_USER_ID, TEST_WORKATION_ID, BudgetType.PERSONAL, 13L);

        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());
        assertEquals("PERSONAL_ACCOMMODATION_80_PERCENT", captor.getValue().getNotificationType());
    }

    // ================================================================
    // 5. 공통 서비스 연동 테스트
    // ================================================================

    @Test
    @DisplayName("BUDGET_NOTIFY 카테고리로 호출됨")
    void checkAndNotifyBudgetAlert_correctCategory() {
        BudgetItemVO item = createBudgetItem(BudgetType.WORK, TEST_EXPENSE_CATEGORY_ID,
                BigDecimal.valueOf(100000), BigDecimal.valueOf(80000), "식비");

        when(budgetMapper.selectBudgetItemList(TEST_WORKATION_ID, TEST_USER_ID, BudgetType.WORK))
                .thenReturn(Collections.singletonList(item));
        when(budgetAlertMapper.existsNotificationByReference(
                TEST_USER_ID, "BUDGET", TEST_BUDGET_ID, "WORK_FOOD_80_PERCENT"))
                .thenReturn(false);

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
                BigDecimal.valueOf(200000), BigDecimal.valueOf(170000), "식비");

        when(budgetMapper.selectBudgetItemList(TEST_WORKATION_ID, TEST_USER_ID, BudgetType.WORK))
                .thenReturn(Collections.singletonList(item));
        when(budgetAlertMapper.existsNotificationByReference(
                TEST_USER_ID, "BUDGET", TEST_BUDGET_ID, "WORK_FOOD_80_PERCENT"))
                .thenReturn(false);

        budgetAlertService.checkAndNotifyBudgetAlert(
                TEST_USER_ID, TEST_WORKATION_ID, BudgetType.WORK, TEST_EXPENSE_CATEGORY_ID);

        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());

        assertEquals(85, captor.getValue().getPlaceholders().get("usageRate")); // 170000/200000 = 85%
        assertEquals(170000L, captor.getValue().getPlaceholders().get("amount"));
    }

    @Test
    @DisplayName("referenceType=BUDGET, referenceId=예산ID로 호출됨")
    void checkAndNotifyBudgetAlert_referenceCorrect() {
        BudgetItemVO item = createBudgetItem(BudgetType.WORK, TEST_EXPENSE_CATEGORY_ID,
                BigDecimal.valueOf(100000), BigDecimal.valueOf(80000), "식비");

        when(budgetMapper.selectBudgetItemList(TEST_WORKATION_ID, TEST_USER_ID, BudgetType.WORK))
                .thenReturn(Collections.singletonList(item));
        when(budgetAlertMapper.existsNotificationByReference(
                TEST_USER_ID, "BUDGET", TEST_BUDGET_ID, "WORK_FOOD_80_PERCENT"))
                .thenReturn(false);

        budgetAlertService.checkAndNotifyBudgetAlert(
                TEST_USER_ID, TEST_WORKATION_ID, BudgetType.WORK, TEST_EXPENSE_CATEGORY_ID);

        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());

        assertEquals("BUDGET", captor.getValue().getReferenceType());
        assertEquals(TEST_BUDGET_ID, captor.getValue().getReferenceId());
    }

    // ================================================================
    // 6. 경계값 테스트
    // ================================================================

    @Test
    @DisplayName("80.9% 사용률 → 80% 알림 생성 (정수 내림)")
    void checkAndNotifyBudgetAlert_80Point9Percent_creates80Alert() {
        BudgetItemVO item = createBudgetItem(BudgetType.WORK, TEST_EXPENSE_CATEGORY_ID,
                BigDecimal.valueOf(100000), BigDecimal.valueOf(80900), "식비");

        when(budgetMapper.selectBudgetItemList(TEST_WORKATION_ID, TEST_USER_ID, BudgetType.WORK))
                .thenReturn(Collections.singletonList(item));
        when(budgetAlertMapper.existsNotificationByReference(
                TEST_USER_ID, "BUDGET", TEST_BUDGET_ID, "WORK_FOOD_80_PERCENT"))
                .thenReturn(false);

        budgetAlertService.checkAndNotifyBudgetAlert(
                TEST_USER_ID, TEST_WORKATION_ID, BudgetType.WORK, TEST_EXPENSE_CATEGORY_ID);

        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());
        assertEquals(80, captor.getValue().getPlaceholders().get("usageRate"));
    }

    @Test
    @DisplayName("사용 금액0일 때 알림 미생성")
    void checkAndNotifyBudgetAlert_zeroSpent_doesNotCreate() {
        BudgetItemVO item = createBudgetItem(BudgetType.WORK, TEST_EXPENSE_CATEGORY_ID,
                BigDecimal.valueOf(100000), BigDecimal.ZERO, "식비");

        when(budgetMapper.selectBudgetItemList(TEST_WORKATION_ID, TEST_USER_ID, BudgetType.WORK))
                .thenReturn(Collections.singletonList(item));

        budgetAlertService.checkAndNotifyBudgetAlert(
                TEST_USER_ID, TEST_WORKATION_ID, BudgetType.WORK, TEST_EXPENSE_CATEGORY_ID);

        verify(notificationCreateService, never()).createNotification(any(), any());
    }

    @Test
    @DisplayName("예산 배정 금액0일 때 알림 미생성")
    void checkAndNotifyBudgetAlert_zeroTarget_doesNotCreate() {
        BudgetItemVO item = createBudgetItem(BudgetType.WORK, TEST_EXPENSE_CATEGORY_ID,
                BigDecimal.ZERO, BigDecimal.ZERO, "식비");

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
    // 7. 중요 알림 플래그 테스트
    // ================================================================

    @Test
    @DisplayName("80% 알림은 중요 알림 아님")
    void checkAndNotifyBudgetAlert_80Percent_notImportant() {
        BudgetItemVO item = createBudgetItem(BudgetType.WORK, TEST_EXPENSE_CATEGORY_ID,
                BigDecimal.valueOf(100000), BigDecimal.valueOf(80000), "식비");

        when(budgetMapper.selectBudgetItemList(TEST_WORKATION_ID, TEST_USER_ID, BudgetType.WORK))
                .thenReturn(Collections.singletonList(item));
        when(budgetAlertMapper.existsNotificationByReference(
                TEST_USER_ID, "BUDGET", TEST_BUDGET_ID, "WORK_FOOD_80_PERCENT"))
                .thenReturn(false);

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
                BigDecimal.valueOf(100000), BigDecimal.valueOf(105000), "식비");

        when(budgetMapper.selectBudgetItemList(TEST_WORKATION_ID, TEST_USER_ID, BudgetType.WORK))
                .thenReturn(Collections.singletonList(item));
        when(budgetAlertMapper.existsNotificationByReference(
                TEST_USER_ID, "BUDGET", TEST_BUDGET_ID, "WORK_FOOD_80_PERCENT"))
                .thenReturn(false);
        when(budgetAlertMapper.existsNotificationByReference(
                TEST_USER_ID, "BUDGET", TEST_BUDGET_ID, "WORK_FOOD_EXCEEDED"))
                .thenReturn(false);

        budgetAlertService.checkAndNotifyBudgetAlert(
                TEST_USER_ID, TEST_WORKATION_ID, BudgetType.WORK, TEST_EXPENSE_CATEGORY_ID);

        // 초과 상황에서는 80% 알림 + 초과 알림 둘 다 생성됨
        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService, times(2)).createNotification(eq(TEST_USER_ID), captor.capture());

        // 두 번째 알림(초과)이 important = true인지 확인
        List<NotificationCreateRequestDTO> requests = captor.getAllValues();
        assertEquals("WORK_FOOD_EXCEEDED", requests.get(1).getNotificationType());
        assertTrue(requests.get(1).getImportant());
    }
}
