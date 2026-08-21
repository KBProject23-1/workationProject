package com.workit.domain.payment.service;

import com.workit.domain.account.mapper.AccountMapper;
import com.workit.domain.account.vo.BankAccountVO;
import com.workit.domain.card.mapper.CardMapper;
import com.workit.domain.card.vo.CardVO;
import com.workit.domain.expense.service.ExpenseImportTrigger;
import com.workit.domain.ledger.service.LedgerService;
import com.workit.domain.ledger.vo.LedgerEntryVO;
import com.workit.domain.payment.TransactionStatus;
import com.workit.domain.payment.gateway.PaymentGatewayClient;
import com.workit.domain.payment.gateway.PgAuthResult;
import com.workit.domain.payment.gateway.PgException;
import com.workit.domain.security.service.PinValidationResult;
import com.workit.domain.security.service.PinValidator;
import com.workit.domain.transaction.dto.request.PaymentRequest;
import com.workit.domain.transaction.dto.response.CancelResponse;
import com.workit.domain.transaction.dto.response.PaymentResponse;
import com.workit.domain.transaction.exception.TransactionErrorCode;
import com.workit.domain.transaction.mapper.TransactionMapper;
import com.workit.domain.transaction.vo.TransactionVO;
import com.workit.domain.wallet.dto.request.ChargeRequest;
import com.workit.domain.wallet.dto.request.RefundRequest;
import com.workit.domain.wallet.dto.response.ChargeResponse;
import com.workit.domain.wallet.dto.response.RefundResponse;
import com.workit.domain.wallet.exception.WalletErrorCode;
import com.workit.domain.wallet.mapper.WalletMapper;
import com.workit.domain.wallet.vo.WalletVO;
import com.workit.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.workit.global.constant.PaymentPolicy.MIN_CHARGE_AMOUNT;
import static com.workit.global.constant.PaymentPolicy.MAX_TRANSACTION_AMOUNT;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 결제 오케스트레이션 도메인 서비스.
 * 충전/환불/결제의 공통 흐름(REQUESTED insert → 자금 이동 → 원장 post → PAID 전이)을 payment 도메인이 소유한다.
 * (기존 WalletServiceImpl.charge/refund + TransactionServiceImpl.pay 를 이관 — behavior-neutral)
 *
 * 에러코드는 FE 계약이라 그대로 보존: 충전/환불은 WalletErrorCode, 결제는 TransactionErrorCode 를 사용하며
 * PIN 검증도 두 계약을 유지하기 위해 validateWalletPin / validatePaymentPin 두 변형을 둔다.
 */
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final WalletMapper walletMapper;
    private final AccountMapper accountMapper;
    private final CardMapper cardMapper;
    private final TransactionMapper transactionMapper;
    private final LedgerService ledgerService;
    private final PaymentGatewayClient paymentGatewayClient;
    private final PinValidator pinValidator;
    private final PaymentTransactionRecorder paymentTransactionRecorder;
    private final ExpenseImportTrigger expenseImportTrigger;
    private final PaymentAlertService paymentAlertService;

    // ===== 충전 =====
    @Override
    @Transactional
    public ChargeResponse charge(Long userId, ChargeRequest request) {

        validateChargeRequest(request);
        validateWalletPin(userId, request.getDeviceId(), request.getPinNumber());

        BigDecimal amount = request.getAmount();

        WalletVO wallet = walletMapper.findByUserId(userId);
        if (wallet == null) {
            throw new BusinessException(WalletErrorCode.WALLET_NOT_FOUND);
        }

        BankAccountVO account = accountMapper.findAccountById(request.getAccountId(), userId);
        if (account == null) {
            throw new BusinessException(WalletErrorCode.WALLET_ACCOUNT_NOT_FOUND);
        }

        // 1) REQUESTED 상태로 거래 생성 — 멱등키 중복이면 잔액이동 전에 가장 먼저 거부(409)
        TransactionVO chargeTx = TransactionVO.forWalletCharge(userId, wallet, account, amount, request.getIdempotencyKey());
        try {
            transactionMapper.insertTransaction(chargeTx);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(WalletErrorCode.WALLET_DUPLICATE_REQUEST);
        }

        // 2) 잔액 이동 (계좌 차감 -> 지갑 적립). 실패 시 예외로 트랜잭션 전체 롤백(REQUESTED insert 포함)
        int accountUpdatedRows = accountMapper.decreaseBalance(request.getAccountId(), userId, amount);
        if (accountUpdatedRows == 0) {
            throw new BusinessException(WalletErrorCode.WALLET_INSUFFICIENT_ACCOUNT_BALANCE);
        }
        walletMapper.increaseBalance(userId, amount);

        // 3) 복식부기 원장 기입: BANK DEBIT(나감) / WALLET CREDIT(들어옴)
        // 동시성 하에서 실제 balance 와 어긋나지 않도록 update 후 재조회한 값을 쓴다 (사전 스냅샷 값 X)
        BigDecimal accountBalanceAfter = accountMapper.findAccountById(request.getAccountId(), userId).getBalance();
        BigDecimal walletBalanceAfter = walletMapper.findByUserId(userId).getBalance();
        ledgerService.post(chargeTx.getId(),
                LedgerEntryVO.debit(LedgerEntryVO.ACCOUNT_BANK, account.getId(), amount, accountBalanceAfter),
                LedgerEntryVO.credit(LedgerEntryVO.ACCOUNT_WALLET, wallet.getId(), amount, walletBalanceAfter));

        // 4) PAID 로 전이 (상태머신 규칙 검증 포함)
        markPaid(chargeTx, userId);

        return ChargeResponse.of(chargeTx, walletBalanceAfter);
    }

    // ===== 환불 =====
    @Override
    @Transactional
    public RefundResponse refund(Long userId, RefundRequest request) {

        validateRefundRequest(request);
        validateWalletPin(userId, request.getDeviceId(), request.getPinNumber());

        BigDecimal amount = request.getAmount();

        WalletVO wallet = walletMapper.findByUserId(userId);
        if (wallet == null) {
            throw new BusinessException(WalletErrorCode.WALLET_NOT_FOUND);
        }

        BankAccountVO targetAccount = accountMapper.findAccountById(request.getAccountId(), userId);
        if (targetAccount == null) {
            throw new BusinessException(WalletErrorCode.WALLET_ACCOUNT_NOT_FOUND);
        }

        // 1) REQUESTED 상태로 거래 생성 — 멱등키 중복이면 잔액이동 전에 먼저 거부(409)
        TransactionVO refundTx = TransactionVO.forWalletRefund(userId, wallet, targetAccount, amount, request.getIdempotencyKey());
        try {
            transactionMapper.insertTransaction(refundTx);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(WalletErrorCode.WALLET_DUPLICATE_REQUEST);
        }

        // 2) 잔액 이동 (지갑 차감 -> 계좌 적립)
        int walletUpdatedRows = walletMapper.decreaseBalance(userId, amount);
        if (walletUpdatedRows == 0) {
            throw new BusinessException(WalletErrorCode.WALLET_INSUFFICIENT_BALANCE);
        }
        int accountUpdatedRows = accountMapper.increaseBalance(targetAccount.getId(), userId, amount);
        if (accountUpdatedRows == 0) {
            throw new BusinessException(WalletErrorCode.WALLET_ACCOUNT_STATE_INVALID);
        }

        // 3) 복식부기 원장 기입: WALLET DEBIT(나감) / BANK CREDIT(들어옴)
        // 동시성 하에서 실제 balance 와 어긋나지 않도록 update 후 재조회한 값을 쓴다 (사전 스냅샷 값 X)
        BigDecimal walletBalanceAfter = walletMapper.findByUserId(userId).getBalance();
        BigDecimal accountBalanceAfter = accountMapper.findAccountById(targetAccount.getId(), userId).getBalance();
        ledgerService.post(refundTx.getId(),
                LedgerEntryVO.debit(LedgerEntryVO.ACCOUNT_WALLET, wallet.getId(), amount, walletBalanceAfter),
                LedgerEntryVO.credit(LedgerEntryVO.ACCOUNT_BANK, targetAccount.getId(), amount, accountBalanceAfter));

        // 4) PAID 로 전이 (상태머신 규칙 검증 포함)
        markPaid(refundTx, userId);

        return RefundResponse.of(refundTx, walletBalanceAfter, targetAccount);
    }

    // ===== 결제 =====
    // 메서드 전체를 @Transactional 로 감싸지 않는다 — 카드 분기(payWithCard)의 PG 호출을
    // DB 트랜잭션 밖에서 수행해야 하기 때문이다(이유는 PaymentTransactionRecorder 클래스 주석 참고).
    // 지갑 분기는 그 자체로 원자적이어야 하므로 PaymentTransactionRecorder.payWithWallet(하나의 @Transactional)에 위임한다.
    @Override
    public PaymentResponse pay(Long userId, PaymentRequest request) {

        validatePaymentRequest(request);
        validatePaymentPin(userId, request.getDeviceId(), request.getPinNumber());

        PaymentResponse response = "WALLET".equals(request.getPaymentSourceType())
                ? paymentTransactionRecorder.payWithWallet(userId, request)
                : payWithCard(userId, request);

        // 결제 성공 알림 생성 (PAID 상태 확정 후)
        paymentAlertService.notifyPaymentSuccess(userId, response);

        // 결제가 커밋된 뒤에 워케이션 지출로 옮긴다.
        // 이 메서드에는 트랜잭션이 없어 유입은 자체 트랜잭션으로 돌고, 실패해도 결제에 영향이 없다
        expenseImportTrigger.onPaymentCompleted(userId);

        return response;
    }

    // 카드결제(PG 2단계). DB 기입은 PaymentTransactionRecorder 의 짧은 개별 트랜잭션들로 분리하고,
    // PG authorize()/capture() 호출은 그 사이에서 트랜잭션 없이 수행한다.
    private PaymentResponse payWithCard(Long userId, PaymentRequest request) {
        if (request.getCardId() == null) {
            throw new BusinessException(TransactionErrorCode.TRANSACTION_CARD_ID_REQUIRED);
        }

        CardVO card = cardMapper.findCardById(request.getCardId(), userId);
        if (card == null) {
            throw new BusinessException(TransactionErrorCode.TRANSACTION_CARD_NOT_FOUND);
        }

        // 사용자가 고른 값이 먼저다. 안 골랐으면 법인카드일 때만 업무로 보고, 개인카드는 미선택으로 남긴다
        Boolean isBusinessExpense = request.getIsBusinessExpense() != null
                ? request.getIsBusinessExpense()
                : ("WORK".equals(card.getCardType()) ? Boolean.TRUE : null);
        BigDecimal amount = request.getAmount();

        // 1) REQUESTED 카드결제 거래 생성 (멱등키 중복 -> 409). 짧은 트랜잭션으로 PG 호출 전에 즉시 커밋.
        TransactionVO paymentTx = TransactionVO.forCardPayment(userId, card, request, isBusinessExpense);
        paymentTransactionRecorder.assignMerchantCategory(paymentTx);
        paymentTransactionRecorder.createRequestedCardPayment(paymentTx);

        // 2) PG 승인(authorize) — 트랜잭션 밖. 실패 시 REQUESTED -> FAILED 로 명시적으로 종료.
        PgAuthResult auth;
        try {
            auth = paymentGatewayClient.authorize(userId, card.getId(), amount, request.getMerchantName());
        } catch (PgException e) {
            paymentTransactionRecorder.markCardPaymentFailed(paymentTx, userId);
            throw new BusinessException(TransactionErrorCode.TRANSACTION_PG_AUTH_FAILED);
        }
        // REQUESTED -> AUTHORIZED 전이 + 승인번호/PG거래ID 기록 (짧은 트랜잭션).
        // PG 승인은 이미 성공했으므로, 이 DB 기록 자체가 실패(데드락 등)해도 그냥 던지면 안 된다 —
        // 예외가 그대로 올라가면 DeadlockRetrier 가 pay() 를 통째로 재시도하는데, 이번엔 멱등키가
        // 이미 첫 시도에서 커밋돼 있어 409만 뜨고 거래는 영원히 REQUESTED 에 멈춘 채 PG 승인만 살아남는다.
        // 승인 취소(cancel)로 보상 후 FAILED 로 명시적으로 종료해 이 상태를 막는다.
        try {
            paymentTransactionRecorder.recordCardAuthorization(paymentTx, userId, auth);
        } catch (RuntimeException e) {
            paymentGatewayClient.cancel(auth.getPgTransactionId());
            paymentTransactionRecorder.markCardPaymentFailed(paymentTx, userId);
            throw new BusinessException(TransactionErrorCode.TRANSACTION_PROCESSING_FAILED);
        }

        // 3) PG 매입(capture) — 트랜잭션 밖. 실패 시 승인 취소(cancel) 보상 후 AUTHORIZED -> FAILED 로 종료.
        try {
            paymentGatewayClient.capture(auth.getPgTransactionId(), amount);
        } catch (PgException e) {
            paymentGatewayClient.cancel(auth.getPgTransactionId());
            paymentTransactionRecorder.markCardPaymentFailed(paymentTx, userId);
            throw new BusinessException(TransactionErrorCode.TRANSACTION_PG_CAPTURE_FAILED);
        }

        // 4) 원장 기입(CARD DEBIT / MERCHANT CREDIT) + PAID 전이. 짧은 트랜잭션.
        // 매입(capture)도 이미 성공했으므로, 위와 동일한 이유로 실패 시 승인 취소로 보상 후 FAILED 종료.
        try {
            paymentTransactionRecorder.recordCardCaptureAndComplete(paymentTx, userId, amount);
        } catch (RuntimeException e) {
            paymentGatewayClient.cancel(auth.getPgTransactionId());
            paymentTransactionRecorder.markCardPaymentFailed(paymentTx, userId);
            throw new BusinessException(TransactionErrorCode.TRANSACTION_PROCESSING_FAILED);
        }

        return PaymentResponse.ofCard(paymentTx);
    }

    // ===== 결제 취소 =====
    // 카드결제 취소의 PG cancel() 호출(아래)도 payWithCard 와 같은 이유로 트랜잭션 밖에 두는 게 이상적이지만,
    // 이 메서드의 유일한 호출부인 ReservationServiceImpl.saveReservationCancellation() 이 이미 자체
    // @Transactional 로 감싸고 있어(REQUIRES_NEW 아닌 기본 전파) 여기서만 분리해도 실제로는 그 바깥 트랜잭션에
    // 합류돼 효과가 없다. 온전히 고치려면 reservation 도메인 호출부까지 같이 바꿔야 해서 이번 범위에서는 제외.
    @Override
    @Transactional
    public CancelResponse cancelPayment(Long userId, Long transactionId) {

        TransactionVO tx = transactionMapper.findTransactionForCancel(transactionId, userId);
        if (tx == null) {
            throw new BusinessException(TransactionErrorCode.TRANSACTION_NOT_FOUND);
        }
        if (!"PAYMENT".equals(tx.getTransactionType())) {
            throw new BusinessException(TransactionErrorCode.TRANSACTION_CANCEL_NOT_ALLOWED);
        }

        // 1) 동시 이중취소 차단: PAID 일 때만 CANCELED 로 원자적 전이. rows==0 이면 이미 취소됨(경합 패자) → 환불 안 함.
        int claimed = transactionMapper.cancelTransaction(transactionId, userId);
        if (claimed == 0) {
            throw new BusinessException(TransactionErrorCode.TRANSACTION_ALREADY_CANCELED);
        }

        // 2) 환불 + 원장 역기입 (돈은 온 곳으로). 원거래에 seq 를 이어붙여 zero-sum 유지.
        BigDecimal amount = tx.getAmount();
        String refundedTo;
        if ("WALLET".equals(tx.getPaymentSourceType())) {
            // 지갑결제 취소 → 지갑 환불 + 역기입(MERCHANT DEBIT / WALLET CREDIT)
            walletMapper.increaseBalance(userId, amount);
            WalletVO wallet = walletMapper.findByUserId(userId);
            ledgerService.post(transactionId,
                    LedgerEntryVO.debit(LedgerEntryVO.ACCOUNT_MERCHANT, tx.getMerchantId(), amount, null),
                    LedgerEntryVO.credit(LedgerEntryVO.ACCOUNT_WALLET, wallet.getId(), amount, wallet.getBalance()));
            refundedTo = "WALLET";
        } else {
            // 카드결제 취소 → PG 취소(카드로 환불) + 역기입(MERCHANT DEBIT / CARD CREDIT). 내부 잔액 이동 없음.
            if (tx.getPgTransactionId() != null) {
                paymentGatewayClient.cancel(tx.getPgTransactionId());
            }
            ledgerService.post(transactionId,
                    LedgerEntryVO.debit(LedgerEntryVO.ACCOUNT_MERCHANT, tx.getMerchantId(), amount, null),
                    LedgerEntryVO.credit(LedgerEntryVO.ACCOUNT_CARD, tx.getCardId(), amount, null));
            refundedTo = "CARD";
        }

        TransactionVO cancelled = transactionMapper.findTransactionForCancel(transactionId, userId);
        return CancelResponse.of(cancelled, amount, refundedTo);
    }

    // ===== 상태 전이 (상태머신 규칙 강제) =====
    /** REQUESTED/AUTHORIZED -> PAID 전이를 규칙 검증과 함께 수행. */
    private void markPaid(TransactionVO tx, Long userId) {
        assertTransition(tx.getStatus(), TransactionStatus.PAID);
        LocalDateTime approvedAt = LocalDateTime.now();
        transactionMapper.updateStatus(tx.getId(), userId, TransactionStatus.PAID.name(), approvedAt);
        tx.setStatus(TransactionStatus.PAID.name());
        tx.setApprovedAt(approvedAt);
    }

    /** 현재 상태에서 target 전이가 상태머신 규칙상 합법인지 검증. 위반 시 롤백(프로그래밍 오류). */
    private void assertTransition(String currentStatus, TransactionStatus target) {
        if (!TransactionStatus.from(currentStatus).canTransitionTo(target)) {
            throw new IllegalStateException("불법 상태 전이: " + currentStatus + " -> " + target);
        }
    }

    // ===== 검증 (에러코드 계약 보존) =====
    private void validateChargeRequest(ChargeRequest request) {
        if (request.getAccountId() == null) {
            throw new BusinessException(WalletErrorCode.WALLET_ACCOUNT_ID_REQUIRED);
        }
        if (request.getAmount() == null) {
            throw new BusinessException(WalletErrorCode.WALLET_AMOUNT_REQUIRED);
        }
        if (request.getPinNumber() == null) {
            throw new BusinessException(WalletErrorCode.WALLET_PIN_REQUIRED);
        }
        if (request.getDeviceId() == null || request.getDeviceId().trim().isEmpty()) {
            throw new BusinessException(WalletErrorCode.WALLET_DEVICE_ID_REQUIRED);
        }
        if (request.getIdempotencyKey() == null || request.getIdempotencyKey().trim().isEmpty()) {
            throw new BusinessException(WalletErrorCode.WALLET_IDEMPOTENCY_KEY_REQUIRED);
        }

        BigDecimal amount = request.getAmount();
        if (amount.compareTo(MIN_CHARGE_AMOUNT) < 0) {
            throw new BusinessException(WalletErrorCode.WALLET_MIN_CHARGE_AMOUNT_VIOLATION);
        }
        if (amount.compareTo(MAX_TRANSACTION_AMOUNT) > 0) {
            throw new BusinessException(WalletErrorCode.WALLET_MAX_TRANSACTION_AMOUNT_EXCEEDED);
        }
    }

    private void validateRefundRequest(RefundRequest request) {
        if (request.getAccountId() == null) {
            throw new BusinessException(WalletErrorCode.WALLET_ACCOUNT_ID_REQUIRED);
        }
        if (request.getAmount() == null) {
            throw new BusinessException(WalletErrorCode.WALLET_REFUND_AMOUNT_REQUIRED);
        }
        if (request.getPinNumber() == null) {
            throw new BusinessException(WalletErrorCode.WALLET_PIN_REQUIRED);
        }
        if (request.getDeviceId() == null || request.getDeviceId().trim().isEmpty()) {
            throw new BusinessException(WalletErrorCode.WALLET_DEVICE_ID_REQUIRED);
        }
        if (request.getIdempotencyKey() == null || request.getIdempotencyKey().trim().isEmpty()) {
            throw new BusinessException(WalletErrorCode.WALLET_IDEMPOTENCY_KEY_REQUIRED);
        }

        BigDecimal amount = request.getAmount();
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(WalletErrorCode.WALLET_INVALID_REFUND_AMOUNT);
        }
        if (amount.compareTo(MAX_TRANSACTION_AMOUNT) > 0) {
            throw new BusinessException(WalletErrorCode.WALLET_MAX_TRANSACTION_AMOUNT_EXCEEDED);
        }
    }

    private void validatePaymentRequest(PaymentRequest request) {
        if (request.getMerchantName() == null || request.getMerchantName().trim().isEmpty()) {
            throw new BusinessException(TransactionErrorCode.TRANSACTION_MERCHANT_NAME_REQUIRED);
        }
        if (request.getAmount() == null) {
            throw new BusinessException(TransactionErrorCode.TRANSACTION_AMOUNT_REQUIRED);
        }
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(TransactionErrorCode.TRANSACTION_INVALID_AMOUNT);
        }
        if (request.getAmount().compareTo(MAX_TRANSACTION_AMOUNT) > 0) {
            throw new BusinessException(TransactionErrorCode.TRANSACTION_MAX_AMOUNT_EXCEEDED);
        }
        if (request.getPaymentSourceType() == null) {
            throw new BusinessException(TransactionErrorCode.TRANSACTION_PAYMENT_SOURCE_TYPE_REQUIRED);
        }
        if (!"WALLET".equals(request.getPaymentSourceType()) && !"CARD".equals(request.getPaymentSourceType())) {
            throw new BusinessException(TransactionErrorCode.TRANSACTION_PAYMENT_SOURCE_TYPE_INVALID);
        }
        if (request.getPinNumber() == null) {
            throw new BusinessException(TransactionErrorCode.TRANSACTION_PIN_REQUIRED);
        }
        if (request.getDeviceId() == null || request.getDeviceId().trim().isEmpty()) {
            throw new BusinessException(TransactionErrorCode.TRANSACTION_DEVICE_ID_REQUIRED);
        }
        if (request.getIdempotencyKey() == null || request.getIdempotencyKey().trim().isEmpty()) {
            throw new BusinessException(TransactionErrorCode.TRANSACTION_IDEMPOTENCY_KEY_REQUIRED);
        }
    }

    /** 충전/환불 PIN 검증 — WalletErrorCode 계약. */
    private void validateWalletPin(Long userId, String deviceId, String pinNumber) {
        if (pinNumber == null || pinNumber.length() != 6) {
            throw new BusinessException(WalletErrorCode.WALLET_PIN_INVALID);
        }
        PinValidationResult result = pinValidator.validate(userId, deviceId, pinNumber);
        switch (result) {
            case DEVICE_NOT_REGISTERED:
                throw new BusinessException(WalletErrorCode.WALLET_PIN_NOT_REGISTERED);
            case LOCKED:
                throw new BusinessException(WalletErrorCode.WALLET_PIN_LOCKED);
            case MISMATCH:
                throw new BusinessException(WalletErrorCode.WALLET_PIN_INVALID);
            default:
                // VALID
        }
    }

    /** 결제 PIN 검증 — TransactionErrorCode 계약. */
    private void validatePaymentPin(Long userId, String deviceId, String pinNumber) {
        if (pinNumber == null || pinNumber.length() != 6) {
            throw new BusinessException(TransactionErrorCode.TRANSACTION_PIN_INVALID);
        }
        PinValidationResult result = pinValidator.validate(userId, deviceId, pinNumber);
        switch (result) {
            case DEVICE_NOT_REGISTERED:
                throw new BusinessException(TransactionErrorCode.TRANSACTION_PIN_NOT_REGISTERED);
            case LOCKED:
                throw new BusinessException(TransactionErrorCode.TRANSACTION_PIN_LOCKED);
            case MISMATCH:
                throw new BusinessException(TransactionErrorCode.TRANSACTION_PIN_INVALID);
            default:
                // VALID
        }
    }
}
