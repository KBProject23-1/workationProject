package com.workit.domain.notification.service;

import com.workit.domain.notification.dto.request.NotificationCreateRequestDTO;
import com.workit.domain.notification.enums.NotificationCategory;
import com.workit.domain.notification.exception.NotificationErrorCode;
import com.workit.domain.notification.mapper.NotificationMapper;
import com.workit.domain.notification.vo.NotificationSettingsVO;
import com.workit.domain.notification.vo.NotificationTemplateVO;
import com.workit.domain.notification.vo.NotificationVO;
import com.workit.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// NotificationCreateServiceImpl (공통 알림 생성 서비스) 테스트
// - NotificationMapper 를 Mockito @Mock 으로 주입한다 (Service 계층 검증에 집중)
@ExtendWith(MockitoExtension.class)
class NotificationCreateServiceImplTest {

    @Mock
    private NotificationMapper notificationMapper;

    private NotificationCreateServiceImpl notificationCreateService;

    private static final Long TEST_USER_ID = 100L;

    @BeforeEach
    void setUp() {
        notificationCreateService = new NotificationCreateServiceImpl(notificationMapper);
    }

    // ---------- 테스트 데이터 헬퍼 ----------

    /** 테스트용 알림 수신 설정 VO 생성 (모두 ON) */
    private NotificationSettingsVO createAllOnSettings() {
        NotificationSettingsVO vo = new NotificationSettingsVO();
        vo.setUserId(TEST_USER_ID);
        vo.setBudgetNotify(true);
        vo.setTransferNotify(true);
        vo.setPaymentNotify(true);
        vo.setWorkationNotify(true);
        vo.setSettlementNotify(true);
        vo.setScheduleNotify(true);
        return vo;
    }

    /** 테스트용 알림 수신 설정 VO 생성 (모두 OFF) */
    private NotificationSettingsVO createAllOffSettings() {
        NotificationSettingsVO vo = new NotificationSettingsVO();
        vo.setUserId(TEST_USER_ID);
        vo.setBudgetNotify(false);
        vo.setTransferNotify(false);
        vo.setPaymentNotify(false);
        vo.setWorkationNotify(false);
        vo.setSettlementNotify(false);
        vo.setScheduleNotify(false);
        return vo;
    }

    /** 테스트용 템플릿 VO 생성 */
    private NotificationTemplateVO createTemplate(String category, String notificationType,
                                                   String titleTemplate, String contentTemplate) {
        NotificationTemplateVO vo = new NotificationTemplateVO();
        vo.setId(1L);
        vo.setCategory(category);
        vo.setNotificationType(notificationType);
        vo.setTitleTemplate(titleTemplate);
        vo.setContentTemplate(contentTemplate);
        vo.setIsActive(true);
        return vo;
    }

    /** 테스트용 알림 생성 요청 빌더 */
    private NotificationCreateRequestDTO createRequest(NotificationCategory category, String notificationType,
                                                        Boolean important, Map<String, Object> placeholders) {
        return NotificationCreateRequestDTO.builder()
                .category(category)
                .notificationType(notificationType)
                .important(important)
                .placeholders(placeholders)
                .referenceType(null)
                .referenceId(null)
                .build();
    }

    // ================================================================
    // 0. 중복 알림 방지 테스트
    // ================================================================

    @Test
    @DisplayName("동일 reference + notificationType이 이미 존재하면 알림 미생성")
    void createNotification_duplicate_doesNotCreate() {
        // Given
        NotificationCreateRequestDTO request = NotificationCreateRequestDTO.builder()
                .category(NotificationCategory.TRANSFER_NOTIFY)
                .notificationType("WALLET_CHARGE_SUCCESS")
                .important(true)
                .referenceType("TRANSACTION")
                .referenceId(1L)
                .build();

        when(notificationMapper.existsNotificationByReference(
                TEST_USER_ID, "TRANSACTION", 1L, "WALLET_CHARGE_SUCCESS"))
                .thenReturn(true); // 이미 알림 존재

        // When
        notificationCreateService.createNotification(TEST_USER_ID, request);

        // Then - 중복으로 인해 알림 생성되지 않음
        verify(notificationMapper, never()).selectNotificationSettings(any());
        verify(notificationMapper, never()).insertNotificationHistory(any(), any());
    }

    @Test
    @DisplayName("중복이 아니면 알림 정상 생성")
    void createNotification_notDuplicate_createsNotification() {
        // Given
        NotificationSettingsVO settings = createAllOnSettings();
        NotificationTemplateVO template = createTemplate(
                "TRANSFER_NOTIFY", "WALLET_CHARGE_SUCCESS",
                "지갑 충전 완료",
                "{amount}원이 지갑에 충전되었습니다.");

        NotificationCreateRequestDTO request = NotificationCreateRequestDTO.builder()
                .category(NotificationCategory.TRANSFER_NOTIFY)
                .notificationType("WALLET_CHARGE_SUCCESS")
                .important(true)
                .referenceType("TRANSACTION")
                .referenceId(1L)
                .build();

        when(notificationMapper.existsNotificationByReference(
                TEST_USER_ID, "TRANSACTION", 1L, "WALLET_CHARGE_SUCCESS"))
                .thenReturn(false); // 중복 아님
        when(notificationMapper.selectNotificationSettings(TEST_USER_ID)).thenReturn(settings);
        when(notificationMapper.selectActiveTemplate("TRANSFER_NOTIFY", "WALLET_CHARGE_SUCCESS"))
                .thenReturn(template);
        when(notificationMapper.insertNotificationHistory(eq(TEST_USER_ID), any(NotificationVO.class)))
                .thenReturn(1);

        // When
        notificationCreateService.createNotification(TEST_USER_ID, request);

        // Then - 알림 정상 생성됨
        verify(notificationMapper).insertNotificationHistory(eq(TEST_USER_ID), any(NotificationVO.class));
    }

