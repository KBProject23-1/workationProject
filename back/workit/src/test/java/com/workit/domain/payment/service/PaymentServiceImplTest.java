package com.workit.domain.payment.service;

import com.workit.domain.account.mapper.AccountMapper;
import com.workit.domain.account.vo.BankAccountVO;
import com.workit.domain.card.mapper.CardMapper;
import com.workit.domain.card.vo.CardVO;
import com.workit.domain.ledger.service.LedgerService;
import com.workit.domain.payment.gateway.PaymentGatewayClient;
import com.workit.domain.payment.gateway.PgAuthResult;
import com.workit.domain.payment.gateway.PgException;
import com.workit.domain.security.service.PinValidationResult;
import com.workit.domain.security.service.PinValidator;
import com.workit.domain.transaction.dto.request.PaymentRequest;
import com.workit.domain.transaction.exception.TransactionErrorCode;
import com.workit.domain.transaction.mapper.TransactionMapper;
import com.workit.domain.transaction.vo.TransactionVO;
import com.workit.domain.wallet.dto.request.ChargeRequest;
import com.workit.domain.wallet.exception.WalletErrorCode;
import com.workit.domain.wallet.mapper.WalletMapper;
import com.workit.domain.wallet.vo.WalletVO;
import com.workit.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PaymentServiceImpl 오케스트레이션 단위 테스트 (Mockito).
 *
 * 매퍼/PG/PIN 을 목으로 갈아끼워, DB 없이 다음 규칙을 검증한다:
 *   1) 멱등키 중복 시 잔액 이동 전에 409 로 거부
 *   2) 이중 취소 방지 (원자적 claim 실패 시 ALREADY_CANCELED)
 *   3) 카드 매입 실패 시 승인 취소(cancel)로 보상 후 예외
 *
 * 자동충전 금액 계산 / 가맹점 카테고리 스냅샷 / 지갑결제 멱등키 중복은
 * payWithWallet 로직이 PaymentTransactionRecorder 로 이관되면서 PaymentTransactionRecorderTest 로 옮겨졌다.
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock private WalletMapper walletMapper;
    @Mock private AccountMapper accountMapper;
    @Mock private CardMapper cardMapper;
    @Mock private TransactionMapper transactionMapper;
    @Mock private LedgerService ledgerService;
    @Mock private PaymentGatewayClient paymentGatewayClient;
    @Mock private PinValidator pinValidator;
    // 지갑결제(payWithWallet)/카드결제 DB 기입은 PaymentTransactionRecorder 로 이관됨 — 그 자체 로직은
    // PaymentTransactionRecorderTest 에서 검증. 여기서는 mock 으로 두고 카드결제 실패 시 PG 호출 순서만 본다.
    @Mock private PaymentTransactionRecorder paymentTransactionRecorder;

    @InjectMocks private PaymentServiceImpl paymentService;

    private static final Long USER_ID = 1L;
    private static final String DEVICE = "device-1";
    private static final String PIN = "123456";

    // ---------- 헬퍼 ----------
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

    private CardVO card(long id, String cardType) {
        CardVO c = new CardVO();
        c.setId(id);
        c.setUserId(USER_ID);
        c.setCardType(cardType);
        return c;
    }

    private PaymentRequest cardPayment(long amount, long cardId) {
        PaymentRequest r = new PaymentRequest();
        r.setMerchantName("테스트가맹점");
        r.setMerchantId(50L);
        r.setAmount(BigDecimal.valueOf(amount));
        r.setPaymentSourceType("CARD");
        r.setCardId(cardId);
        r.setPinNumber(PIN);
        r.setDeviceId(DEVICE);
        r.setIdempotencyKey("idem-card-" + amount);
        return r;
    }

    private ChargeRequest charge(long accountId, long amount) {
        ChargeRequest r = new ChargeRequest();
        r.setAccountId(accountId);
        r.setAmount(BigDecimal.valueOf(amount));
        r.setPinNumber(PIN);
        r.setDeviceId(DEVICE);
        r.setIdempotencyKey("idem-charge");
        return r;
    }

    private void pinValid() {
        when(pinValidator.validate(eq(USER_ID), anyString(), anyString())).thenReturn(PinValidationResult.VALID);
    }

    // 자동충전 금액 계산 / 가맹점 카테고리 스냅샷 / 지갑결제 멱등키 중복 테스트는
    // payWithWallet 로직 자체가 PaymentTransactionRecorder 로 이관되면서 PaymentTransactionRecorderTest 로 이동.

    // ============================================================
    // 2) 멱등키 중복 → 409
    // ============================================================
    @Test
    @DisplayName("충전: 멱등키 중복(UNIQUE 위반)이면 잔액 이동 전에 409 로 거부한다")
    void 충전_멱등키중복이면_DUPLICATE_REQUEST() {
        pinValid();
        when(walletMapper.findByUserId(USER_ID)).thenReturn(wallet(10L, 0));
        when(accountMapper.findAccountById(30L, USER_ID)).thenReturn(account(30L, 1_000_000));
        doThrow(new DuplicateKeyException("duplicate idempotency_key"))
                .when(transactionMapper).insertTransaction(any(TransactionVO.class));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> paymentService.charge(USER_ID, charge(30L, 10_000)));

        assertEquals(WalletErrorCode.WALLET_DUPLICATE_REQUEST, ex.getErrorCode());
    }

    // ============================================================
    // 3) 이중 취소 방지
    // ============================================================
    @Test
    @DisplayName("취소: 원자적 claim 이 0건이면(이미 취소됨) ALREADY_CANCELED, 환불하지 않는다")
    void 취소_이미취소된건이면_ALREADY_CANCELED() {
        TransactionVO paid = new TransactionVO();
        paid.setTransactionType("PAYMENT");
        paid.setPaymentSourceType("WALLET");
        paid.setStatus("PAID");
        when(transactionMapper.findTransactionForCancel(100L, USER_ID)).thenReturn(paid);
        when(transactionMapper.cancelTransaction(100L, USER_ID)).thenReturn(0); // 경합 패자

        BusinessException ex = assertThrows(BusinessException.class,
                () -> paymentService.cancelPayment(USER_ID, 100L));

        assertEquals(TransactionErrorCode.TRANSACTION_ALREADY_CANCELED, ex.getErrorCode());
    }

    @Test
    @DisplayName("취소: 대상 거래가 없으면 NOT_FOUND")
    void 취소_대상거래없으면_NOT_FOUND() {
        when(transactionMapper.findTransactionForCancel(999L, USER_ID)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> paymentService.cancelPayment(USER_ID, 999L));

        assertEquals(TransactionErrorCode.TRANSACTION_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("취소: 결제(PAYMENT) 건이 아니면 CANCEL_NOT_ALLOWED")
    void 취소_결제건이아니면_CANCEL_NOT_ALLOWED() {
        TransactionVO deposit = new TransactionVO();
        deposit.setTransactionType("DEPOSIT"); // 충전 건은 취소 대상 아님
        when(transactionMapper.findTransactionForCancel(200L, USER_ID)).thenReturn(deposit);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> paymentService.cancelPayment(USER_ID, 200L));

        assertEquals(TransactionErrorCode.TRANSACTION_CANCEL_NOT_ALLOWED, ex.getErrorCode());
    }

    // ============================================================
    // 4) 카드 매입 실패 → 승인 취소(보상) 후 예외
    // ============================================================
    @Test
    @DisplayName("카드결제: 매입(capture) 실패 시 승인(cancel)으로 보상하고 PG_CAPTURE_FAILED 를 던진다")
    void 카드결제_매입실패시_승인취소로_보상하고_예외를_던진다() {
        pinValid();
        when(cardMapper.findCardById(8L, USER_ID)).thenReturn(card(8L, "PERSONAL"));
        when(paymentGatewayClient.authorize(eq(USER_ID), eq(8L), any(BigDecimal.class), anyString()))
                .thenReturn(PgAuthResult.of("pgtx-1", "APV-0001"));
        doThrow(new PgException(PgException.Type.CAPTURE_FAILED, "capture failed"))
                .when(paymentGatewayClient).capture(eq("pgtx-1"), any(BigDecimal.class));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> paymentService.pay(USER_ID, cardPayment(5_000, 8L)));

        assertEquals(TransactionErrorCode.TRANSACTION_PG_CAPTURE_FAILED, ex.getErrorCode());
        // 핵심: 승인만 되고 매입이 실패했으므로 반드시 승인 취소(보상)가 호출되어야 한다 — 부분성공을 남기지 않음
        verify(paymentGatewayClient).cancel("pgtx-1");
    }
}
