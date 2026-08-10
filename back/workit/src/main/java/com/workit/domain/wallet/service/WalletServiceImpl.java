package com.workit.domain.wallet.service;

import com.workit.domain.account.mapper.AccountMapper;
import com.workit.domain.account.vo.BankAccountVO;
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

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletMapper walletMapper;
    private final AccountMapper accountMapper;
    private final TransactionMapper transactionMapper;
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

        int accountUpdatedRows = accountMapper.decreaseBalance(request.getAccountId(), amount);
        if (accountUpdatedRows == 0) {
            throw new BusinessException(WalletErrorCode.WALLET_INSUFFICIENT_ACCOUNT_BALANCE);
        }

        walletMapper.increaseBalance(userId, amount);

        TransactionVO chargeTx = TransactionVO.forWalletCharge(userId, wallet, account, amount, request.getIdempotencyKey());
        try {
            transactionMapper.insertTransaction(chargeTx);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(WalletErrorCode.WALLET_DUPLICATE_REQUEST);
        }

        return ChargeResponse.of(chargeTx, wallet.getBalance().add(amount));
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

        int walletUpdatedRows = walletMapper.decreaseBalance(userId, amount);
        if (walletUpdatedRows == 0) {
            throw new BusinessException(WalletErrorCode.WALLET_INSUFFICIENT_BALANCE);
        }

        int accountUpdatedRows = accountMapper.increaseBalance(targetAccount.getId(), amount);
        if (accountUpdatedRows == 0) {
            throw new BusinessException(WalletErrorCode.WALLET_ACCOUNT_STATE_INVALID);
        }

        WalletVO updatedWallet = walletMapper.findByUserId(userId);

        TransactionVO refundTx = TransactionVO.forWalletRefund(userId, wallet, targetAccount, amount, request.getIdempotencyKey());
        try {
            transactionMapper.insertTransaction(refundTx);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(WalletErrorCode.WALLET_DUPLICATE_REQUEST);
        }

        return RefundResponse.of(refundTx, updatedWallet.getBalance(), targetAccount);
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