    @Test
    @DisplayName("referenceType이 null이면 중복 체크 스킵하고 알림 생성")
    void createNotification_nullReference_skipsDuplicateCheck() {
        // Given
        NotificationSettingsVO settings = createAllOnSettings();
        NotificationTemplateVO template = createTemplate(
                "BUDGET_NOTIFY", "WORK_FOOD_80_PERCENT",
                "예산 알림",
                "{usageRate}% 사용");

        NotificationCreateRequestDTO request = createRequest(
                NotificationCategory.BUDGET_NOTIFY, "WORK_FOOD_80_PERCENT", true, null);
        // referenceType, referenceId는 null (기본값)

        when(notificationMapper.selectNotificationSettings(TEST_USER_ID)).thenReturn(settings);
        when(notificationMapper.selectActiveTemplate("BUDGET_NOTIFY", "WORK_FOOD_80_PERCENT"))
                .thenReturn(template);
        when(notificationMapper.insertNotificationHistory(eq(TEST_USER_ID), any(NotificationVO.class)))
                .thenReturn(1);

        // When
        notificationCreateService.createNotification(TEST_USER_ID, request);

        // Then - 중복 체크 쿼리가 호출되지 않고 바로 알림 생성
        verify(notificationMapper, never()).existsNotificationByReference(any(), any(), any(), any());
        verify(notificationMapper).insertNotificationHistory(eq(TEST_USER_ID), any(NotificationVO.class));
    }

    // ================================================================
    // 1. 템플릿 조회 테스트
    // ================================================================

    @Test
    @DisplayName("활성화된 템플릿 정상 조회 - 공통 알림 생성 성공")
    void createNotification_success_activeTemplate() {
        // Given
        NotificationSettingsVO settings = createAllOnSettings();
        NotificationTemplateVO template = createTemplate(
                "BUDGET_NOTIFY", "WORK_FOOD_80_PERCENT",
                "법인 식비 예산 80% 소진",
                "법인 식비 예산의 {usageRate}%를 사용했어요. 현재 사용 금액은 {amount}원이에요.");

        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put("usageRate", 80);
        placeholders.put("amount", 800000);

        NotificationCreateRequestDTO request = createRequest(
                NotificationCategory.BUDGET_NOTIFY, "WORK_FOOD_80_PERCENT", true, placeholders);

        when(notificationMapper.selectNotificationSettings(TEST_USER_ID)).thenReturn(settings);
        when(notificationMapper.selectActiveTemplate("BUDGET_NOTIFY", "WORK_FOOD_80_PERCENT"))
                .thenReturn(template);
        when(notificationMapper.insertNotificationHistory(eq(TEST_USER_ID), any(NotificationVO.class)))
                .thenReturn(1);

        // When
        notificationCreateService.createNotification(TEST_USER_ID, request);

        // Then
        verify(notificationMapper).selectNotificationSettings(TEST_USER_ID);
        verify(notificationMapper).selectActiveTemplate("BUDGET_NOTIFY", "WORK_FOOD_80_PERCENT");
        verify(notificationMapper).insertNotificationHistory(eq(TEST_USER_ID), any(NotificationVO.class));
    }

    @Test
    @DisplayName("존재하지 않는 템플릿 예외 처리 - NOTIFICATION_TEMPLATE_NOT_FOUND")
    void createNotification_fail_templateNotFound() {
        // Given
        NotificationSettingsVO settings = createAllOnSettings();
        NotificationCreateRequestDTO request = createRequest(
                NotificationCategory.BUDGET_NOTIFY, "NONEXISTENT_TYPE", true, null);

        when(notificationMapper.selectNotificationSettings(TEST_USER_ID)).thenReturn(settings);
        when(notificationMapper.selectActiveTemplate("BUDGET_NOTIFY", "NONEXISTENT_TYPE"))
                .thenReturn(null);

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> notificationCreateService.createNotification(TEST_USER_ID, request));
        assertEquals(NotificationErrorCode.NOTIFICATION_TEMPLATE_NOT_FOUND, ex.getErrorCode());

