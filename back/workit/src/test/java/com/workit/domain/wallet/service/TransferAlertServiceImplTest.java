package com.workit.domain.wallet.service;

import com.workit.domain.notification.dto.request.NotificationCreateRequestDTO;
import com.workit.domain.notification.enums.NotificationCategory;
import com.workit.domain.notification.service.NotificationCreateService;
import com.workit.domain.wallet.dto.response.ChargeResponse;
import com.workit.domain.wallet.dto.response.RefundResponse;
import com.workit.domain.wallet.mapper.TransferAlertMapper;
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
// - WALLET_CHARGE_SUCCESS 알림, ACCOUNT_REFUND_SUCCESS 알림, 중복 방지 등을 검증한다
@ExtendWith(MockitoExtension.class)
class TransferAlertServiceImplTest {

    @Mock
    private TransferAlertMapper transferAlertMapper;

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
        transferAlertService = new TransferAlertServiceImpl(
                transferAlertMapper, notificationCreateService);
    }

    // ---------- 테스트 데이터 헬퍼 ----------

    /** 충전 완료 응답 생성 */
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

    /** 환불 완료 응답 생성 */
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
        // Given
        ChargeResponse response = createChargeResponse(TEST_TRANSACTION_ID, BigDecimal.valueOf(50000));

        when(transferAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_TRANSACTION_ID, NOTIFICATION_TYPE_CHARGE_SUCCESS))
                .thenReturn(false);

        // When
        transferAlertService.notifyChargeSuccess(TEST_USER_ID, response);

        // Then
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
        // Given
        ChargeResponse response = createChargeResponse(TEST_TRANSACTION_ID, BigDecimal.valueOf(100000));

        when(transferAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_TRANSACTION_ID, NOTIFICATION_TYPE_CHARGE_SUCCESS))
                .thenReturn(false);

        // When
        transferAlertService.notifyChargeSuccess(TEST_USER_ID, response);

        // Then
        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());
        assertEquals(BigDecimal.valueOf(100000), captor.getValue().getPlaceholders().get("amount"));
    }

    @Test
    @DisplayName("WALLET_CHARGE_SUCCESS - 소수점 금액 placeholder 정상 전달")
    void notifyChargeSuccess_decimalAmount_correct() {
        // Given
        ChargeResponse response = createChargeResponse(TEST_TRANSACTION_ID, BigDecimal.valueOf(12345.67));

        when(transferAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_TRANSACTION_ID, NOTIFICATION_TYPE_CHARGE_SUCCESS))
                .thenReturn(false);

        // When
        transferAlertService.notifyChargeSuccess(TEST_USER_ID, response);

        // Then
        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());
        assertEquals(BigDecimal.valueOf(12345.67), captor.getValue().getPlaceholders().get("amount"));
    }

    // ================================================================
    // 2. ACCOUNT_REFUND_SUCCESS 알림 테스트
    // ================================================================

    @Test
    @DisplayName("정상 환불 완료 → ACCOUNT_REFUND_SUCCESS 알림 생성")
    void notifyRefundSuccess_createsNotification() {
        // Given
        RefundResponse response = createRefundResponse(TEST_TRANSACTION_ID, BigDecimal.valueOf(30000));

        when(transferAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_TRANSACTION_ID, NOTIFICATION_TYPE_REFUND_SUCCESS))
                .thenReturn(false);

        // When
        transferAlertService.notifyRefundSuccess(TEST_USER_ID, response);

        // Then
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
        // Given
        RefundResponse response = createRefundResponse(TEST_TRANSACTION_ID, BigDecimal.valueOf(25000));

        when(transferAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_TRANSACTION_ID, NOTIFICATION_TYPE_REFUND_SUCCESS))
                .thenReturn(false);

        // When
        transferAlertService.notifyRefundSuccess(TEST_USER_ID, response);

        // Then
        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());
        assertEquals(BigDecimal.valueOf(25000), captor.getValue().getPlaceholders().get("amount"));
    }

    // ================================================================
    // 3. 중복 방지 테스트
    // ================================================================

    @Test
    @DisplayName("동일 거래 + 동일 notificationType → 중복 생성하지 않음 (WALLET_CHARGE_SUCCESS)")
    void notifyChargeSuccess_duplicate_doesNotCreate() {
        // Given
        ChargeResponse response = createChargeResponse(TEST_TRANSACTION_ID, BigDecimal.valueOf(50000));

        when(transferAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_TRANSACTION_ID, NOTIFICATION_TYPE_CHARGE_SUCCESS))
                .thenReturn(true);

        // When
        transferAlertService.notifyChargeSuccess(TEST_USER_ID, response);

        // Then
        verify(notificationCreateService, never()).createNotification(any(), any());
    }

    @Test
    @DisplayName("동일 거래 + 동일 notificationType → 중복 생성하지 않음 (ACCOUNT_REFUND_SUCCESS)")
    void notifyRefundSuccess_duplicate_doesNotCreate() {
        // Given
        RefundResponse response = createRefundResponse(TEST_TRANSACTION_ID, BigDecimal.valueOf(30000));

        when(transferAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_TRANSACTION_ID, NOTIFICATION_TYPE_REFUND_SUCCESS))
                .thenReturn(true);

        // When
        transferAlertService.notifyRefundSuccess(TEST_USER_ID, response);

        // Then
        verify(notificationCreateService, never()).createNotification(any(), any());
    }

    @Test
    @DisplayName("WALLET_CHARGE_SUCCESS 알림이 존재해도 ACCOUNT_REFUND_SUCCESS는 별도 알림으로 생성")
    void notifyRefundSuccess_chargeExistsButRefundNot_createsNotification() {
        // Given
        RefundResponse response = createRefundResponse(TEST_TRANSACTION_ID, BigDecimal.valueOf(30000));

        // ACCOUNT_REFUND_SUCCESS는 존재하지 않음
        when(transferAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_TRANSACTION_ID, NOTIFICATION_TYPE_REFUND_SUCCESS))
                .thenReturn(false);

        // When
        transferAlertService.notifyRefundSuccess(TEST_USER_ID, response);

        // Then
        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService, times(1)).createNotification(eq(TEST_USER_ID), captor.capture());
        assertEquals(NOTIFICATION_TYPE_REFUND_SUCCESS, captor.getValue().getNotificationType());
    }

    @Test
    @DisplayName("서로 다른 거래는 각각 정상 처리 (충전)")
    void notifyChargeSuccess_differentTransactions_createsBoth() {
        // Given
        ChargeResponse response1 = createChargeResponse(1L, BigDecimal.valueOf(10000));
        ChargeResponse response2 = createChargeResponse(2L, BigDecimal.valueOf(20000));

        when(transferAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, 1L, NOTIFICATION_TYPE_CHARGE_SUCCESS))
                .thenReturn(false);
        when(transferAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, 2L, NOTIFICATION_TYPE_CHARGE_SUCCESS))
                .thenReturn(false);

        // When
        transferAlertService.notifyChargeSuccess(TEST_USER_ID, response1);
        transferAlertService.notifyChargeSuccess(TEST_USER_ID, response2);

        // Then
        verify(notificationCreateService, times(2)).createNotification(eq(TEST_USER_ID), any());
    }

    @Test
    @DisplayName("서로 다른 거래는 각각 정상 처리 (환불)")
    void notifyRefundSuccess_differentTransactions_createsBoth() {
        // Given
        RefundResponse response1 = createRefundResponse(1L, BigDecimal.valueOf(10000));
        RefundResponse response2 = createRefundResponse(2L, BigDecimal.valueOf(20000));

        when(transferAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, 1L, NOTIFICATION_TYPE_REFUND_SUCCESS))
                .thenReturn(false);
        when(transferAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, 2L, NOTIFICATION_TYPE_REFUND_SUCCESS))
                .thenReturn(false);

        // When
        transferAlertService.notifyRefundSuccess(TEST_USER_ID, response1);
        transferAlertService.notifyRefundSuccess(TEST_USER_ID, response2);

        // Then
        verify(notificationCreateService, times(2)).createNotification(eq(TEST_USER_ID), any());
    }

    @Test
    @DisplayName("한 사용자의 거래가 다른 사용자의 알림 중복 판단에 영향을 주지 않음")
    void notifyChargeSuccess_differentUsers_independentDuplicateCheck() {
        // Given - 200번 사용자의 동일 거래에 대해 알림이 없는 경우
        ChargeResponse response = createChargeResponse(TEST_TRANSACTION_ID, BigDecimal.valueOf(50000));

        when(transferAlertMapper.existsNotificationByReference(
                200L, REFERENCE_TYPE, TEST_TRANSACTION_ID, NOTIFICATION_TYPE_CHARGE_SUCCESS))
                .thenReturn(false); // 200번 사용자는 신규

        // When
        transferAlertService.notifyChargeSuccess(200L, response);

        // Then - 200번 사용자에게 알림 생성됨 (100번 사용자의 알림 유무와 무관)
        verify(notificationCreateService, times(1)).createNotification(eq(200L), any());
        // 200번 사용자의 알림 중복 확인 쿼리가 호출됨 (userId=200으로)
        verify(transferAlertMapper, times(1)).existsNotificationByReference(
                eq(200L), eq(REFERENCE_TYPE), eq(TEST_TRANSACTION_ID), eq(NOTIFICATION_TYPE_CHARGE_SUCCESS));
    }

    // ================================================================
    // 4. null 입력 테스트
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
        // Given
        ChargeResponse response = new ChargeResponse();
        response.setTransactionId(null);

        // When
        transferAlertService.notifyChargeSuccess(TEST_USER_ID, response);

        // Then
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
        // Given
        RefundResponse response = new RefundResponse();
        response.setTransactionId(null);

        // When
        transferAlertService.notifyRefundSuccess(TEST_USER_ID, response);

        // Then
        verify(notificationCreateService, never()).createNotification(any(), any());
    }

    // ================================================================
    // 5. 공통 서비스 연동 테스트
    // ================================================================

    @Test
    @DisplayName("TRANSFER_NOTIFY 카테고리로 호출됨 (충전)")
    void notifyChargeSuccess_correctCategory() {
        // Given
        ChargeResponse response = createChargeResponse(TEST_TRANSACTION_ID, BigDecimal.valueOf(50000));

        when(transferAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_TRANSACTION_ID, NOTIFICATION_TYPE_CHARGE_SUCCESS))
                .thenReturn(false);

        // When
        transferAlertService.notifyChargeSuccess(TEST_USER_ID, response);

        // Then
        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());
        assertEquals(NotificationCategory.TRANSFER_NOTIFY, captor.getValue().getCategory());
    }

    @Test
    @DisplayName("TRANSFER_NOTIFY 카테고리로 호출됨 (환불)")
    void notifyRefundSuccess_correctCategory() {
        // Given
        RefundResponse response = createRefundResponse(TEST_TRANSACTION_ID, BigDecimal.valueOf(30000));

        when(transferAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_TRANSACTION_ID, NOTIFICATION_TYPE_REFUND_SUCCESS))
                .thenReturn(false);

        // When
        transferAlertService.notifyRefundSuccess(TEST_USER_ID, response);

        // Then
        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());
        assertEquals(NotificationCategory.TRANSFER_NOTIFY, captor.getValue().getCategory());
    }

    @Test
    @DisplayName("referenceType=TRANSACTION, referenceId=transactionId로 호출됨 (충전)")
    void notifyChargeSuccess_referenceCorrect() {
        // Given
        ChargeResponse response = createChargeResponse(TEST_TRANSACTION_ID, BigDecimal.valueOf(50000));

        when(transferAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_TRANSACTION_ID, NOTIFICATION_TYPE_CHARGE_SUCCESS))
                .thenReturn(false);

        // When
        transferAlertService.notifyChargeSuccess(TEST_USER_ID, response);

        // Then
        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());
        assertEquals(REFERENCE_TYPE, captor.getValue().getReferenceType());
        assertEquals(TEST_TRANSACTION_ID, captor.getValue().getReferenceId());
    }

    @Test
    @DisplayName("referenceType=TRANSACTION, referenceId=transactionId로 호출됨 (환불)")
    void notifyRefundSuccess_referenceCorrect() {
        // Given
        RefundResponse response = createRefundResponse(TEST_TRANSACTION_ID, BigDecimal.valueOf(30000));

        when(transferAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_TRANSACTION_ID, NOTIFICATION_TYPE_REFUND_SUCCESS))
                .thenReturn(false);

        // When
        transferAlertService.notifyRefundSuccess(TEST_USER_ID, response);

        // Then
        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());
        assertEquals(REFERENCE_TYPE, captor.getValue().getReferenceType());
        assertEquals(TEST_TRANSACTION_ID, captor.getValue().getReferenceId());
    }

    // ================================================================
    // 6. 중요 알림 플래그 테스트
    // ================================================================

    @Test
    @DisplayName("충전 알림은 중요 알림")
    void notifyChargeSuccess_isImportant() {
        // Given
        ChargeResponse response = createChargeResponse(TEST_TRANSACTION_ID, BigDecimal.valueOf(50000));

        when(transferAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_TRANSACTION_ID, NOTIFICATION_TYPE_CHARGE_SUCCESS))
                .thenReturn(false);

        // When
        transferAlertService.notifyChargeSuccess(TEST_USER_ID, response);

        // Then
        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());
        assertTrue(captor.getValue().getImportant());
    }

    @Test
    @DisplayName("환불 알림은 중요 알림")
    void notifyRefundSuccess_isImportant() {
        // Given
        RefundResponse response = createRefundResponse(TEST_TRANSACTION_ID, BigDecimal.valueOf(30000));

        when(transferAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_TRANSACTION_ID, NOTIFICATION_TYPE_REFUND_SUCCESS))
                .thenReturn(false);

        // When
        transferAlertService.notifyRefundSuccess(TEST_USER_ID, response);

        // Then
        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());
        assertTrue(captor.getValue().getImportant());
    }

    // ================================================================
    // 7. 수신 설정 테스트 (TransferAlertService는 수신 설정을 직접 조회하지 않음)
    // ================================================================

    @Test
    @DisplayName("TRANSFER_NOTIFY 수신 ON → NotificationCreateService에 알림 생성 요청 전달")
    void notifyChargeSuccess_receivesOn_passesToCreateService() {
        // Given
        ChargeResponse response = createChargeResponse(TEST_TRANSACTION_ID, BigDecimal.valueOf(50000));

        when(transferAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_TRANSACTION_ID, NOTIFICATION_TYPE_CHARGE_SUCCESS))
                .thenReturn(false);

        // When
        transferAlertService.notifyChargeSuccess(TEST_USER_ID, response);

        // Then - NotificationCreateService가 호출됨 (수신 설정은 NotificationCreateServiceImpl이 처리)
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), any());
    }

    @Test
    @DisplayName("입출금 도메인에서 수신 설정을 직접 조회하지 않음")
    void notifyChargeSuccess_doesNotQueryNotificationSettings() {
        // Given
        ChargeResponse response = createChargeResponse(TEST_TRANSACTION_ID, BigDecimal.valueOf(50000));

        when(transferAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_TRANSACTION_ID, NOTIFICATION_TYPE_CHARGE_SUCCESS))
                .thenReturn(false);

        // When
        transferAlertService.notifyChargeSuccess(TEST_USER_ID, response);

        // Then - TransferAlertMapper의 existsNotificationByReference만 호출되고,
        //        알림 수신 설정 관련 추가 쿼리는 없음
        verify(transferAlertMapper, times(1)).existsNotificationByReference(
                any(), any(), any(), any());
    }

    // ================================================================
    // 8. placeholder가 template에 없는 값은 전달하지 않는지 확인
    // ================================================================

    @Test
    @DisplayName("충전 알림 - template에 없는 placeholder는 전달하지 않음 (merchant 등)")
    void notifyChargeSuccess_doesNotPassIrrelevantPlaceholders() {
        // Given
        ChargeResponse response = createChargeResponse(TEST_TRANSACTION_ID, BigDecimal.valueOf(50000));

        when(transferAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_TRANSACTION_ID, NOTIFICATION_TYPE_CHARGE_SUCCESS))
                .thenReturn(false);

        // When
        transferAlertService.notifyChargeSuccess(TEST_USER_ID, response);

        // Then
        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());

        Map<String, Object> placeholders = captor.getValue().getPlaceholders();
        // DB 템플릿에는 {amount}만 존재하므로 amount만 전달됨
        assertEquals(1, placeholders.size());
        assertTrue(placeholders.containsKey("amount"));
        assertFalse(placeholders.containsKey("merchant"));
        assertFalse(placeholders.containsKey("date"));
    }

    @Test
    @DisplayName("환불 알림 - template에 없는 placeholder는 전달하지 않음 (merchant 등)")
    void notifyRefundSuccess_doesNotPassIrrelevantPlaceholders() {
        // Given
        RefundResponse response = createRefundResponse(TEST_TRANSACTION_ID, BigDecimal.valueOf(30000));

        when(transferAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_TRANSACTION_ID, NOTIFICATION_TYPE_REFUND_SUCCESS))
                .thenReturn(false);

        // When
        transferAlertService.notifyRefundSuccess(TEST_USER_ID, response);

        // Then
        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());

        Map<String, Object> placeholders = captor.getValue().getPlaceholders();
        // DB 템플릿에는 {amount}만 존재하므로 amount만 전달됨
        assertEquals(1, placeholders.size());
        assertTrue(placeholders.containsKey("amount"));
        assertFalse(placeholders.containsKey("merchant"));
        assertFalse(placeholders.containsKey("date"));
    }

    // ================================================================
    // 9. 자동 충전 WALLET_CHARGE_SUCCESS 알림 테스트
    // ================================================================

    @Test
    @DisplayName("자동 충전 성공 → WALLET_CHARGE_SUCCESS 알림 생성")
    void notifyAutoChargeSuccess_createsNotification() {
        // Given
        when(transferAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_TRANSACTION_ID, NOTIFICATION_TYPE_CHARGE_SUCCESS))
                .thenReturn(false);

        // When
        transferAlertService.notifyChargeSuccess(TEST_USER_ID, TEST_TRANSACTION_ID, BigDecimal.valueOf(10000));

        // Then
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
        // Given
        when(transferAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_TRANSACTION_ID, NOTIFICATION_TYPE_CHARGE_SUCCESS))
                .thenReturn(false);

        // When
        transferAlertService.notifyChargeSuccess(TEST_USER_ID, TEST_TRANSACTION_ID, BigDecimal.valueOf(15000));

        // Then
        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());
        assertEquals(BigDecimal.valueOf(15000), captor.getValue().getPlaceholders().get("amount"));
    }

    @Test
    @DisplayName("자동 충전 - notificationType이 WALLET_CHARGE_SUCCESS")
    void notifyAutoChargeSuccess_notificationType_correct() {
        // Given
        when(transferAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_TRANSACTION_ID, NOTIFICATION_TYPE_CHARGE_SUCCESS))
                .thenReturn(false);

        // When
        transferAlertService.notifyChargeSuccess(TEST_USER_ID, TEST_TRANSACTION_ID, BigDecimal.valueOf(10000));

        // Then
        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());
        assertEquals(NOTIFICATION_TYPE_CHARGE_SUCCESS, captor.getValue().getNotificationType());
    }

    @Test
    @DisplayName("자동 충전 - category가 TRANSFER_NOTIFY")
    void notifyAutoChargeSuccess_category_correct() {
        // Given
        when(transferAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_TRANSACTION_ID, NOTIFICATION_TYPE_CHARGE_SUCCESS))
                .thenReturn(false);

        // When
        transferAlertService.notifyChargeSuccess(TEST_USER_ID, TEST_TRANSACTION_ID, BigDecimal.valueOf(10000));

        // Then
        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());
        assertEquals(NotificationCategory.TRANSFER_NOTIFY, captor.getValue().getCategory());
    }

    @Test
    @DisplayName("자동 충전 - referenceType=TRANSACTION, referenceId=자동충전 거래 ID")
    void notifyAutoChargeSuccess_referenceCorrect() {
        // Given
        Long autoChargeTxId = 999L;
        when(transferAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, autoChargeTxId, NOTIFICATION_TYPE_CHARGE_SUCCESS))
                .thenReturn(false);

        // When
        transferAlertService.notifyChargeSuccess(TEST_USER_ID, autoChargeTxId, BigDecimal.valueOf(10000));

        // Then
        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());
        assertEquals(REFERENCE_TYPE, captor.getValue().getReferenceType());
        assertEquals(autoChargeTxId, captor.getValue().getReferenceId());
    }

    @Test
    @DisplayName("자동 충전 - 동일 거래 + 동일 notificationType → 중복 생성하지 않음")
    void notifyAutoChargeSuccess_duplicate_doesNotCreate() {
        // Given
        when(transferAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_TRANSACTION_ID, NOTIFICATION_TYPE_CHARGE_SUCCESS))
                .thenReturn(true);

        // When
        transferAlertService.notifyChargeSuccess(TEST_USER_ID, TEST_TRANSACTION_ID, BigDecimal.valueOf(10000));

        // Then
        verify(notificationCreateService, never()).createNotification(any(), any());
    }

    @Test
    @DisplayName("자동 충전 - 서로 다른 거래는 각각 알림 생성")
    void notifyAutoChargeSuccess_differentTransactions_createsBoth() {
        // Given
        Long txId1 = 101L;
        Long txId2 = 102L;
        when(transferAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, txId1, NOTIFICATION_TYPE_CHARGE_SUCCESS))
                .thenReturn(false);
        when(transferAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, txId2, NOTIFICATION_TYPE_CHARGE_SUCCESS))
                .thenReturn(false);

        // When
        transferAlertService.notifyChargeSuccess(TEST_USER_ID, txId1, BigDecimal.valueOf(10000));
        transferAlertService.notifyChargeSuccess(TEST_USER_ID, txId2, BigDecimal.valueOf(15000));

        // Then
        verify(notificationCreateService, times(2)).createNotification(eq(TEST_USER_ID), any());
    }

    @Test
    @DisplayName("자동 충전 - 서로 다른 사용자는 서로 영향을 주지 않음")
    void notifyAutoChargeSuccess_differentUsers_independentDuplicateCheck() {
        // Given
        when(transferAlertMapper.existsNotificationByReference(
                200L, REFERENCE_TYPE, TEST_TRANSACTION_ID, NOTIFICATION_TYPE_CHARGE_SUCCESS))
                .thenReturn(false);

        // When
        transferAlertService.notifyChargeSuccess(200L, TEST_TRANSACTION_ID, BigDecimal.valueOf(10000));

        // Then
        verify(notificationCreateService, times(1)).createNotification(eq(200L), any());
        verify(transferAlertMapper, times(1)).existsNotificationByReference(
                eq(200L), eq(REFERENCE_TYPE), eq(TEST_TRANSACTION_ID), eq(NOTIFICATION_TYPE_CHARGE_SUCCESS));
    }

    @Test
    @DisplayName("자동 충전 - transactionId가 null이면 알림 미생성")
    void notifyAutoChargeSuccess_nullTransactionId_doesNotCreate() {
        transferAlertService.notifyChargeSuccess(TEST_USER_ID, null, BigDecimal.valueOf(10000));
        verify(notificationCreateService, never()).createNotification(any(), any());
    }

    @Test
    @DisplayName("자동 충전 - placeholder가 amount만 전달됨")
    void notifyAutoChargeSuccess_onlyAmountPlaceholder() {
        // Given
        when(transferAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_TRANSACTION_ID, NOTIFICATION_TYPE_CHARGE_SUCCESS))
                .thenReturn(false);

        // When
        transferAlertService.notifyChargeSuccess(TEST_USER_ID, TEST_TRANSACTION_ID, BigDecimal.valueOf(10000));

        // Then
        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());

        Map<String, Object> placeholders = captor.getValue().getPlaceholders();
        assertEquals(1, placeholders.size());
        assertTrue(placeholders.containsKey("amount"));
        assertFalse(placeholders.containsKey("merchant"));
    }

    @Test
    @DisplayName("자동 충전 - 중요 알림")
    void notifyAutoChargeSuccess_isImportant() {
        // Given
        when(transferAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, TEST_TRANSACTION_ID, NOTIFICATION_TYPE_CHARGE_SUCCESS))
                .thenReturn(false);

        // When
        transferAlertService.notifyChargeSuccess(TEST_USER_ID, TEST_TRANSACTION_ID, BigDecimal.valueOf(10000));

        // Then
        ArgumentCaptor<NotificationCreateRequestDTO> captor =
                ArgumentCaptor.forClass(NotificationCreateRequestDTO.class);
        verify(notificationCreateService).createNotification(eq(TEST_USER_ID), captor.capture());
        assertTrue(captor.getValue().getImportant());
    }

    @Test
    @DisplayName("자동 충전과 수동 충전은 별도 알림으로 생성 (각각 transactionId가 다름)")
    void autoCharge_and_manualCharge_areIndependentNotifications() {
        // Given - 수동 충전
        ChargeResponse response = createChargeResponse(1L, BigDecimal.valueOf(50000));
        when(transferAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, 1L, NOTIFICATION_TYPE_CHARGE_SUCCESS))
                .thenReturn(false);

        // Given - 자동 충전 (별도 transactionId)
        Long autoChargeTxId = 2L;
        when(transferAlertMapper.existsNotificationByReference(
                TEST_USER_ID, REFERENCE_TYPE, autoChargeTxId, NOTIFICATION_TYPE_CHARGE_SUCCESS))
                .thenReturn(false);

        // When
        transferAlertService.notifyChargeSuccess(TEST_USER_ID, response);
        transferAlertService.notifyChargeSuccess(TEST_USER_ID, autoChargeTxId, BigDecimal.valueOf(10000));

        // Then - 각각 별도 알림 생성
        verify(notificationCreateService, times(2)).createNotification(eq(TEST_USER_ID), any());
    }
}
