package com.workit.domain.payment.service;

import com.workit.domain.notification.dto.request.NotificationCreateRequestDTO;
import com.workit.domain.notification.enums.NotificationCategory;
import com.workit.domain.notification.service.NotificationCreateService;
import com.workit.domain.payment.mapper.PaymentAlertMapper;
import com.workit.domain.transaction.dto.response.PaymentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// PaymentAlertServiceImpl 테스트
// - PAYMENT_SUCCESS 알림, REFUND_SUCCESS 알림, 중복 방지 등을 검증한다
@ExtendWith(MockitoExtension.class)
class PaymentAlertServiceImplTest {

    @Mock
    private PaymentAlertMapper paymentAlertMapper;

    @Mock
    private NotificationCreateService notificationCreateService;

    private PaymentAlertServiceImpl paymentAlertService;

    private static final Long TEST_USER_ID = 100L;
    private static final Long TEST_TRANSACTION_ID = 1L;
    private static final String REFERENCE_TYPE = "TRANSACTION";
    private static final String NOTIFICATION_TYPE_PAYMENT_SUCCESS = "PAYMENT_SUCCESS";
    private static final String NOTIFICATION_TYPE_REFUND_SUCCESS = "REFUND_SUCCESS";

    @BeforeEach
    void setUp() {
        paymentAlertService = new PaymentAlertServiceImpl(
                paymentAlertMapper, notificationCreateService);
    }

    // ---------- 테스트 데이터 헬퍼 ----------

    /** 결제 완료 응답 생성 */
    private PaymentResponse createPaymentResponse(Long transactionId, String merchantName,
                                                   BigDecimal amount) {
        PaymentResponse response = new PaymentResponse();
        response.setTransactionId(transactionId);
        response.setMerchantName(merchantName);
        response.setAmount(amount);
        response.setTransactionType("PAYMENT");
        response.setPaymentSourceType("WALLET");
        response.setStatus("PAID");
        return response;
    }

    /** 결제가 아닌 거래 응답 생성 (DEPOSIT 등) */
    private PaymentResponse createNonPaymentResponse(Long transactionId) {
        PaymentResponse response = new PaymentResponse();
        response.setTransactionId(transactionId);
        response.setMerchantName("지갑 충전");
        response.setAmount(BigDecimal.valueOf(10000));
        response.setTransactionType("DEPOSIT");
        response.setPaymentSourceType("WALLET");
        response.setStatus("PAID");
        return response;
    }

    // ================================================================
    // 1. PAYMENT_SUCCESS 알림 테스트
    // ================================================================

    @Test
    @DisplayName("결제 성공 → PAYMENT_SUCCESS 알림 생성")
    void notifyPaymentSuccess_payment_createsNotification() {
        // Given
        PaymentResponse response = createPaymentResponse(TEST_TRANSACTION_ID, "테스트 가맹점",
                BigDecimal.valueOf(50000));

        when(paymentAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_TRANSACTION_ID, NOTIFICATION_TYPE_PAYMENT_SUCCESS))
                .thenReturn(false);

        // When
        paymentAlertService.notifyPaymentSuccess(TEST_USER_ID, response);

