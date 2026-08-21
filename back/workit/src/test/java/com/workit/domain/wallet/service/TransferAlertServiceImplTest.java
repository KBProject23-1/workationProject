package com.workit.domain.wallet.service;

import com.workit.domain.notification.dto.request.NotificationCreateRequestDTO;
import com.workit.domain.notification.enums.NotificationCategory;
import com.workit.domain.notification.service.NotificationCreateService;
import com.workit.domain.wallet.dto.response.ChargeResponse;
import com.workit.domain.wallet.dto.response.RefundResponse;
import com.workit.domain.transaction.vo.TransactionVO;
import com.workit.domain.account.vo.BankAccountVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// TransferAlertServiceImpl 테스트
// - WALLET_CHARGE_SUCCESS 알림, ACCOUNT_REFUND_SUCCESS 알림 등을 검증한다
// - 중복 알림 방지는 NotificationCreateServiceImpl 내부에서 처리하므로, 이 테스트에서는
//   TransferAlertServiceImpl이 올바르게 NotificationCreateService를 호출하는지만 검증한다
@ExtendWith(MockitoExtension.class)
class TransferAlertServiceImplTest {

    @Mock
    private NotificationCreateService notificationCreateService;

    private TransferAlertServiceImpl transferAlertService;

    private static final Long TEST_USER_ID = 100L;
    private static final Long TEST_TRANSACTION_ID = 1L;
    private static final String REFERENCE_TYPE = "TRANSACTION";
    private static final String NOTIFICATION_TYPE_CHARGE_SUCCESS = "WALLET_CHARGE_SUCCESS";
    private static final String NOTIFICATION_TYPE_REFUND_SUCCESS = "ACCOUNT_REFUND_SUCCESS";

    @BeforeEach
    void setUp() {
        transferAlertService = new TransferAlertServiceImpl(notificationCreateService);
    }

    // ---------- 테스트 데이터 헬퍼 ----------

    private ChargeResponse createChargeResponse(Long transactionId, BigDecimal chargedAmount) {
        TransactionVO tx = new TransactionVO();
        tx.setId(transactionId);
        tx.setAmount(chargedAmount);
        tx.setTransactionType("DEPOSIT");
        tx.setPaymentSourceType("WALLET");
        tx.setStatus("PAID");

        BigDecimal currentBalance = BigDecimal.valueOf(100000);
        return ChargeResponse.of(tx, currentBalance);
    }

    private RefundResponse createRefundResponse(Long transactionId, BigDecimal refundedAmount) {
        TransactionVO tx = new TransactionVO();
        tx.setId(transactionId);
        tx.setAmount(refundedAmount);
        tx.setTransactionType("WITHDRAWAL");
        tx.setPaymentSourceType("WALLET");
        tx.setStatus("PAID");

        BankAccountVO account = new BankAccountVO();
        account.setId(1L);
        account.setBankCode("004");
        account.setAccountNumber("1234567890123");
        account.setBalance(BigDecimal.valueOf(200000));

        BigDecimal remainingBalance = BigDecimal.valueOf(50000);
        return RefundResponse.of(tx, remainingBalance, account);
    }

    // ================================================================
    // 1. WALLET_CHARGE_SUCCESS 알림 테스트
    // ================================================================

    @Test
    @DisplayName("정상 충전 완료 → WALLET_CHARGE_SUCCESS 알림 생성")
    void notifyChargeSuccess_createsNotification() {
        ChargeResponse response = createChargeResponse(TEST_TRANSACTION_ID, BigDecimal.valueOf(50000));

        transferAlertService.notifyChargeSuccess(TEST_USER_ID, response);

        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());

