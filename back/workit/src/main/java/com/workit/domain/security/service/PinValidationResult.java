package com.workit.domain.security.service;

// PinValidator.validate() 결과 - 도메인별 ErrorCode 매핑은 각 호출부(Service)에서 수행한다
public enum PinValidationResult {
    VALID,
    DEVICE_NOT_REGISTERED,
    LOCKED,
    MISMATCH
}
