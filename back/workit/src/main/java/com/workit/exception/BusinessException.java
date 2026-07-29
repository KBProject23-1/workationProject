package com.workit.exception;

import lombok.Getter;

// 모든 도메인이 공통으로 사용하는 비즈니스 예외
// 상태 코드와 메시지는 ErrorCode 가 결정하므로 도메인마다 예외 클래스를 새로 만들 필요가 없음

@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());      // RuntimeException(부모)에 메시지 전달
        this.errorCode = errorCode;         // ErrorCode 자체도 보관
    }

    // 기본 메시지 대신 상황별 메시지를 전달해야 할 때 사용
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
