package com.workit.domain.payment.service;

import com.workit.domain.account.mapper.AccountMapper;
import com.workit.domain.account.vo.BankAccountVO;
import com.workit.domain.ledger.service.LedgerService;
import com.workit.domain.transaction.dto.request.PaymentRequest;
import com.workit.domain.transaction.dto.response.PaymentResponse;
import com.workit.domain.transaction.exception.TransactionErrorCode;
import com.workit.domain.transaction.mapper.TransactionMapper;
import com.workit.domain.transaction.vo.TransactionVO;
import com.workit.domain.wallet.mapper.WalletMapper;
import com.workit.domain.wallet.service.TransferAlertService;
import com.workit.domain.wallet.vo.WalletVO;
import com.workit.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PaymentTransactionRecorder.payWithWallet 단위 테스트 (Mockito).
 *
 * pay() 의 WALLET 분기 로직 원자성 유지를 위해 PaymentServiceImpl 에서 이 클래스로 옮겨졌다
 * (자세한 이유는 PaymentTransactionRecorder 클래스 주석 참고).
 * 기존 PaymentServiceImplTest 의 자동충전/카테고리 스냅샷/지갑결제 멱등키 테스트를 그대로 이관.
 */
@ExtendWith(MockitoExtension.class)
class PaymentTransactionRecorderTest {

    @Mock private WalletMapper walletMapper;
    @Mock private AccountMapper accountMapper;
    @Mock private TransactionMapper transactionMapper;
    @Mock private LedgerService ledgerService;
    @Mock private TransferAlertService transferAlertService;

    @InjectMocks private PaymentTransactionRecorder recorder;

    private static final Long USER_ID = 1L;

    private WalletVO wallet(long id, long balance) {
        WalletVO w = new WalletVO();
        w.setId(id);
        w.setUserId(USER_ID);
        w.setBalance(BigDecimal.valueOf(balance));
        return w;
    }

    private BankAccountVO account(long id, long balance) {
        BankAccountVO a = new BankAccountVO();
        a.setId(id);
        a.setUserId(USER_ID);
        a.setBalance(BigDecimal.valueOf(balance));
        return a;
    }

    private PaymentRequest walletPayment(long amount) {
        PaymentRequest r = new PaymentRequest();
        r.setMerchantName("테스트가맹점");
        r.setMerchantId(50L);
        r.setAmount(BigDecimal.valueOf(amount));
        r.setPaymentSourceType("WALLET");
        r.setPinNumber("123456");
        r.setDeviceId("device-1");
        r.setIdempotencyKey("idem-" + amount);
        return r;
    }

    // ============================================================
    // 1) 자동충전 금액 계산
    // ============================================================
    @Test
    @DisplayName("자동충전: 부족분이 최소충전금액(1만원)보다 적으면 1만원으로 채운다")
    void 자동충전_부족분이_최소금액미만이면_최소금액으로_충전한다() {
        // 지갑 3,000 / 결제 5,000 → 부족분 2,000(<10,000) → 10,000 충전
        when(walletMapper.findByUserId(USER_ID)).thenReturn(wallet(10L, 3_000));
        when(accountMapper.findPrimaryAccount(USER_ID)).thenReturn(account(20L, 1_000_000));
        when(accountMapper.decreaseBalance(any(Long.class), eq(USER_ID), any(BigDecimal.class))).thenReturn(1);
        when(walletMapper.decreaseBalance(eq(USER_ID), any(BigDecimal.class))).thenReturn(1);

        PaymentResponse res = recorder.payWithWallet(USER_ID, walletPayment(5_000));

        assertTrue(res.getIsAutoCharged());
        assertEquals(0, res.getAutoChargedAmount().compareTo(BigDecimal.valueOf(10_000)),
                "부족분 2,000 이지만 최소충전 10,000 으로 채워야 한다");

        // 자동충전 알림이 WALLET_CHARGE_SUCCESS 로 생성됨
        // transactionId는 DB useGeneratedKeys로 세팅되므로, mock 에서는 null이므로 nullable 로 검증
        verify(transferAlertService).notifyChargeSuccess(
                eq(USER_ID), nullable(Long.class), eq(BigDecimal.valueOf(10_000)));
    }

