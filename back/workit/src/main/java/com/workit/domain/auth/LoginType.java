package com.workit.domain.auth;

// 통합 로그인 방식 구분 (docs: 로그인 → loginType: PASSWORD 또는 PIN)
//
// - PASSWORD: 이메일 또는 휴대폰 번호 + 비밀번호 로그인
// - PIN     : 등록된 기기(deviceId) + PIN 번호 간편 로그인
//
// 잘못된 값은 valueOf 실패(IllegalArgumentException) → AuthServiceImpl 에서
// INVALID_LOGIN_TYPE(400) 으로 변환한다.
public enum LoginType {

    PASSWORD,
    PIN
}
