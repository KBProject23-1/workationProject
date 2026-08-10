package com.workit.domain.transaction.gateway;

import lombok.Getter;

/**
 * PG 처리 실패(승인 거절 / 매입 실패 / 타임아웃). 언체크 예외로, 서비스 트랜잭션을 롤백시킨다.
 */
@Getter
public class PgException extends RuntimeException {

    public enum Type {
        DECLINED,   // 승인 거절
        TIMEOUT,    // 응답 지연/타임아웃
        CAPTURE_FAILED
    }

    private final Type type;

    public PgException(Type type, String message) {
        super(message);
        this.type = type;
    }
}
