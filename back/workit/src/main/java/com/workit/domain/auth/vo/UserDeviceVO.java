package com.workit.domain.auth.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

// user_device 테이블 VO (MyBatis ResultType / insert 파라미터)
// ERD 기준 컬럼: id, user_id, device_id, device_name, pin_hash, pin_updated_at,
//                last_login_at, created_at, updated_at
//
// - pin_hash 는 BCrypt 해시 (knowledge.md: PIN 원문 저장 금지, 단방향 암호화만 허용)
// - last_login_at 은 PIN 최초 설정 시점에는 null (최초 로그인 일시는 PIN 로그인 성공 시 갱신 예정)
// - created_at 은 DB 기본값(CURRENT_TIMESTAMP) 사용 — insert 시 설정하지 않는다
@Getter
@Setter
@ToString
public class UserDeviceVO {

    /** 기기 등록 고유 번호 (PK, AUTO_INCREMENT) */
    private Long id;

    /** 회원 고유 번호 (users.id FK) */
    private Long userId;

    /** 브라우저 고유 식별 UUID */
    private String deviceId;

    /** 사용자 기기 정보 (예: Chrome / Windows) */
    private String deviceName;

    /** 6자리 PIN BCrypt 해시 (원문 아님 — 로그 주의는 동일하게 요구) */
    private String pinHash;

    /** PIN 최종 변경 일시 */
    private LocalDateTime pinUpdatedAt;

    /** 해당 기기 최종 로그인 일시 (PIN 최초 설정 시 null) */
    private LocalDateTime lastLoginAt;

    /** 기기 최초 인증 등록 일시 (DB 기본값) */
    private LocalDateTime createdAt;

    /** 기기 정보 수정 일시 (DB ON UPDATE CURRENT_TIMESTAMP) */
    private LocalDateTime updatedAt;
}
