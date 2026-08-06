package com.workit.domain.auth.dto.request;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// PIN 번호 최초 설정 요청
// API 스펙(docs): POST /api/v1/users/me/pin-number (본 프로젝트 경로: POST /api/v1/auth/me/pin)
// 필드: pinNumber, deviceId, deviceName
// - javax.validation 미사용 프로젝트 구조이므로 필수 값/형식 검증은 Service Layer 에서 수행한다
//   (기존 Request DTO 와 동일 방식)
//
// 보안 규칙 (knowledge.md: PIN 원문 로그 출력 금지):
// - pinNumber 는 @ToString.Exclude 로 처리 — DTO 를 로그에 남겨도 PIN 원문이 노출되지 않는다
@Getter
@Setter
@ToString
public class PinSetupRequestDTO {

    /** 6자리 숫자 PIN — BCrypt 해시로 저장. 로그 출력 금지 */
    @ToString.Exclude
    private String pinNumber;

    /** 브라우저 로컬스토리지에 저장된 기기 식별 UUID */
    private String deviceId;

    /** 사용자 기기명 (예: Chrome / Windows) */
    private String deviceName;
}
