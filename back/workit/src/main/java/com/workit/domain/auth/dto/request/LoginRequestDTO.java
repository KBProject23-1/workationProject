package com.workit.domain.auth.dto.request;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// 통합 로그인 요청
// API 스펙(docs): POST /api/v1/auth/login
// 필드: loginType, loginId, password, pinNumber, deviceId
//
// - loginType : "PASSWORD" 또는 "PIN" (필수)
// - PASSWORD  : loginId(이메일 또는 휴대폰 번호) + password 필수
// - PIN       : pinNumber(6자리) + deviceId(UUID) 필수
//
// javax.validation 미사용 프로젝트 구조이므로 필수 값 검증은 Service Layer 에서 수행한다
// (기존 Request DTO 와 동일 방식)
//
// 보안 규칙 (knowledge.md: password/pin 원문 로그 출력 금지):
// - password/pinNumber 는 @ToString.Exclude 로 처리 — DTO 를 로그에 남겨도 원문이 노출되지 않는다
@Getter
@Setter
@ToString
public class LoginRequestDTO {

    /** 로그인 방식 (PASSWORD | PIN) */
    private String loginType;

    /** PASSWORD 일 때 필수 — 이메일 또는 하이픈 없는 휴대폰 번호 */
    private String loginId;

    /** PASSWORD 일 때 필수 — 비밀번호. 로그 출력 금지 */
    @ToString.Exclude
    private String password;

    /** PIN 일 때 필수 — 6자리 숫자 PIN. 로그 출력 금지 */
    @ToString.Exclude
    private String pinNumber;

    /** PIN 일 때 필수 — 브라우저 로컬스토리지에 저장된 기기 식별 UUID */
    private String deviceId;
}