        // Then
        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());

        NotificationCreateRequestDTO request = captor.getValue();
        assertEquals(NotificationCategory.PAYMENT_NOTIFY, request.getCategory());
        assertEquals(NOTIFICATION_TYPE_PAYMENT_SUCCESS, request.getNotificationType());
        assertEquals(REFERENCE_TYPE, request.getReferenceType());
        assertEquals(TEST_TRANSACTION_ID, request.getReferenceId());
        assertTrue(request.getImportant());
    }

    @Test
    @DisplayName("PAYMENT_SUCCESS - {merchant} placeholder에 가맹점명이 전달됨")
    void notifyPaymentSuccess_merchantPlaceholder_correct() {
        // Given
        PaymentResponse response = createPaymentResponse(TEST_TRANSACTION_ID, "스타벅스 강남점",
                BigDecimal.valueOf(5500));

        when(paymentAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_TRANSACTION_ID, NOTIFICATION_TYPE_PAYMENT_SUCCESS))
                .thenReturn(false);

        // When
        paymentAlertService.notifyPaymentSuccess(TEST_USER_ID, response);

        // Then
        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());
        assertEquals("스타벅스 강남점", captor.getValue().getPlaceholders().get("merchant"));
    }

    @Test
    @DisplayName("PAYMENT_SUCCESS - {amount} placeholder에 결제 금액이 전달됨")
    void notifyPaymentSuccess_amountPlaceholder_correct() {
        // Given
        PaymentResponse response = createPaymentResponse(TEST_TRANSACTION_ID, "테스트 가맹점",
                BigDecimal.valueOf(123456));

        when(paymentAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_TRANSACTION_ID, NOTIFICATION_TYPE_PAYMENT_SUCCESS))
                .thenReturn(false);

        // When
        paymentAlertService.notifyPaymentSuccess(TEST_USER_ID, response);

        // Then
        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());
        assertEquals(BigDecimal.valueOf(123456), captor.getValue().getPlaceholders().get("amount"));
    }

    // ================================================================
    // 2. REFUND_SUCCESS 알림 테스트
    // ================================================================

    @Test
    @DisplayName("환불 완료 → REFUND_SUCCESS 알림 생성")
    void notifyRefundSuccess_payment_createsNotification() {
        // Given
        PaymentResponse response = createPaymentResponse(TEST_TRANSACTION_ID, "테스트 가맹점",
                BigDecimal.valueOf(30000));

        when(paymentAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_TRANSACTION_ID, NOTIFICATION_TYPE_REFUND_SUCCESS))
                .thenReturn(false);

        // When
        paymentAlertService.notifyRefundSuccess(TEST_USER_ID, response);

        // Then
        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());

        NotificationCreateRequestDTO request = captor.getValue();
        assertEquals(NotificationCategory.PAYMENT_NOTIFY, request.getCategory());
        assertEquals(NOTIFICATION_TYPE_REFUND_SUCCESS, request.getNotificationType());
        assertEquals(REFERENCE_TYPE, request.getReferenceType());
        assertEquals(TEST_TRANSACTION_ID, request.getReferenceId());
        assertTrue(request.getImportant());
    }

    @Test
    @DisplayName("REFUND_SUCCESS - {merchant} placeholder에 가맹점명이 전달됨")
    void notifyRefundSuccess_merchantPlaceholder_correct() {
        // Given
        PaymentResponse response = createPaymentResponse(TEST_TRANSACTION_ID, "쿠팡",
                BigDecimal.valueOf(25000));

        when(paymentAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_TRANSACTION_ID, NOTIFICATION_TYPE_REFUND_SUCCESS))
                .thenReturn(false);

        // When
        paymentAlertService.notifyRefundSuccess(TEST_USER_ID, response);

        // Then
        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());
        assertEquals("쿠팡", captor.getValue().getPlaceholders().get("merchant"));
    }

    @Test
    @DisplayName("REFUND_SUCCESS - {amount} placeholder에 환불 금액이 전달됨")
    void notifyRefundSuccess_amountPlaceholder_correct() {
        // Given
        PaymentResponse response = createPaymentResponse(TEST_TRANSACTION_ID, "테스트 가맹점",
                BigDecimal.valueOf(99000));

        when(paymentAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_TRANSACTION_ID, NOTIFICATION_TYPE_REFUND_SUCCESS))
                .thenReturn(false);

        // When
        paymentAlertService.notifyRefundSuccess(TEST_USER_ID, response);

        // Then
        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());
        assertEquals(BigDecimal.valueOf(99000), captor.getValue().getPlaceholders().get("amount"));
    }

    // ================================================================
    // 3. 중복 방지 테스트
    // ================================================================

    @Test
    @DisplayName("동일 거래 + 동일 notificationType → 중복 생성하지 않음 (PAYMENT_SUCCESS)")
    void notifyPaymentSuccess_duplicate_doesNotCreate() {
        // Given
        PaymentResponse response = createPaymentResponse(TEST_TRANSACTION_ID, "테스트 가맹점",
                BigDecimal.valueOf(50000));

        when(paymentAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_TRANSACTION_ID, NOTIFICATION_TYPE_PAYMENT_SUCCESS))
                .thenReturn(true);

        // When
        paymentAlertService.notifyPaymentSuccess(TEST_USER_ID, response);

        // Then
        verify(notificationCreateService, never()).createNotification(any(), any());
    }

    @Test
    @DisplayName("동일 거래 + 동일 notificationType → 중복 생성하지 않음 (REFUND_SUCCESS)")
    void notifyRefundSuccess_duplicate_doesNotCreate() {
        // Given
        PaymentResponse response = createPaymentResponse(TEST_TRANSACTION_ID, "테스트 가맹점",
                BigDecimal.valueOf(30000));

        when(paymentAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_TRANSACTION_ID, NOTIFICATION_TYPE_REFUND_SUCCESS))
                .thenReturn(true);

        // When
        paymentAlertService.notifyRefundSuccess(TEST_USER_ID, response);

        // Then
        verify(notificationCreateService, never()).createNotification(any(), any());
    }

    @Test
    @DisplayName("PAYMENT_SUCCESS 알림이 존재해도 REFUND_SUCCESS는 별도 알림으로 생성")
    void notifyRefundSuccess_paymentExistsButRefundNot_createsNotification() {
        // Given
        PaymentResponse response = createPaymentResponse(TEST_TRANSACTION_ID, "테스트 가맹점",
                BigDecimal.valueOf(30000));

        // REFUND_SUCCESS는 존재하지 않음
        when(paymentAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_TRANSACTION_ID, NOTIFICATION_TYPE_REFUND_SUCCESS))
                .thenReturn(false);

        // When
        paymentAlertService.notifyRefundSuccess(TEST_USER_ID, response);

        // Then
        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService, times(1)).createNotification(eq(TEST_USER_ID), captor.capture());
        assertEquals(NOTIFICATION_TYPE_REFUND_SUCCESS, captor.getValue().getNotificationType());
    }

    @Test
    @DisplayName("서로 다른 결제 거래는 각각 정상 처리")
    void notifyPaymentSuccess_differentTransactions_createsBoth() {
        // Given
        PaymentResponse response1 = createPaymentResponse(1L, "가맹점A", BigDecimal.valueOf(10000));
        PaymentResponse response2 = createPaymentResponse(2L, "가맹점B", BigDecimal.valueOf(20000));

        when(paymentAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, 1L, NOTIFICATION_TYPE_PAYMENT_SUCCESS))
                .thenReturn(false);
        when(paymentAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, 2L, NOTIFICATION_TYPE_PAYMENT_SUCCESS))
                .thenReturn(false);

        // When
        paymentAlertService.notifyPaymentSuccess(TEST_USER_ID, response1);
        paymentAlertService.notifyPaymentSuccess(TEST_USER_ID, response2);

        // Then
        verify(notificationCreateService, times(2)).createNotification(eq(TEST_USER_ID), any());
    }

    // ================================================================
    // 4. 결제가 아닌 거래 테스트
    // ================================================================

    @Test
    @DisplayName("결제가 아닌 거래(DEPOSIT) → 알림 미생성")
    void notifyPaymentSuccess_deposit_doesNotCreate() {
        // Given
        PaymentResponse response = createNonPaymentResponse(TEST_TRANSACTION_ID);

        // When
        paymentAlertService.notifyPaymentSuccess(TEST_USER_ID, response);

        // Then
        verify(notificationCreateService, never()).createNotification(any(), any());
        verify(paymentAlertMapper, never()).existsNotificationByReference(any(), any(), any(), any());
    }

    @Test
    @DisplayName("REFUND_SUCCESS - 결제가 아닌 거래(DEPOSIT) → 알림 미생성")
    void notifyRefundSuccess_deposit_doesNotCreate() {
        // Given
        PaymentResponse response = createNonPaymentResponse(TEST_TRANSACTION_ID);

        // When
        paymentAlertService.notifyRefundSuccess(TEST_USER_ID, response);

        // Then
        verify(notificationCreateService, never()).createNotification(any(), any());
        verify(paymentAlertMapper, never()).existsNotificationByReference(any(), any(), any(), any());
    }

    // ================================================================
    // 5. null 입력 테스트
    // ================================================================

    @Test
    @DisplayName("null 응답 → 알림 미생성 (PAYMENT_SUCCESS)")
    void notifyPaymentSuccess_nullResponse_doesNotCreate() {
        paymentAlertService.notifyPaymentSuccess(TEST_USER_ID, null);
        verify(notificationCreateService, never()).createNotification(any(), any());
    }

    @Test
    @DisplayName("transactionId가 null인 응답 → 알림 미생성 (PAYMENT_SUCCESS)")
    void notifyPaymentSuccess_nullTransactionId_doesNotCreate() {
        // Given
        PaymentResponse response = new PaymentResponse();
        response.setTransactionId(null);

        // When
        paymentAlertService.notifyPaymentSuccess(TEST_USER_ID, response);

        // Then
        verify(notificationCreateService, never()).createNotification(any(), any());
    }

    @Test
    @DisplayName("null 응답 → 알림 미생성 (REFUND_SUCCESS)")
    void notifyRefundSuccess_nullResponse_doesNotCreate() {
        paymentAlertService.notifyRefundSuccess(TEST_USER_ID, null);
        verify(notificationCreateService, never()).createNotification(any(), any());
    }

    // ================================================================
    // 6. 공통 서비스 연동 테스트
    // ================================================================

    @Test
    @DisplayName("PAYMENT_NOTIFY 카테고리로 호출됨 (PAYMENT_SUCCESS)")
    void notifyPaymentSuccess_correctCategory() {
        // Given
        PaymentResponse response = createPaymentResponse(TEST_TRANSACTION_ID, "테스트 가맹점",
                BigDecimal.valueOf(50000));

        when(paymentAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_TRANSACTION_ID, NOTIFICATION_TYPE_PAYMENT_SUCCESS))
                .thenReturn(false);

        // When
        paymentAlertService.notifyPaymentSuccess(TEST_USER_ID, response);

        // Then
        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());
        assertEquals(NotificationCategory.PAYMENT_NOTIFY, captor.getValue().getCategory());
    }

    @Test
    @DisplayName("PAYMENT_NOTIFY 카테고리로 호출됨 (REFUND_SUCCESS)")
    void notifyRefundSuccess_correctCategory() {
        // Given
        PaymentResponse response = createPaymentResponse(TEST_TRANSACTION_ID, "테스트 가맹점",
                BigDecimal.valueOf(30000));

        when(paymentAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_TRANSACTION_ID, NOTIFICATION_TYPE_REFUND_SUCCESS))
                .thenReturn(false);

        // When
        paymentAlertService.notifyRefundSuccess(TEST_USER_ID, response);

        // Then
        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());
        assertEquals(NotificationCategory.PAYMENT_NOTIFY, captor.getValue().getCategory());
    }

    @Test
    @DisplayName("referenceType=TRANSACTION, referenceId=transactionId로 호출됨")
    void notifyPaymentSuccess_referenceCorrect() {
        // Given
        PaymentResponse response = createPaymentResponse(TEST_TRANSACTION_ID, "테스트 가맹점",
                BigDecimal.valueOf(50000));

        when(paymentAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_TRANSACTION_ID, NOTIFICATION_TYPE_PAYMENT_SUCCESS))
                .thenReturn(false);

        // When
        paymentAlertService.notifyPaymentSuccess(TEST_USER_ID, response);

        // Then
        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());
        assertEquals(REFERENCE_TYPE, captor.getValue().getReferenceType());
        assertEquals(TEST_TRANSACTION_ID, captor.getValue().getReferenceId());
    }

    // ================================================================
    // 7. 중요 알림 플래그 테스트
    // ================================================================

    @Test
    @DisplayName("PAYMENT_SUCCESS 알림은 중요 알림")
    void notifyPaymentSuccess_isImportant() {
        // Given
        PaymentResponse response = createPaymentResponse(TEST_TRANSACTION_ID, "테스트 가맹점",
                BigDecimal.valueOf(50000));

        when(paymentAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_TRANSACTION_ID, NOTIFICATION_TYPE_PAYMENT_SUCCESS))
                .thenReturn(false);

        // When
        paymentAlertService.notifyPaymentSuccess(TEST_USER_ID, response);

        // Then
        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());
        assertTrue(captor.getValue().getImportant());
    }

    @Test
    @DisplayName("REFUND_SUCCESS 알림은 중요 알림")
    void notifyRefundSuccess_isImportant() {
        // Given
        PaymentResponse response = createPaymentResponse(TEST_TRANSACTION_ID, "테스트 가맹점",
                BigDecimal.valueOf(30000));

        when(paymentAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_TRANSACTION_ID, NOTIFICATION_TYPE_REFUND_SUCCESS))
                .thenReturn(false);

        // When
        paymentAlertService.notifyRefundSuccess(TEST_USER_ID, response);

        // Then
        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());
        assertTrue(captor.getValue().getImportant());
    }

    // ================================================================
    // 8. 수신 설정 테스트 (PaymentAlertService는 수신 설정을 직접 조회하지 않음)
    // ================================================================

    @Test
    @DisplayName("PAYMENT_NOTIFY 수신 ON → NotificationCreateService에 알림 생성 요청 전달")
    void notifyPaymentSuccess_receivesOn_passesToCreateService() {
        // Given
        PaymentResponse response = createPaymentResponse(TEST_TRANSACTION_ID, "테스트 가맹점",
                BigDecimal.valueOf(50000));

        when(paymentAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_TRANSACTION_ID, NOTIFICATION_TYPE_PAYMENT_SUCCESS))
                .thenReturn(false);

        // When
        paymentAlertService.notifyPaymentSuccess(TEST_USER_ID, response);

        // Then - NotificationCreateService가 호출됨 (수신 설정은 NotificationCreateServiceImpl이 처리)
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), any());
    }

    @Test
    @DisplayName("결제 도메인에서 수신 설정을 직접 조회하지 않음")
    void notifyPaymentSuccess_doesNotQueryNotificationSettings() {
        // Given
        PaymentResponse response = createPaymentResponse(TEST_TRANSACTION_ID, "테스트 가맹점",
                BigDecimal.valueOf(50000));

        when(paymentAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_TRANSACTION_ID, NOTIFICATION_TYPE_PAYMENT_SUCCESS))
                .thenReturn(false);

        // When
        paymentAlertService.notifyPaymentSuccess(TEST_USER_ID, response);

        // Then - PaymentAlertMapper의 existsNotificationByReference만 호출되고,
        //        알림 수신 설정 관련 추가 쿼리는 없음
        verify(paymentAlertMapper, times(1)).existsNotificationByReference(
                any(), any(), any(), any());
    }
}
