package com.workit.domain.workation.exception;

/**
 * 워케이션 도메인 409 Conflict 예외
 * 예) 진행 중인 워케이션이 이미 있는 경우, 이미 정산 완료된 워케이션을 다시 처리하는 경우
 */
public class WorkationConflictException extends RuntimeException {

    public WorkationConflictException(String message) {
        super(message);
    }
}
