package com.workit.domain.payment.service;

import com.workit.domain.notification.dto.request.NotificationCreateRequestDTO;
import com.workit.domain.notification.enums.NotificationCategory;
import com.workit.domain.notification.service.NotificationCreateService;
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

@ExtendWith(MockitoExtension.class)
class PaymentAlertServiceImplTest {

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
        paymentAlertService = new PaymentAlertServiceImpl(notificationCreateService);
    }

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

    @Test
    @DisplayName("결제 성공 → PAYMENT_SUCCESS 알림 생성")
    void notifyPaymentSuccess_createsNotification() {
        PaymentResponse response = createPaymentResponse(TEST_TRANSACTION_ID, "테스트 가맹점",
                BigDecimal.valueOf(50000));

        paymentAlertService.notifyPaymentSuccess(TEST_USER_ID, response);

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
        PaymentResponse response = createPaymentResponse(TEST_TRANSACTION_ID, "스타벅스 강남점",
                BigDecimal.valueOf(5500));

        paymentAlertService.notifyPaymentSuccess(TEST_USER_ID, response);

        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());
        assertEquals("스타벅스 강남점", captor.getValue().getPlaceholders().get("merchant"));
    }

    @Test
    @DisplayName("환불 완료 → REFUND_SUCCESS 알림 생성")
    void notifyRefundSuccess_createsNotification() {
        PaymentResponse response = createPaymentResponse(TEST_TRANSACTION_ID, "테스트 가맹점",
                BigDecimal.valueOf(30000));

        paymentAlertService.notifyRefundSuccess(TEST_USER_ID, response);

        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());

        NotificationCreateRequestDTO request = captor.getValue();
        assertEquals(NotificationCategory.PAYMENT_NOTIFY, request.getCategory());
        assertEquals(NOTIFICATION_TYPE_REFUND_SUCCESS, request.getNotificationType());
        assertTrue(request.getImportant());
    }

    @Test
    @DisplayName("결제가 아닌 거래(DEPOSIT) → 알림 미생성")
    void notifyPaymentSuccess_deposit_doesNotCreate() {
        PaymentResponse response = createNonPaymentResponse(TEST_TRANSACTION_ID);

        paymentAlertService.notifyPaymentSuccess(TEST_USER_ID, response);

        verify(notificationCreateService, never()).createNotification(any(), any());
    }

    @Test
    @DisplayName("null 응답 → 알림 미생성")
    void notifyPaymentSuccess_nullResponse_doesNotCreate() {
        paymentAlertService.notifyPaymentSuccess(TEST_USER_ID, null);
        verify(notificationCreateService, never()).createNotification(any(), any());
    }

    @Test
    @DisplayName("transactionId가 null인 응답 → 알림 미생성")
    void notifyPaymentSuccess_nullTransactionId_doesNotCreate() {
        PaymentResponse response = new PaymentResponse();
        response.setTransactionId(null);

        paymentAlertService.notifyPaymentSuccess(TEST_USER_ID, response);

        verify(notificationCreateService, never()).createNotification(any(), any());
    }
}
