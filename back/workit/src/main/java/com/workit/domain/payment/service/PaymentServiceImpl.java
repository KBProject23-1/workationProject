package com.workit.domain.payment.service;

import com.workit.domain.account.mapper.AccountMapper;
import com.workit.domain.account.vo.BankAccountVO;
import com.workit.domain.card.mapper.CardMapper;
import com.workit.domain.card.vo.CardVO;
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
        BigDecimal accountBalanceAfter = account.getBalance().subtract(amount);
        BigDecimal walletBalanceAfter = wallet.getBalance().add(amount);
        ledgerService.post(chargeTx.getId(),
                LedgerEntryVO.debit(LedgerEntryVO.ACCOUNT_BANK, account.getId(), amount, accountBalanceAfter),
                LedgerEntryVO.credit(LedgerEntryVO.ACCOUNT_WALLET, wallet.getId(), amount, walletBalanceAfter));

        // 4) PAID 로 전이
        LocalDateTime approvedAt = LocalDateTime.now();
        transactionMapper.updateStatus(chargeTx.getId(), userId, TransactionStatus.PAID.name(), approvedAt);
        chargeTx.setStatus(TransactionStatus.PAID.name());
        chargeTx.setApprovedAt(approvedAt);

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
        BigDecimal walletBalanceAfter = wallet.getBalance().subtract(amount);
        BigDecimal accountBalanceAfter = targetAccount.getBalance().add(amount);
        ledgerService.post(refundTx.getId(),
                LedgerEntryVO.debit(LedgerEntryVO.ACCOUNT_WALLET, wallet.getId(), amount, walletBalanceAfter),
                LedgerEntryVO.credit(LedgerEntryVO.ACCOUNT_BANK, targetAccount.getId(), amount, accountBalanceAfter));

        // 4) PAID 로 전이
        LocalDateTime approvedAt = LocalDateTime.now();
        transactionMapper.updateStatus(refundTx.getId(), userId, TransactionStatus.PAID.name(), approvedAt);
        refundTx.setStatus(TransactionStatus.PAID.name());
        refundTx.setApprovedAt(approvedAt);

        return RefundResponse.of(refundTx, walletBalanceAfter, targetAccount);
    }

    // ===== 결제 =====
    @Override
    @Transactional
    public PaymentResponse pay(Long userId, PaymentRequest request) {

        validatePaymentRequest(request);
        validatePaymentPin(userId, request.getDeviceId(), request.getPinNumber());

        if ("WALLET".equals(request.getPaymentSourceType())) {
            return payWithWallet(userId, request);
        } else {
            return payWithCard(userId, request);
        }
    }

    private PaymentResponse payWithWallet(Long userId, PaymentRequest request) {
        BigDecimal amount = request.getAmount();

        WalletVO wallet = walletMapper.findByUserId(userId);
        if (wallet == null) {
            throw new BusinessException(TransactionErrorCode.TRANSACTION_WALLET_NOT_FOUND);
        }

        // 1) REQUESTED 결제 거래 생성 — 멱등키 중복이면 자동충전/차감 전에 먼저 거부(409)
        TransactionVO paymentTx = TransactionVO.forWalletPayment(userId, wallet, request);
        try {
            transactionMapper.insertTransaction(paymentTx);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(TransactionErrorCode.TRANSACTION_DUPLICATE_REQUEST);
        }

        // 2) 잔액 부족 시 자동충전 (주계좌 -> 지갑). 내부 서브거래로 별도 기록 + 원장 기입
        BigDecimal shortage = amount.subtract(wallet.getBalance());
        boolean isAutoCharged = shortage.compareTo(BigDecimal.ZERO) > 0;
        BigDecimal autoChargedAmount = null;

        if (isAutoCharged) {
            // 부족분이 최소 충전금액(1만원)보다 적으면 1만원으로 채움
            BigDecimal actualChargeAmount = shortage.compareTo(MIN_CHARGE_AMOUNT) < 0
                    ? MIN_CHARGE_AMOUNT
                    : shortage;

            BankAccountVO primaryAccount = accountMapper.findPrimaryAccount(userId);
            if (primaryAccount == null) {
                throw new BusinessException(TransactionErrorCode.TRANSACTION_PRIMARY_ACCOUNT_NOT_FOUND_FOR_AUTO_CHARGE);
            }

            int accountUpdatedRows = accountMapper.decreaseBalance(primaryAccount.getId(), userId, actualChargeAmount);
            if (accountUpdatedRows == 0) {
                throw new BusinessException(TransactionErrorCode.TRANSACTION_INSUFFICIENT_ACCOUNT_BALANCE);
            }

            walletMapper.increaseBalance(userId, actualChargeAmount);
            autoChargedAmount = actualChargeAmount;

            // 자동충전은 내부 원자 충전이라 PAID 로 즉시 생성(forAutoCharge). 단, 잔액이 움직였으므로 원장은 기입한다.
            TransactionVO depositTx = TransactionVO.forAutoCharge(userId, wallet, primaryAccount, actualChargeAmount);
            transactionMapper.insertTransaction(depositTx);
            BigDecimal accBalanceAfter = primaryAccount.getBalance().subtract(actualChargeAmount);
            BigDecimal walBalanceAfter = wallet.getBalance().add(actualChargeAmount);
            ledgerService.post(depositTx.getId(),
                    LedgerEntryVO.debit(LedgerEntryVO.ACCOUNT_BANK, primaryAccount.getId(), actualChargeAmount, accBalanceAfter),
                    LedgerEntryVO.credit(LedgerEntryVO.ACCOUNT_WALLET, wallet.getId(), actualChargeAmount, walBalanceAfter));
        }

        // 3) 지갑 차감
        int walletUpdatedRows = walletMapper.decreaseBalance(userId, amount);
        if (walletUpdatedRows == 0) {
            throw new BusinessException(TransactionErrorCode.TRANSACTION_INSUFFICIENT_WALLET_BALANCE);
        }

        WalletVO updatedWallet = walletMapper.findByUserId(userId);

        // 4) 결제 원장: WALLET DEBIT(나감) / MERCHANT CREDIT(들어옴). 가맹점 잔액은 미보유 -> balance_after null
        ledgerService.post(paymentTx.getId(),
                LedgerEntryVO.debit(LedgerEntryVO.ACCOUNT_WALLET, wallet.getId(), amount, updatedWallet.getBalance()),
                LedgerEntryVO.credit(LedgerEntryVO.ACCOUNT_MERCHANT, paymentTx.getMerchantId(), amount, null));

        // 5) PAID 로 전이
        LocalDateTime approvedAt = LocalDateTime.now();
        transactionMapper.updateStatus(paymentTx.getId(), userId, TransactionStatus.PAID.name(), approvedAt);
        paymentTx.setStatus(TransactionStatus.PAID.name());
        paymentTx.setApprovedAt(approvedAt);

        return PaymentResponse.ofWallet(paymentTx, updatedWallet.getBalance(), isAutoCharged, autoChargedAmount);
    }

    private PaymentResponse payWithCard(Long userId, PaymentRequest request) {
        if (request.getCardId() == null) {
            throw new BusinessException(TransactionErrorCode.TRANSACTION_CARD_ID_REQUIRED);
        }

        CardVO card = cardMapper.findCardById(request.getCardId(), userId);
        if (card == null) {
            throw new BusinessException(TransactionErrorCode.TRANSACTION_CARD_NOT_FOUND);
        }

        boolean isBusinessExpense = "WORK".equals(card.getCardType());
        BigDecimal amount = request.getAmount();

        // 1) REQUESTED 카드결제 거래 생성 (멱등키 중복 -> 409)
        TransactionVO paymentTx = TransactionVO.forCardPayment(userId, card, request, isBusinessExpense);
        try {
            transactionMapper.insertTransaction(paymentTx);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(TransactionErrorCode.TRANSACTION_DUPLICATE_REQUEST);
        }

        // 2) PG 승인(authorize) — 실패 시 트랜잭션 롤백(거래 미생성). 승인번호/PG거래ID 기록 + AUTHORIZED 전이
        PgAuthResult auth;
        try {
            auth = paymentGatewayClient.authorize(userId, card.getId(), amount, request.getMerchantName());
        } catch (PgException e) {
            throw new BusinessException(TransactionErrorCode.TRANSACTION_PG_AUTH_FAILED);
        }
        transactionMapper.applyPgAuthorization(paymentTx.getId(), userId, auth.getPgTransactionId(), auth.getApprovalNumber());
        paymentTx.setPgTransactionId(auth.getPgTransactionId());
        paymentTx.setApprovedNumber(auth.getApprovalNumber());
        paymentTx.setStatus(TransactionStatus.AUTHORIZED.name());

        // 3) PG 매입(capture) — 실패 시 승인 취소(cancel)로 보상 후 롤백
        try {
            paymentGatewayClient.capture(auth.getPgTransactionId(), amount);
        } catch (PgException e) {
            paymentGatewayClient.cancel(auth.getPgTransactionId());
            throw new BusinessException(TransactionErrorCode.TRANSACTION_PG_CAPTURE_FAILED);
        }

        // 4) 원장: CARD DEBIT / MERCHANT CREDIT. 카드는 외부 발급사 자금이라 내부 잔액 이동 없음 -> balance_after null
        ledgerService.post(paymentTx.getId(),
                LedgerEntryVO.debit(LedgerEntryVO.ACCOUNT_CARD, card.getId(), amount, null),
                LedgerEntryVO.credit(LedgerEntryVO.ACCOUNT_MERCHANT, paymentTx.getMerchantId(), amount, null));

        // 5) PAID 로 전이 (매입 완료)
        LocalDateTime approvedAt = LocalDateTime.now();
        transactionMapper.updateStatus(paymentTx.getId(), userId, TransactionStatus.PAID.name(), approvedAt);
        paymentTx.setStatus(TransactionStatus.PAID.name());
        paymentTx.setApprovedAt(approvedAt);

        return PaymentResponse.ofCard(paymentTx);
    }

    // ===== 결제 취소 =====
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