    @Test
    @DisplayName("자동충전: 부족분이 최소충전금액 이상이면 부족분만큼만 충전한다")
    void 자동충전_부족분이_최소금액이상이면_부족분만큼_충전한다() {
        // 지갑 0 / 결제 15,000 → 부족분 15,000(>=10,000) → 15,000 충전
        when(walletMapper.findByUserId(USER_ID)).thenReturn(wallet(10L, 0));
        when(accountMapper.findPrimaryAccount(USER_ID)).thenReturn(account(20L, 1_000_000));
        when(accountMapper.decreaseBalance(any(Long.class), eq(USER_ID), any(BigDecimal.class))).thenReturn(1);
        when(walletMapper.decreaseBalance(eq(USER_ID), any(BigDecimal.class))).thenReturn(1);

        PaymentResponse res = recorder.payWithWallet(USER_ID, walletPayment(15_000));

        assertTrue(res.getIsAutoCharged());
        assertEquals(0, res.getAutoChargedAmount().compareTo(BigDecimal.valueOf(15_000)));

        // 자동충전 알림이 WALLET_CHARGE_SUCCESS 로 생성됨
        // transactionId는 DB useGeneratedKeys로 세팅되므로, mock 에서는 null이므로 nullable 로 검증
        verify(transferAlertService).notifyChargeSuccess(
                eq(USER_ID), nullable(Long.class), eq(BigDecimal.valueOf(15_000)));
    }

    @Test
    @DisplayName("자동충전: 지갑 잔액이 충분하면 자동충전이 일어나지 않는다")
    void 자동충전_잔액충분하면_충전하지_않는다() {
        // 지갑 10,000 / 결제 5,000 → 부족분 없음
        when(walletMapper.findByUserId(USER_ID)).thenReturn(wallet(10L, 10_000));
        when(walletMapper.decreaseBalance(eq(USER_ID), any(BigDecimal.class))).thenReturn(1);

        PaymentResponse res = recorder.payWithWallet(USER_ID, walletPayment(5_000));

        assertFalse(res.getIsAutoCharged());
        assertNull(res.getAutoChargedAmount());

        // 잔액 충분 → 자동충전 없음 → 알림 없음
        verify(transferAlertService, never()).notifyChargeSuccess(any(), nullable(Long.class), any(BigDecimal.class));
    }

    // ============================================================
    // 2) 결제 시 가맹점 카테고리 스냅샷 저장 (assignMerchantCategory)
    // ============================================================
    @Test
    @DisplayName("결제: 가맹점 카테고리를 조회해 거래의 category_assigned 에 세팅한 뒤 insert 한다")
    void 결제_가맹점카테고리를_거래에_세팅한다() {
        when(walletMapper.findByUserId(USER_ID)).thenReturn(wallet(10L, 10_000));
        when(walletMapper.decreaseBalance(eq(USER_ID), any(BigDecimal.class))).thenReturn(1);
        when(transactionMapper.findMerchantCategoryById(50L)).thenReturn("RESTAURANT");

        recorder.payWithWallet(USER_ID, walletPayment(5_000));

        ArgumentCaptor<TransactionVO> captor = ArgumentCaptor.forClass(TransactionVO.class);
        verify(transactionMapper).insertTransaction(captor.capture());
        assertEquals("RESTAURANT", captor.getValue().getCategoryAssigned(),
                "insert 되는 거래에 가맹점 카테고리가 담겨야 한다");
    }

    @Test
    @DisplayName("결제: 매칭 가맹점이 없으면 category_assigned 를 '기타'로 저장한다")
    void 결제_가맹점없으면_기타로_저장한다() {
        when(walletMapper.findByUserId(USER_ID)).thenReturn(wallet(10L, 10_000));
        when(walletMapper.decreaseBalance(eq(USER_ID), any(BigDecimal.class))).thenReturn(1);
        when(transactionMapper.findMerchantCategoryById(50L)).thenReturn(null);

        recorder.payWithWallet(USER_ID, walletPayment(5_000));

        ArgumentCaptor<TransactionVO> captor = ArgumentCaptor.forClass(TransactionVO.class);
        verify(transactionMapper).insertTransaction(captor.capture());
        assertEquals("기타", captor.getValue().getCategoryAssigned(),
                "가맹점 카테고리를 못 찾으면 '기타'로 채워야 한다");
    }

    // ============================================================
    // 3) 멱등키 중복 → 409
    // ============================================================
    @Test
    @DisplayName("지갑결제: 멱등키 중복이면 자동충전/차감 전에 409 로 거부한다")
    void 지갑결제_멱등키중복이면_DUPLICATE_REQUEST() {
        when(walletMapper.findByUserId(USER_ID)).thenReturn(wallet(10L, 0));
        doThrow(new DuplicateKeyException("duplicate idempotency_key"))
                .when(transactionMapper).insertTransaction(any(TransactionVO.class));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> recorder.payWithWallet(USER_ID, walletPayment(5_000)));

        assertEquals(TransactionErrorCode.TRANSACTION_DUPLICATE_REQUEST, ex.getErrorCode());

        // 멱등키 중복 → 자동충전 실행 안 됨 → 알림 없음
        verify(transferAlertService, never()).notifyChargeSuccess(any(), nullable(Long.class), any(BigDecimal.class));
    }
}
