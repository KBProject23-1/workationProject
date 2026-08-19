package com.workit.domain.transaction.exception;

import com.workit.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum TransactionErrorCode implements ErrorCode {

    TRANSACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 거래 내역입니다."),
    TRANSACTION_RECEIPT_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "매출전표를 조회할 수 없는 거래입니다."),
    TRANSACTION_ALREADY_CANCELED(HttpStatus.BAD_REQUEST, "이미 취소된 거래입니다."),
    TRANSACTION_CANCEL_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "취소할 수 없는 거래입니다."),

    // 결제(payment) 관련
    TRANSACTION_MERCHANT_NAME_REQUIRED(HttpStatus.BAD_REQUEST, "가맹점명을 입력해주세요."),
    TRANSACTION_AMOUNT_REQUIRED(HttpStatus.BAD_REQUEST, "결제 금액을 입력해주세요."),
    TRANSACTION_INVALID_AMOUNT(HttpStatus.BAD_REQUEST, "결제 금액이 올바르지 않습니다."),
    TRANSACTION_MAX_AMOUNT_EXCEEDED(HttpStatus.BAD_REQUEST, "1회 결제 가능 금액은 최대 200만원입니다."),
    TRANSACTION_PAYMENT_SOURCE_TYPE_REQUIRED(HttpStatus.BAD_REQUEST, "결제 수단을 선택해주세요."),
    TRANSACTION_PAYMENT_SOURCE_TYPE_INVALID(HttpStatus.BAD_REQUEST, "결제 수단은 WALLET 또는 CARD 여야 합니다."),
    TRANSACTION_PIN_REQUIRED(HttpStatus.BAD_REQUEST, "PIN 번호를 입력해주세요."),
    TRANSACTION_DEVICE_ID_REQUIRED(HttpStatus.BAD_REQUEST, "기기 정보가 필요합니다."),
    TRANSACTION_PIN_INVALID(HttpStatus.BAD_REQUEST, "PIN 번호가 유효하지 않습니다."),
    TRANSACTION_PIN_NOT_REGISTERED(HttpStatus.BAD_REQUEST, "등록된 PIN 번호가 없습니다. PIN 번호를 먼저 설정해주세요."),
    TRANSACTION_PIN_LOCKED(HttpStatus.FORBIDDEN, "PIN 번호 입력 횟수가 초과되어 잠겼습니다. PASS 본인인증을 통해 PIN 번호를 재설정해 주세요."),
    TRANSACTION_IDEMPOTENCY_KEY_REQUIRED(HttpStatus.BAD_REQUEST, "요청 식별 키가 필요합니다."),
    TRANSACTION_DUPLICATE_REQUEST(HttpStatus.CONFLICT, "이미 처리된 요청입니다."),
    TRANSACTION_CARD_ID_REQUIRED(HttpStatus.BAD_REQUEST, "카드를 선택해주세요."),
    TRANSACTION_CARD_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 카드입니다."),
    TRANSACTION_WALLET_NOT_FOUND(HttpStatus.NOT_FOUND, "지갑이 존재하지 않습니다."),
    TRANSACTION_PRIMARY_ACCOUNT_NOT_FOUND_FOR_AUTO_CHARGE(HttpStatus.BAD_REQUEST, "연동된 주 계좌가 없어 자동충전이 불가합니다."),
    TRANSACTION_INSUFFICIENT_ACCOUNT_BALANCE(HttpStatus.BAD_REQUEST, "계좌 잔액이 부족합니다."),
    TRANSACTION_INSUFFICIENT_WALLET_BALANCE(HttpStatus.BAD_REQUEST, "지갑 잔액이 부족합니다."),

    // 카드결제 PG(외부 결제망) 관련
    TRANSACTION_PG_AUTH_FAILED(HttpStatus.BAD_GATEWAY, "카드 결제 승인에 실패했습니다. 잠시 후 다시 시도해주세요."),
    TRANSACTION_PG_CAPTURE_FAILED(HttpStatus.BAD_GATEWAY, "카드 결제 매입에 실패했습니다. 승인이 취소되었습니다."),
    TRANSACTION_PROCESSING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "결제 처리 중 오류가 발생했습니다. 승인이 취소되었습니다. 다시 시도해주세요.");

    private final HttpStatus status;
    private final String message;

    TransactionErrorCode(HttpStatus status, String message) {
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