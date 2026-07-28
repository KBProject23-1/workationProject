package com.workit.domain.workation.exception;

/**
 * 워케이션 도메인 404 Not Found 예외
 */
public class WorkationNotFoundException extends RuntimeException {

    public WorkationNotFoundException(String message) {
        super(message);
    }
}