        // 템플릿 조회 후 알림 저장은 호출되지 않음
        verify(notificationMapper, never()).insertNotificationHistory(any(), any());
    }

    @Test
    @DisplayName("비활성화된 템플릿 사용 불가 - selectActiveTemplate이 is_active=1 조건 포함")
    void createNotification_fail_inactiveTemplate() {
        // Given - selectActiveTemplate은 is_active=1 조건이 포함되므로 비활성 템플릿은 null 반환
        NotificationSettingsVO settings = createAllOnSettings();
        NotificationCreateRequestDTO request = createRequest(
                NotificationCategory.BUDGET_NOTIFY, "WORK_FOOD_80_PERCENT", true, null);

        when(notificationMapper.selectNotificationSettings(TEST_USER_ID)).thenReturn(settings);
        when(notificationMapper.selectActiveTemplate("BUDGET_NOTIFY", "WORK_FOOD_80_PERCENT"))
                .thenReturn(null);

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> notificationCreateService.createNotification(TEST_USER_ID, request));
        assertEquals(NotificationErrorCode.NOTIFICATION_TEMPLATE_NOT_FOUND, ex.getErrorCode());
    }

    // ================================================================
    // 2. placeholder 치환 테스트
    // ================================================================

    @Test
    @DisplayName("단일 placeholder 정상 치환")
    void createNotification_replaceSinglePlaceholder() {
        // Given
        NotificationSettingsVO settings = createAllOnSettings();
        NotificationTemplateVO template = createTemplate(
                "TRANSFER_NOTIFY", "WALLET_CHARGE_SUCCESS",
                "지갑 충전 완료",
                "{amount}원이 지갑에 충전되었습니다.");

        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put("amount", 50000);

        NotificationCreateRequestDTO request = createRequest(
                NotificationCategory.TRANSFER_NOTIFY, "WALLET_CHARGE_SUCCESS", false, placeholders);

        when(notificationMapper.selectNotificationSettings(TEST_USER_ID)).thenReturn(settings);
        when(notificationMapper.selectActiveTemplate("TRANSFER_NOTIFY", "WALLET_CHARGE_SUCCESS"))
                .thenReturn(template);

        ArgumentCaptor<NotificationVO> captor = ArgumentCaptor.forClass(NotificationVO.class);
        when(notificationMapper.insertNotificationHistory(eq(TEST_USER_ID), captor.capture()))
                .thenReturn(1);

        // When
        notificationCreateService.createNotification(TEST_USER_ID, request);

        // Then
        NotificationVO saved = captor.getValue();
        assertEquals("지갑 충전 완료", saved.getTitle());
        assertEquals("50000원이 지갑에 충전되었습니다.", saved.getContent());
    }

    @Test
    @DisplayName("여러 placeholder 정상 치환")
    void createNotification_replaceMultiplePlaceholders() {
        // Given
        NotificationSettingsVO settings = createAllOnSettings();
        NotificationTemplateVO template = createTemplate(
                "BUDGET_NOTIFY", "WORK_FOOD_80_PERCENT",
                "법인 식비 예산 80% 소진",
                "법인 식비 예산의 {usageRate}%를 사용했어요. 현재 사용 금액은 {amount}원이에요.");

        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put("usageRate", 80);
        placeholders.put("amount", 800000);

        NotificationCreateRequestDTO request = createRequest(
                NotificationCategory.BUDGET_NOTIFY, "WORK_FOOD_80_PERCENT", true, placeholders);

        when(notificationMapper.selectNotificationSettings(TEST_USER_ID)).thenReturn(settings);
        when(notificationMapper.selectActiveTemplate("BUDGET_NOTIFY", "WORK_FOOD_80_PERCENT"))
                .thenReturn(template);

        ArgumentCaptor<NotificationVO> captor = ArgumentCaptor.forClass(NotificationVO.class);
        when(notificationMapper.insertNotificationHistory(eq(TEST_USER_ID), captor.capture()))
                .thenReturn(1);

        // When
        notificationCreateService.createNotification(TEST_USER_ID, request);

        // Then
        NotificationVO saved = captor.getValue();
        assertEquals("법인 식비 예산 80% 소진", saved.getTitle());
        assertEquals("법인 식비 예산의 80%를 사용했어요. 현재 사용 금액은 800000원이에요.", saved.getContent());
    }

    @Test
    @DisplayName("title_template placeholder 정상 치환")
    void createNotification_replaceTitlePlaceholder() {
        // Given
        NotificationSettingsVO settings = createAllOnSettings();
        NotificationTemplateVO template = createTemplate(
                "PAYMENT_NOTIFY", "PAYMENT_SUCCESS",
                "{merchant} 결제 완료",
                "{merchant}에서 {amount}원 결제가 완료되었습니다.");

        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put("merchant", "스타벅스");
        placeholders.put("amount", 5500);

        NotificationCreateRequestDTO request = createRequest(
                NotificationCategory.PAYMENT_NOTIFY, "PAYMENT_SUCCESS", false, placeholders);

        when(notificationMapper.selectNotificationSettings(TEST_USER_ID)).thenReturn(settings);
        when(notificationMapper.selectActiveTemplate("PAYMENT_NOTIFY", "PAYMENT_SUCCESS"))
                .thenReturn(template);

        ArgumentCaptor<NotificationVO> captor = ArgumentCaptor.forClass(NotificationVO.class);
        when(notificationMapper.insertNotificationHistory(eq(TEST_USER_ID), captor.capture()))
                .thenReturn(1);

        // When
        notificationCreateService.createNotification(TEST_USER_ID, request);

        // Then
        NotificationVO saved = captor.getValue();
        assertEquals("스타벅스 결제 완료", saved.getTitle());
        assertEquals("스타벅스에서 5500원 결제가 완료되었습니다.", saved.getContent());
    }

    @Test
    @DisplayName("content_template placeholder 정상 치환")
    void createNotification_replaceContentPlaceholder() {
        // Given
        NotificationSettingsVO settings = createAllOnSettings();
        NotificationTemplateVO template = createTemplate(
                "SCHEDULE_NOTIFY", "SCHEDULE_D_MINUS_1_HOUR",
                "일정이 1시간 후 시작돼요",
                "{scheduleTitle} 일정이 {date}에 시작됩니다.");

        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put("scheduleTitle", "점심 회의");
        placeholders.put("date", "2026-08-22 12:00");

        NotificationCreateRequestDTO request = createRequest(
                NotificationCategory.SCHEDULE_NOTIFY, "SCHEDULE_D_MINUS_1_HOUR", false, placeholders);

        when(notificationMapper.selectNotificationSettings(TEST_USER_ID)).thenReturn(settings);
        when(notificationMapper.selectActiveTemplate("SCHEDULE_NOTIFY", "SCHEDULE_D_MINUS_1_HOUR"))
                .thenReturn(template);

        ArgumentCaptor<NotificationVO> captor = ArgumentCaptor.forClass(NotificationVO.class);
        when(notificationMapper.insertNotificationHistory(eq(TEST_USER_ID), captor.capture()))
                .thenReturn(1);

        // When
        notificationCreateService.createNotification(TEST_USER_ID, request);

        // Then
        NotificationVO saved = captor.getValue();
        assertEquals("일정이 1시간 후 시작돼요", saved.getTitle());
        assertEquals("점심 회의 일정이 2026-08-22 12:00에 시작됩니다.", saved.getContent());
    }

    @Test
    @DisplayName("placeholder가 없는 템플릿도 정상 처리")
    void createNotification_noPlaceholders() {
        // Given
        NotificationSettingsVO settings = createAllOnSettings();
        NotificationTemplateVO template = createTemplate(
                "WORKATION_NOTIFY", "WORKATION_START_D_MINUS_1",
                "워케이션 시작 예정",
                "내일 {date}에 워케이션이 시작됩니다. 일정을 확인해 주세요.");

        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put("date", "2026-08-22");

        NotificationCreateRequestDTO request = createRequest(
                NotificationCategory.WORKATION_NOTIFY, "WORKATION_START_D_MINUS_1", true, placeholders);

        when(notificationMapper.selectNotificationSettings(TEST_USER_ID)).thenReturn(settings);
        when(notificationMapper.selectActiveTemplate("WORKATION_NOTIFY", "WORKATION_START_D_MINUS_1"))
                .thenReturn(template);

        ArgumentCaptor<NotificationVO> captor = ArgumentCaptor.forClass(NotificationVO.class);
        when(notificationMapper.insertNotificationHistory(eq(TEST_USER_ID), captor.capture()))
                .thenReturn(1);

        // When
        notificationCreateService.createNotification(TEST_USER_ID, request);

        // Then
        NotificationVO saved = captor.getValue();
        assertEquals("워케이션 시작 예정", saved.getTitle());
        assertEquals("내일 2026-08-22에 워케이션이 시작됩니다. 일정을 확인해 주세요.", saved.getContent());
    }

    @Test
    @DisplayName("전달된 placeholder 값이 숫자인 경우 정상 치환")
    void createNotification_numericPlaceholderValues() {
        // Given
        NotificationSettingsVO settings = createAllOnSettings();
        NotificationTemplateVO template = createTemplate(
                "SETTLEMENT_NOTIFY", "SETTLEMENT_OVERDUE",
                "정산이 지연되고 있어요",
                "확인하지 않은 지출이 {unconfirmedCount}건 있어요.");

        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put("unconfirmedCount", 5);

        NotificationCreateRequestDTO request = createRequest(
                NotificationCategory.SETTLEMENT_NOTIFY, "SETTLEMENT_OVERDUE", true, placeholders);

        when(notificationMapper.selectNotificationSettings(TEST_USER_ID)).thenReturn(settings);
        when(notificationMapper.selectActiveTemplate("SETTLEMENT_NOTIFY", "SETTLEMENT_OVERDUE"))
                .thenReturn(template);

        ArgumentCaptor<NotificationVO> captor = ArgumentCaptor.forClass(NotificationVO.class);
        when(notificationMapper.insertNotificationHistory(eq(TEST_USER_ID), captor.capture()))
                .thenReturn(1);

        // When
        notificationCreateService.createNotification(TEST_USER_ID, request);

        // Then
        NotificationVO saved = captor.getValue();
        assertEquals("정산이 지연되고 있어요", saved.getTitle());
        assertEquals("확인하지 않은 지출이 5건 있어요.", saved.getContent());
    }

    @Test
    @DisplayName("placeholder가 null인 경우 빈 문자열로 치환")
    void createNotification_nullPlaceholderValue() {
        // Given
        NotificationSettingsVO settings = createAllOnSettings();
        NotificationTemplateVO template = createTemplate(
                "PAYMENT_NOTIFY", "PAYMENT_SUCCESS",
                "결제 완료",
                "{merchant}에서 {amount}원 결제가 완료되었습니다.");

        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put("merchant", "스타벅스");
        placeholders.put("amount", null);

        NotificationCreateRequestDTO request = createRequest(
                NotificationCategory.PAYMENT_NOTIFY, "PAYMENT_SUCCESS", false, placeholders);

        when(notificationMapper.selectNotificationSettings(TEST_USER_ID)).thenReturn(settings);
        when(notificationMapper.selectActiveTemplate("PAYMENT_NOTIFY", "PAYMENT_SUCCESS"))
                .thenReturn(template);

        ArgumentCaptor<NotificationVO> captor = ArgumentCaptor.forClass(NotificationVO.class);
        when(notificationMapper.insertNotificationHistory(eq(TEST_USER_ID), captor.capture()))
                .thenReturn(1);

        // When
        notificationCreateService.createNotification(TEST_USER_ID, request);

        // Then
        NotificationVO saved = captor.getValue();
        assertEquals("스타벅스에서 원 결제가 완료되었습니다.", saved.getContent());
    }

    // ================================================================
    // 3. 알림 수신 설정 테스트
    // ================================================================

    @Test
    @DisplayName("수신 설정 ON → notification_histories 저장")
    void createNotification_settingsOn_savesNotification() {
        // Given
        NotificationSettingsVO settings = createAllOnSettings();
        NotificationTemplateVO template = createTemplate(
                "BUDGET_NOTIFY", "WORK_FOOD_80_PERCENT",
                "예산 80% 소진",
                "{usageRate}% 사용");

        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put("usageRate", 80);

        NotificationCreateRequestDTO request = createRequest(
                NotificationCategory.BUDGET_NOTIFY, "WORK_FOOD_80_PERCENT", true, placeholders);

        when(notificationMapper.selectNotificationSettings(TEST_USER_ID)).thenReturn(settings);
        when(notificationMapper.selectActiveTemplate("BUDGET_NOTIFY", "WORK_FOOD_80_PERCENT"))
                .thenReturn(template);
        when(notificationMapper.insertNotificationHistory(eq(TEST_USER_ID), any(NotificationVO.class)))
                .thenReturn(1);

        // When
        notificationCreateService.createNotification(TEST_USER_ID, request);

        // Then - 알림 저장이 호출됨
        verify(notificationMapper).insertNotificationHistory(eq(TEST_USER_ID), any(NotificationVO.class));
    }

    @Test
    @DisplayName("수신 설정 OFF → notification_histories 저장하지 않음")
    void createNotification_settingsOff_doesNotSave() {
        // Given
        NotificationSettingsVO settings = createAllOffSettings();
        NotificationCreateRequestDTO request = createRequest(
                NotificationCategory.BUDGET_NOTIFY, "WORK_FOOD_80_PERCENT", true, null);

        when(notificationMapper.selectNotificationSettings(TEST_USER_ID)).thenReturn(settings);

        // When
        notificationCreateService.createNotification(TEST_USER_ID, request);

        // Then - 알림 저장이 호출되지 않음
        verify(notificationMapper, never()).selectActiveTemplate(any(), any());
        verify(notificationMapper, never()).insertNotificationHistory(any(), any());
    }

    @Test
    @DisplayName("BUDGET_NOTIFY 카테고리 OFF → 해당 카테고리 알림 미저장")
    void createNotification_budgetCategoryOff() {
        // Given
        NotificationSettingsVO settings = createAllOnSettings();
        settings.setBudgetNotify(false);

        NotificationCreateRequestDTO request = createRequest(
                NotificationCategory.BUDGET_NOTIFY, "WORK_FOOD_80_PERCENT", true, null);

        when(notificationMapper.selectNotificationSettings(TEST_USER_ID)).thenReturn(settings);

        // When
        notificationCreateService.createNotification(TEST_USER_ID, request);

        // Then
        verify(notificationMapper, never()).selectActiveTemplate(any(), any());
        verify(notificationMapper, never()).insertNotificationHistory(any(), any());
    }

    @Test
    @DisplayName("TRANSFER_NOTIFY 카테고리 OFF → 해당 카테고리 알림 미저장")
    void createNotification_transferCategoryOff() {
        // Given
        NotificationSettingsVO settings = createAllOnSettings();
        settings.setTransferNotify(false);

        NotificationCreateRequestDTO request = createRequest(
                NotificationCategory.TRANSFER_NOTIFY, "WALLET_CHARGE_SUCCESS", false, null);

        when(notificationMapper.selectNotificationSettings(TEST_USER_ID)).thenReturn(settings);

        // When
        notificationCreateService.createNotification(TEST_USER_ID, request);

        // Then
        verify(notificationMapper, never()).selectActiveTemplate(any(), any());
        verify(notificationMapper, never()).insertNotificationHistory(any(), any());
    }

    @Test
    @DisplayName("PAYMENT_NOTIFY 카테고리 OFF → 해당 카테고리 알림 미저장")
    void createNotification_paymentCategoryOff() {
        // Given
        NotificationSettingsVO settings = createAllOnSettings();
        settings.setPaymentNotify(false);

        NotificationCreateRequestDTO request = createRequest(
                NotificationCategory.PAYMENT_NOTIFY, "PAYMENT_SUCCESS", false, null);

        when(notificationMapper.selectNotificationSettings(TEST_USER_ID)).thenReturn(settings);

        // When
        notificationCreateService.createNotification(TEST_USER_ID, request);

        // Then
        verify(notificationMapper, never()).selectActiveTemplate(any(), any());
        verify(notificationMapper, never()).insertNotificationHistory(any(), any());
    }

    @Test
    @DisplayName("WORKATION_NOTIFY 카테고리 OFF → 해당 카테고리 알림 미저장")
    void createNotification_workationCategoryOff() {
        // Given
        NotificationSettingsVO settings = createAllOnSettings();
        settings.setWorkationNotify(false);

        NotificationCreateRequestDTO request = createRequest(
                NotificationCategory.WORKATION_NOTIFY, "WORKATION_START_D_MINUS_1", true, null);

        when(notificationMapper.selectNotificationSettings(TEST_USER_ID)).thenReturn(settings);

        // When
        notificationCreateService.createNotification(TEST_USER_ID, request);

        // Then
        verify(notificationMapper, never()).selectActiveTemplate(any(), any());
        verify(notificationMapper, never()).insertNotificationHistory(any(), any());
    }

    @Test
    @DisplayName("SETTLEMENT_NOTIFY 카테고리 OFF → 해당 카테고리 알림 미저장")
    void createNotification_settlementCategoryOff() {
        // Given
        NotificationSettingsVO settings = createAllOnSettings();
        settings.setSettlementNotify(false);

        NotificationCreateRequestDTO request = createRequest(
                NotificationCategory.SETTLEMENT_NOTIFY, "SETTLEMENT_OVERDUE", true, null);

        when(notificationMapper.selectNotificationSettings(TEST_USER_ID)).thenReturn(settings);

        // When
        notificationCreateService.createNotification(TEST_USER_ID, request);

        // Then
        verify(notificationMapper, never()).selectActiveTemplate(any(), any());
        verify(notificationMapper, never()).insertNotificationHistory(any(), any());
    }

    @Test
    @DisplayName("SCHEDULE_NOTIFY 카테고리 OFF → 해당 카테고리 알림 미저장")
    void createNotification_scheduleCategoryOff() {
        // Given
        NotificationSettingsVO settings = createAllOnSettings();
        settings.setScheduleNotify(false);

        NotificationCreateRequestDTO request = createRequest(
                NotificationCategory.SCHEDULE_NOTIFY, "SCHEDULE_D_MINUS_1_HOUR", false, null);

        when(notificationMapper.selectNotificationSettings(TEST_USER_ID)).thenReturn(settings);

        // When
        notificationCreateService.createNotification(TEST_USER_ID, request);

        // Then
        verify(notificationMapper, never()).selectActiveTemplate(any(), any());
        verify(notificationMapper, never()).insertNotificationHistory(any(), any());
    }

    @Test
    @DisplayName("수신 설정이 없는 경우(방어적 처리) → 알림 저장")
    void createNotification_noSettings_savesNotification() {
        // Given - 설정이 null인 경우 (방어적 처리)
        NotificationTemplateVO template = createTemplate(
                "BUDGET_NOTIFY", "WORK_FOOD_80_PERCENT",
                "예산 80% 소진",
                "{usageRate}% 사용");

        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put("usageRate", 80);

        NotificationCreateRequestDTO request = createRequest(
                NotificationCategory.BUDGET_NOTIFY, "WORK_FOOD_80_PERCENT", true, placeholders);

        when(notificationMapper.selectNotificationSettings(TEST_USER_ID)).thenReturn(null);
        when(notificationMapper.selectActiveTemplate("BUDGET_NOTIFY", "WORK_FOOD_80_PERCENT"))
                .thenReturn(template);
        when(notificationMapper.insertNotificationHistory(eq(TEST_USER_ID), any(NotificationVO.class)))
                .thenReturn(1);

        // When
        notificationCreateService.createNotification(TEST_USER_ID, request);

        // Then - 설정이 없어도 알림이 저장됨 (방어적 처리)
        verify(notificationMapper).insertNotificationHistory(eq(TEST_USER_ID), any(NotificationVO.class));
    }

    // ================================================================
    // 4. 알림 저장 데이터 검증 테스트
    // ================================================================

    @Test
    @DisplayName("userId 정상 저장")
    void createNotification_saveUserId() {
        // Given
        NotificationSettingsVO settings = createAllOnSettings();
        NotificationTemplateVO template = createTemplate(
                "TRANSFER_NOTIFY", "WALLET_CHARGE_SUCCESS",
                "충전 완료",
                "{amount}원 충전");

        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put("amount", 10000);

        NotificationCreateRequestDTO request = createRequest(
                NotificationCategory.TRANSFER_NOTIFY, "WALLET_CHARGE_SUCCESS", false, placeholders);

        when(notificationMapper.selectNotificationSettings(TEST_USER_ID)).thenReturn(settings);
        when(notificationMapper.selectActiveTemplate("TRANSFER_NOTIFY", "WALLET_CHARGE_SUCCESS"))
                .thenReturn(template);

        ArgumentCaptor<NotificationVO> captor = ArgumentCaptor.forClass(NotificationVO.class);
        when(notificationMapper.insertNotificationHistory(eq(TEST_USER_ID), captor.capture()))
                .thenReturn(1);

        // When
        notificationCreateService.createNotification(TEST_USER_ID, request);

        // Then - userId가 Mapper에 전달됨
        verify(notificationMapper).insertNotificationHistory(eq(TEST_USER_ID), captor.capture());
    }

    @Test
    @DisplayName("category 정상 저장")
    void createNotification_saveCategory() {
        // Given
        NotificationSettingsVO settings = createAllOnSettings();
        NotificationTemplateVO template = createTemplate(
                "PAYMENT_NOTIFY", "PAYMENT_SUCCESS",
                "결제 완료",
                "결제 완료");

        NotificationCreateRequestDTO request = createRequest(
                NotificationCategory.PAYMENT_NOTIFY, "PAYMENT_SUCCESS", false, null);

        when(notificationMapper.selectNotificationSettings(TEST_USER_ID)).thenReturn(settings);
        when(notificationMapper.selectActiveTemplate("PAYMENT_NOTIFY", "PAYMENT_SUCCESS"))
                .thenReturn(template);

        ArgumentCaptor<NotificationVO> captor = ArgumentCaptor.forClass(NotificationVO.class);
        when(notificationMapper.insertNotificationHistory(eq(TEST_USER_ID), captor.capture()))
                .thenReturn(1);

        // When
        notificationCreateService.createNotification(TEST_USER_ID, request);

        // Then
        NotificationVO saved = captor.getValue();
        assertEquals("PAYMENT_NOTIFY", saved.getCategory());
    }

    @Test
    @DisplayName("important 정상 저장")
    void createNotification_saveImportant() {
        // Given
        NotificationSettingsVO settings = createAllOnSettings();
        NotificationTemplateVO template = createTemplate(
                "BUDGET_NOTIFY", "WORK_FOOD_EXCEEDED",
                "예산 초과",
                "예산 초과");

        NotificationCreateRequestDTO request = createRequest(
                NotificationCategory.BUDGET_NOTIFY, "WORK_FOOD_EXCEEDED", true, null);

        when(notificationMapper.selectNotificationSettings(TEST_USER_ID)).thenReturn(settings);
        when(notificationMapper.selectActiveTemplate("BUDGET_NOTIFY", "WORK_FOOD_EXCEEDED"))
                .thenReturn(template);

        ArgumentCaptor<NotificationVO> captor = ArgumentCaptor.forClass(NotificationVO.class);
        when(notificationMapper.insertNotificationHistory(eq(TEST_USER_ID), captor.capture()))
                .thenReturn(1);

        // When
        notificationCreateService.createNotification(TEST_USER_ID, request);

        // Then
        NotificationVO saved = captor.getValue();
        assertEquals(true, saved.getImportant());
    }

    @Test
    @DisplayName("최종 title 정상 저장")
    void createNotification_saveTitle() {
        // Given
        NotificationSettingsVO settings = createAllOnSettings();
        NotificationTemplateVO template = createTemplate(
                "WORKATION_NOTIFY", "WORKATION_START_D_MINUS_1",
                "워케이션 시작 예정",
                "내일 시작");

        NotificationCreateRequestDTO request = createRequest(
                NotificationCategory.WORKATION_NOTIFY, "WORKATION_START_D_MINUS_1", true, null);

        when(notificationMapper.selectNotificationSettings(TEST_USER_ID)).thenReturn(settings);
        when(notificationMapper.selectActiveTemplate("WORKATION_NOTIFY", "WORKATION_START_D_MINUS_1"))
                .thenReturn(template);

        ArgumentCaptor<NotificationVO> captor = ArgumentCaptor.forClass(NotificationVO.class);
        when(notificationMapper.insertNotificationHistory(eq(TEST_USER_ID), captor.capture()))
                .thenReturn(1);

        // When
        notificationCreateService.createNotification(TEST_USER_ID, request);

        // Then
        NotificationVO saved = captor.getValue();
        assertEquals("워케이션 시작 예정", saved.getTitle());
    }

    @Test
    @DisplayName("최종 content 정상 저장 (placeholder 치환 후)")
    void createNotification_saveContent() {
        // Given
        NotificationSettingsVO settings = createAllOnSettings();
        NotificationTemplateVO template = createTemplate(
                "SETTLEMENT_NOTIFY", "UNCONFIRMED_EXPENSE_OVER_3",
                "미확인 지출",
                "확인하지 않은 지출이 {unconfirmedCount}건 있습니다.");

        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put("unconfirmedCount", 3);

        NotificationCreateRequestDTO request = createRequest(
                NotificationCategory.SETTLEMENT_NOTIFY, "UNCONFIRMED_EXPENSE_OVER_3", false, placeholders);

        when(notificationMapper.selectNotificationSettings(TEST_USER_ID)).thenReturn(settings);
        when(notificationMapper.selectActiveTemplate("SETTLEMENT_NOTIFY", "UNCONFIRMED_EXPENSE_OVER_3"))
                .thenReturn(template);

        ArgumentCaptor<NotificationVO> captor = ArgumentCaptor.forClass(NotificationVO.class);
        when(notificationMapper.insertNotificationHistory(eq(TEST_USER_ID), captor.capture()))
                .thenReturn(1);

        // When
        notificationCreateService.createNotification(TEST_USER_ID, request);

        // Then
        NotificationVO saved = captor.getValue();
        assertEquals("확인하지 않은 지출이 3건 있습니다.", saved.getContent());
    }

    @Test
    @DisplayName("referenceType 정상 저장")
    void createNotification_saveReferenceType() {
        // Given
        NotificationSettingsVO settings = createAllOnSettings();
        NotificationTemplateVO template = createTemplate(
                "WORKATION_NOTIFY", "WORKATION_START_D_MINUS_1",
                "워케이션 시작", "시작");

        NotificationCreateRequestDTO request = NotificationCreateRequestDTO.builder()
                .category(NotificationCategory.WORKATION_NOTIFY)
                .notificationType("WORKATION_START_D_MINUS_1")
                .important(true)
                .referenceType("WORKATION")
                .referenceId(123L)
                .build();

        when(notificationMapper.selectNotificationSettings(TEST_USER_ID)).thenReturn(settings);
        when(notificationMapper.selectActiveTemplate("WORKATION_NOTIFY", "WORKATION_START_D_MINUS_1"))
                .thenReturn(template);

        ArgumentCaptor<NotificationVO> captor = ArgumentCaptor.forClass(NotificationVO.class);
        when(notificationMapper.insertNotificationHistory(eq(TEST_USER_ID), captor.capture()))
                .thenReturn(1);

        // When
        notificationCreateService.createNotification(TEST_USER_ID, request);

        // Then
        NotificationVO saved = captor.getValue();
        assertEquals("WORKATION", saved.getReferenceType());
    }

    @Test
    @DisplayName("referenceId 정상 저장")
    void createNotification_saveReferenceId() {
        // Given
        NotificationSettingsVO settings = createAllOnSettings();
        NotificationTemplateVO template = createTemplate(
                "WORKATION_NOTIFY", "WORKATION_START_D_MINUS_1",
                "워케이션 시작", "시작");

        NotificationCreateRequestDTO request = NotificationCreateRequestDTO.builder()
                .category(NotificationCategory.WORKATION_NOTIFY)
                .notificationType("WORKATION_START_D_MINUS_1")
                .important(true)
                .referenceType("WORKATION")
                .referenceId(456L)
                .build();

        when(notificationMapper.selectNotificationSettings(TEST_USER_ID)).thenReturn(settings);
        when(notificationMapper.selectActiveTemplate("WORKATION_NOTIFY", "WORKATION_START_D_MINUS_1"))
                .thenReturn(template);

        ArgumentCaptor<NotificationVO> captor = ArgumentCaptor.forClass(NotificationVO.class);
        when(notificationMapper.insertNotificationHistory(eq(TEST_USER_ID), captor.capture()))
                .thenReturn(1);

        // When
        notificationCreateService.createNotification(TEST_USER_ID, request);

        // Then
        NotificationVO saved = captor.getValue();
        assertEquals(456L, saved.getReferenceId());
    }

    @Test
    @DisplayName("referenceType/referenceId가 null인 경우 정상 저장")
    void createNotification_nullReference() {
        // Given
        NotificationSettingsVO settings = createAllOnSettings();
        NotificationTemplateVO template = createTemplate(
                "TRANSFER_NOTIFY", "WALLET_CHARGE_SUCCESS",
                "충전 완료", "충전 완료");

        NotificationCreateRequestDTO request = createRequest(
                NotificationCategory.TRANSFER_NOTIFY, "WALLET_CHARGE_SUCCESS", false, null);

        when(notificationMapper.selectNotificationSettings(TEST_USER_ID)).thenReturn(settings);
        when(notificationMapper.selectActiveTemplate("TRANSFER_NOTIFY", "WALLET_CHARGE_SUCCESS"))
                .thenReturn(template);

        ArgumentCaptor<NotificationVO> captor = ArgumentCaptor.forClass(NotificationVO.class);
        when(notificationMapper.insertNotificationHistory(eq(TEST_USER_ID), captor.capture()))
                .thenReturn(1);

        // When
        notificationCreateService.createNotification(TEST_USER_ID, request);

        // Then
        NotificationVO saved = captor.getValue();
        assertEquals(null, saved.getReferenceType());
        assertEquals(null, saved.getReferenceId());
    }

    @Test
    @DisplayName("read = 0으로 정상 저장됨")
    void createNotification_readDefaultZero() {
        // Given
        NotificationSettingsVO settings = createAllOnSettings();
        NotificationTemplateVO template = createTemplate(
                "TRANSFER_NOTIFY", "WALLET_CHARGE_SUCCESS",
                "충전 완료", "충전 완료");

        NotificationCreateRequestDTO request = createRequest(
                NotificationCategory.TRANSFER_NOTIFY, "WALLET_CHARGE_SUCCESS", false, null);

        when(notificationMapper.selectNotificationSettings(TEST_USER_ID)).thenReturn(settings);
        when(notificationMapper.selectActiveTemplate("TRANSFER_NOTIFY", "WALLET_CHARGE_SUCCESS"))
                .thenReturn(template);

        ArgumentCaptor<NotificationVO> captor = ArgumentCaptor.forClass(NotificationVO.class);
        when(notificationMapper.insertNotificationHistory(eq(TEST_USER_ID), captor.capture()))
                .thenReturn(1);

        // When
        notificationCreateService.createNotification(TEST_USER_ID, request);

        // Then - read는 DB에서 DEFAULT 0으로 저장되므로 VO에는 null (설정하지 않음)
        NotificationVO saved = captor.getValue();
        assertEquals(null, saved.getIsRead()); // VO에는 설정하지 않음, DB에서 DEFAULT 0 적용
    }

    // ================================================================
    // 5. 통합 흐름 테스트
    // ================================================================

    @Test
    @DisplayName("통합 흐름 - 수신 설정 확인 → 활성 템플릿 조회 → placeholder 치환 → notification_histories 저장")
    void createNotification_fullFlow() {
        // Given
        NotificationSettingsVO settings = createAllOnSettings();
        NotificationTemplateVO template = createTemplate(
                "BUDGET_NOTIFY", "WORK_FOOD_80_PERCENT",
                "법인 식비 예산 80% 소진",
                "법인 식비 예산의 {usageRate}%를 사용했어요. 현재 사용 금액은 {amount}원이에요.");

        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put("usageRate", 80);
        placeholders.put("amount", 800000);

        NotificationCreateRequestDTO request = NotificationCreateRequestDTO.builder()
                .category(NotificationCategory.BUDGET_NOTIFY)
                .notificationType("WORK_FOOD_80_PERCENT")
                .important(true)
                .placeholders(placeholders)
                .referenceType("BUDGET")
                .referenceId(789L)
                .build();

        when(notificationMapper.selectNotificationSettings(TEST_USER_ID)).thenReturn(settings);
        when(notificationMapper.selectActiveTemplate("BUDGET_NOTIFY", "WORK_FOOD_80_PERCENT"))
                .thenReturn(template);

        ArgumentCaptor<NotificationVO> captor = ArgumentCaptor.forClass(NotificationVO.class);
        when(notificationMapper.insertNotificationHistory(eq(TEST_USER_ID), captor.capture()))
                .thenReturn(1);

        // When
        notificationCreateService.createNotification(TEST_USER_ID, request);

        // Then
        // 1. 수신 설정 조회 확인
        verify(notificationMapper).selectNotificationSettings(TEST_USER_ID);

        // 2. 템플릿 조회 확인
        verify(notificationMapper).selectActiveTemplate("BUDGET_NOTIFY", "WORK_FOOD_80_PERCENT");

        // 3. 알림 저장 확인 (captor로 저장 데이터 검증)
        verify(notificationMapper).insertNotificationHistory(eq(TEST_USER_ID), captor.capture());
        NotificationVO saved = captor.getValue();

        assertEquals("BUDGET_NOTIFY", saved.getCategory());
        assertEquals(true, saved.getImportant());
        assertEquals("법인 식비 예산 80% 소진", saved.getTitle());
        assertEquals("법인 식비 예산의 80%를 사용했어요. 현재 사용 금액은 800000원이에요.", saved.getContent());
        assertEquals("BUDGET", saved.getReferenceType());
        assertEquals(789L, saved.getReferenceId());
    }

    @Test
    @DisplayName("통합 흐름 - 수신 설정 OFF → 알림 미저장")
    void createNotification_fullFlow_settingsOff() {
        // Given
        NotificationSettingsVO settings = createAllOffSettings();
        NotificationCreateRequestDTO request = createRequest(
                NotificationCategory.BUDGET_NOTIFY, "WORK_FOOD_80_PERCENT", true, null);

        when(notificationMapper.selectNotificationSettings(TEST_USER_ID)).thenReturn(settings);

        // When
        notificationCreateService.createNotification(TEST_USER_ID, request);

        // Then - 수신 설정 확인 후 알림 미저장
        verify(notificationMapper).selectNotificationSettings(TEST_USER_ID);
        verify(notificationMapper, never()).selectActiveTemplate(any(), any());
        verify(notificationMapper, never()).insertNotificationHistory(any(), any());
    }
}