        NotificationCreateRequestDTO request = captor.getValue();
        assertEquals(NotificationCategory.TRANSFER_NOTIFY, request.getCategory());
        assertEquals(NOTIFICATION_TYPE_CHARGE_SUCCESS, request.getNotificationType());
        assertEquals(REFERENCE_TYPE, request.getReferenceType());
        assertEquals(TEST_TRANSACTION_ID, request.getReferenceId());
        assertTrue(request.getImportant());
    }

    @Test
    @DisplayName("WALLET_CHARGE_SUCCESS - {amount} placeholder에 충전 금액이 전달됨")
    void notifyChargeSuccess_amountPlaceholder_correct() {
        ChargeResponse response = createChargeResponse(TEST_TRANSACTION_ID, BigDecimal.valueOf(100000));

        transferAlertService.notifyChargeSuccess(TEST_USER_ID, response);

        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());
        assertEquals(BigDecimal.valueOf(100000), captor.getValue().getPlaceholders().get("amount"));
    }

    // ================================================================
    // 2. ACCOUNT_REFUND_SUCCESS 알림 테스트
    // ================================================================

    @Test
    @DisplayName("정상 환불 완료 → ACCOUNT_REFUND_SUCCESS 알림 생성")
    void notifyRefundSuccess_createsNotification() {
        RefundResponse response = createRefundResponse(TEST_TRANSACTION_ID, BigDecimal.valueOf(30000));

        transferAlertService.notifyRefundSuccess(TEST_USER_ID, response);

        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());

        NotificationCreateRequestDTO request = captor.getValue();
        assertEquals(NotificationCategory.TRANSFER_NOTIFY, request.getCategory());
        assertEquals(NOTIFICATION_TYPE_REFUND_SUCCESS, request.getNotificationType());
        assertEquals(REFERENCE_TYPE, request.getReferenceType());
        assertEquals(TEST_TRANSACTION_ID, request.getReferenceId());
        assertTrue(request.getImportant());
    }

    @Test
    @DisplayName("ACCOUNT_REFUND_SUCCESS - {amount} placeholder에 환불 금액이 전달됨")
    void notifyRefundSuccess_amountPlaceholder_correct() {
        RefundResponse response = createRefundResponse(TEST_TRANSACTION_ID, BigDecimal.valueOf(25000));

        transferAlertService.notifyRefundSuccess(TEST_USER_ID, response);

        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());
        assertEquals(BigDecimal.valueOf(25000), captor.getValue().getPlaceholders().get("amount"));
    }

    // ================================================================
    // 3. null 입력 테스트
    // ================================================================

    @Test
    @DisplayName("null 응답 → 알림 미생성 (충전)")
    void notifyChargeSuccess_nullResponse_doesNotCreate() {
        transferAlertService.notifyChargeSuccess(TEST_USER_ID, null);
        verify(notificationCreateService, never()).createNotification(any(), any());
    }

    @Test
    @DisplayName("transactionId가 null인 응답 → 알림 미생성 (충전)")
    void notifyChargeSuccess_nullTransactionId_doesNotCreate() {
        ChargeResponse response = new ChargeResponse();
        response.setTransactionId(null);

        transferAlertService.notifyChargeSuccess(TEST_USER_ID, response);

        verify(notificationCreateService, never()).createNotification(any(), any());
    }

    @Test
    @DisplayName("null 응답 → 알림 미생성 (환불)")
    void notifyRefundSuccess_nullResponse_doesNotCreate() {
        transferAlertService.notifyRefundSuccess(TEST_USER_ID, null);
        verify(notificationCreateService, never()).createNotification(any(), any());
    }

    @Test
    @DisplayName("transactionId가 null인 응답 → 알림 미생성 (환불)")
    void notifyRefundSuccess_nullTransactionId_doesNotCreate() {
        RefundResponse response = new RefundResponse();
        response.setTransactionId(null);

        transferAlertService.notifyRefundSuccess(TEST_USER_ID, response);

        verify(notificationCreateService, never()).createNotification(any(), any());
    }

    // ================================================================
    // 4. 공통 서비스 연동 테스트
    // ================================================================

    @Test
    @DisplayName("TRANSFER_NOTIFY 카테고리로 호출됨 (충전)")
    void notifyChargeSuccess_correctCategory() {
        ChargeResponse response = createChargeResponse(TEST_TRANSACTION_ID, BigDecimal.valueOf(50000));

        transferAlertService.notifyChargeSuccess(TEST_USER_ID, response);

        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());
        assertEquals(NotificationCategory.TRANSFER_NOTIFY, captor.getValue().getCategory());
    }

    @Test
    @DisplayName("referenceType=TRANSACTION, referenceId=transactionId로 호출됨 (충전)")
    void notifyChargeSuccess_referenceCorrect() {
        ChargeResponse response = createChargeResponse(TEST_TRANSACTION_ID, BigDecimal.valueOf(50000));

        transferAlertService.notifyChargeSuccess(TEST_USER_ID, response);

        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());
        assertEquals(REFERENCE_TYPE, captor.getValue().getReferenceType());
        assertEquals(TEST_TRANSACTION_ID, captor.getValue().getReferenceId());
    }

    // ================================================================
    // 5. 중요 알림 플래그 테스트
    // ================================================================

    @Test
    @DisplayName("충전 알림은 중요 알림")
    void notifyChargeSuccess_isImportant() {
        ChargeResponse response = createChargeResponse(TEST_TRANSACTION_ID, BigDecimal.valueOf(50000));

        transferAlertService.notifyChargeSuccess(TEST_USER_ID, response);

        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());
        assertTrue(captor.getValue().getImportant());
    }

    // ================================================================
    // 6. placeholder 테스트
    // ================================================================

    @Test
    @DisplayName("충전 알림 - amount placeholder만 전달됨")
    void notifyChargeSuccess_onlyAmountPlaceholder() {
        ChargeResponse response = createChargeResponse(TEST_TRANSACTION_ID, BigDecimal.valueOf(50000));

        transferAlertService.notifyChargeSuccess(TEST_USER_ID, response);

        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());

        Map<String, Object> placeholders = captor.getValue().getPlaceholders();
        assertEquals(1, placeholders.size());
        assertTrue(placeholders.containsKey("amount"));
        assertFalse(placeholders.containsKey("merchant"));
        assertFalse(placeholders.containsKey("date"));
    }

    // ================================================================
    // 7. 자동 충전 알림 테스트
    // ================================================================

    @Test
    @DisplayName("자동 충전 성공 → WALLET_CHARGE_SUCCESS 알림 생성")
    void notifyAutoChargeSuccess_createsNotification() {
        transferAlertService.notifyChargeSuccess(TEST_USER_ID, TEST_TRANSACTION_ID, BigDecimal.valueOf(10000));

        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());

        NotificationCreateRequestDTO request = captor.getValue();
        assertEquals(NotificationCategory.TRANSFER_NOTIFY, request.getCategory());
        assertEquals(NOTIFICATION_TYPE_CHARGE_SUCCESS, request.getNotificationType());
        assertEquals(REFERENCE_TYPE, request.getReferenceType());
        assertEquals(TEST_TRANSACTION_ID, request.getReferenceId());
        assertTrue(request.getImportant());
    }

    @Test
    @DisplayName("자동 충전 - amount placeholder에 충전 금액이 전달됨")
    void notifyAutoChargeSuccess_amountPlaceholder_correct() {
        transferAlertService.notifyChargeSuccess(TEST_USER_ID, TEST_TRANSACTION_ID, BigDecimal.valueOf(15000));

        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());
        assertEquals(BigDecimal.valueOf(15000), captor.getValue().getPlaceholders().get("amount"));
    }

    @Test
    @DisplayName("자동 충전 - transactionId가 null이면 알림 미생성")
    void notifyAutoChargeSuccess_nullTransactionId_doesNotCreate() {
        transferAlertService.notifyChargeSuccess(TEST_USER_ID, null, BigDecimal.valueOf(10000));
        verify(notificationCreateService, never()).createNotification(any(), any());
    }
}
