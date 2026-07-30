package com.workit.domain.wallet.exception;

import com.workit.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum WalletErrorCode implements ErrorCode {

    WALLET_NOT_FOUND(HttpStatus.NOT_FOUND, "지갑이 존재하지 않습니다."),
    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 계좌입니다."),
    ACCOUNT_ID_REQUIRED(HttpStatus.BAD_REQUEST, "계좌를 선택해주세요."),
    AMOUNT_REQUIRED(HttpStatus.BAD_REQUEST, "충전 금액을 입력해주세요."),
    REFUND_AMOUNT_REQUIRED(HttpStatus.BAD_REQUEST, "환불 금액을 입력해주세요."),
    PIN_REQUIRED(HttpStatus.BAD_REQUEST, "PIN 번호를 입력해주세요."),
    PIN_INVALID(HttpStatus.BAD_REQUEST, "PIN 번호가 유효하지 않습니다."),
    MIN_CHARGE_AMOUNT_VIOLATION(HttpStatus.BAD_REQUEST, "최소 충전 금액은 10,000원입니다."),
    MAX_TRANSACTION_AMOUNT_EXCEEDED(HttpStatus.BAD_REQUEST, "1회 거래 가능 금액은 최대 200만원입니다."),
    INVALID_REFUND_AMOUNT(HttpStatus.BAD_REQUEST, "환불 금액이 올바르지 않습니다."),
    INSUFFICIENT_ACCOUNT_BALANCE(HttpStatus.BAD_REQUEST, "계좌 잔액이 부족합니다."),
    INSUFFICIENT_WALLET_BALANCE(HttpStatus.BAD_REQUEST, "환불 가능한 잔액이 부족합니다."),
    PRIMARY_ACCOUNT_NOT_FOUND(HttpStatus.BAD_REQUEST, "환불받을 주 계좌가 없습니다. 계좌를 먼저 연동해주세요."),
    ACCOUNT_STATE_INVALID(HttpStatus.BAD_REQUEST, "계좌 상태가 유효하지 않습니다.");

    private final HttpStatus status;
    private final String message;

    WalletErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    @Override
    public HttpStatus getStatus() {
        return status;
    }

    @Override
    public String getMessage() {
        return message;
    }
}