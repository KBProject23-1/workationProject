package com.workit.domain.security.mapper;

import org.apache.ibatis.annotations.Param;

// user_device 조회 전용 매퍼 - PIN 검증(charge/refund/payments 공용)에서만 사용
// 등록/수정은 auth 도메인(PIN 최초 설정 API)이 소유하므로 여기서는 조회만 담당한다
public interface UserDeviceMapper {

    /**
     * (userId, deviceId) 조합으로 등록된 기기의 PIN 해시를 조회한다.
     * 등록된 기기가 없으면 null 반환 - 실패 판단은 Service Layer(PinValidator)에서 수행
     */
    String findPinHashByUserIdAndDeviceId(@Param("userId") Long userId, @Param("deviceId") String deviceId);
}
