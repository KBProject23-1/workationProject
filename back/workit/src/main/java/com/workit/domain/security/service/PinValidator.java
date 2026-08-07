package com.workit.domain.security.service;

import com.workit.domain.auth.service.LoginFailCounter;
import com.workit.domain.security.mapper.UserDeviceMapper;
import com.workit.global.util.PasswordEncryptor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

// 결제성 API(wallet charge/refund, transaction payments) 공용 PIN 검증
// - AuthServiceImpl.loginByPin()과 동일한 정책(기기 조회 -> 잠금 확인 -> BCrypt 대조 -> 실패 카운터)을 재사용
// - LoginFailCounter는 auth 도메인과 동일한 Redis 키(auth:fail:{userId})를 공유한다 -
//   PIN 로그인 실패와 결제 PIN 실패가 하나의 잠금 카운터로 합산되는 것이 의도된 동작이다
// - 도메인별 ErrorCode 변환은 호출부(WalletServiceImpl/TransactionServiceImpl)에서 수행한다
@Service
@RequiredArgsConstructor
public class PinValidator {

    private static final int MAX_PIN_FAIL_COUNT = 5;

    private final UserDeviceMapper userDeviceMapper;
    private final LoginFailCounter loginFailCounter;

    public PinValidationResult validate(Long userId, String deviceId, String pinNumber) {
        String pinHash = userDeviceMapper.findPinHashByUserIdAndDeviceId(userId, deviceId);
        if (pinHash == null) {
            return PinValidationResult.DEVICE_NOT_REGISTERED;
        }

        if (loginFailCounter.getCount(userId) >= MAX_PIN_FAIL_COUNT) {
            return PinValidationResult.LOCKED;
        }

        if (!PasswordEncryptor.matches(pinNumber, pinHash)) {
            loginFailCounter.increment(userId);
            return PinValidationResult.MISMATCH;
        }

        loginFailCounter.reset(userId);
        return PinValidationResult.VALID;
    }
}
