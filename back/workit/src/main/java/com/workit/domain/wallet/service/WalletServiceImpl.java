package com.workit.domain.wallet.service;

import com.workit.domain.account.mapper.AccountMapper;
import com.workit.domain.account.vo.BankAccountVO;
import com.workit.domain.ledger.service.LedgerService;
import com.workit.domain.ledger.vo.LedgerEntryVO;
import com.workit.domain.payment.TransactionStatus;
import com.workit.domain.transaction.mapper.TransactionMapper;
import com.workit.domain.transaction.vo.TransactionVO;
import com.workit.domain.wallet.dto.request.ChargeRequest;
import com.workit.domain.wallet.dto.request.RefundRequest;
import com.workit.domain.wallet.dto.response.ChargeResponse;
import com.workit.domain.wallet.dto.response.RefundResponse;
import com.workit.domain.wallet.dto.response.WalletResponse;
import com.workit.domain.wallet.exception.WalletErrorCode;
import com.workit.domain.wallet.mapper.WalletMapper;
import com.workit.domain.wallet.vo.WalletVO;
import com.workit.domain.security.service.PinValidationResult;
import com.workit.domain.security.service.PinValidator;
import com.workit.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.workit.global.constant.PaymentPolicy.MIN_CHARGE_AMOUNT;
import static com.workit.global.constant.PaymentPolicy.MAX_TRANSACTION_AMOUNT;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletMapper walletMapper;
    private final AccountMapper accountMapper;
    private final TransactionMapper transactionMapper;
    private final LedgerService ledgerService;
    private final PinValidator pinValidator;

    @Override
    public void createWallet(Long userId) {
        if (walletMapper.existsByUserId(userId)) {
            return;
        }
        walletMapper.insertWallet(userId);
    }

    @Override
    public WalletResponse getMyWallet(Long userId) {
        WalletVO wallet = walletMapper.findByUserId(userId);
        if (wallet == null) {
            walletMapper.insertWallet(userId);
            wallet = walletMapper.findByUserId(userId);
        }
        return WalletResponse.from(wallet);
    }

    @Override
    @Transactional
    public ChargeResponse charge(Long userId, ChargeRequest request) {

        validateChargeRequest(request);
        validatePin(userId, request.getDeviceId(), request.getPinNumber());

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

        // 3) 복식부기 원장 기입: BANK DEBIT(나감) / WALLET CREDIT(들어옴), SUM(DEBIT)==SUM(CREDIT)
        //    balance_after 는 참고용 스냅샷(권위값은 계좌/지갑 row, 대사는 원장 금액 합으로 수행)
        BigDecimal accountBalanceAfter = account.getBalance().subtract(amount);
        BigDecimal walletBalanceAfter = wallet.getBalance().add(amount);
        ledgerService.post(chargeTx.getId(),
                LedgerEntryVO.debit(LedgerEntryVO.ACCOUNT_BANK, account.getId(), amount, accountBalanceAfter),
                LedgerEntryVO.credit(LedgerEntryVO.ACCOUNT_WALLET, wallet.getId(), amount, walletBalanceAfter));

        // 4) PAID 로 전이 (승인 시각 기록)
        LocalDateTime approvedAt = LocalDateTime.now();
        transactionMapper.updateStatus(chargeTx.getId(), userId, TransactionStatus.PAID.name(), approvedAt);
        chargeTx.setStatus(TransactionStatus.PAID.name());
        chargeTx.setApprovedAt(approvedAt);

        return ChargeResponse.of(chargeTx, walletBalanceAfter);
    }

    @Override
    @Transactional
    public RefundResponse refund(Long userId, RefundRequest request) {

        validateRefundRequest(request);
        validatePin(userId, request.getDeviceId(), request.getPinNumber());

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

        // 2) 잔액 이동 (지갑 차감 -> 계좌 적립). 실패 시 예외로 트랜잭션 전체 롤백
        int walletUpdatedRows = walletMapper.decreaseBalance(userId, amount);
        if (walletUpdatedRows == 0) {
            throw new BusinessException(WalletErrorCode.WALLET_INSUFFICIENT_BALANCE);
        }
        int accountUpdatedRows = accountMapper.increaseBalance(targetAccount.getId(), userId, amount);
        if (accountUpdatedRows == 0) {
            throw new BusinessException(WalletErrorCode.WALLET_ACCOUNT_STATE_INVALID);
        }

        // 3) 복식부기 원장 기입: WALLET DEBIT(나감) / BANK CREDIT(들어옴), SUM(DEBIT)==SUM(CREDIT)
        BigDecimal walletBalanceAfter = wallet.getBalance().subtract(amount);
        BigDecimal accountBalanceAfter = targetAccount.getBalance().add(amount);
        ledgerService.post(refundTx.getId(),
                LedgerEntryVO.debit(LedgerEntryVO.ACCOUNT_WALLET, wallet.getId(), amount, walletBalanceAfter),
                LedgerEntryVO.credit(LedgerEntryVO.ACCOUNT_BANK, targetAccount.getId(), amount, accountBalanceAfter));

        // 4) PAID 로 전이 (승인 시각 기록)
        LocalDateTime approvedAt = LocalDateTime.now();
        transactionMapper.updateStatus(refundTx.getId(), userId, TransactionStatus.PAID.name(), approvedAt);
        refundTx.setStatus(TransactionStatus.PAID.name());
        refundTx.setApprovedAt(approvedAt);

        return RefundResponse.of(refundTx, walletBalanceAfter, targetAccount);
    }

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

    private void validatePin(Long userId, String deviceId, String pinNumber) {
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
}