package com.workit.domain.auth.provider;

import lombok.Builder;
import lombok.Getter;

// 본인인증 검증 결과 (Provider 출력)
//
// 주의 (knowledge.md):
// - ci 는 개인식별값 — 평문 로그 출력 금지, DB 저장/응답 시 반드시 AES-256 암호화
// - Controller/Mapper 가 아닌 Service Layer 에서만 사용·암호화
@Getter
@Builder
public class IdentityVerificationResult {

    /** 개인식별값 (CI) — 암호화 저장 대상 */
    private String ci;

    /** 유저 이름 — 화면 표시 전용 */
    private String name;

    /** 유저 휴대폰 번호 — users.phone_number_* 저장 대상 (개인정보, 암호화 저장) */
    private String phoneNumber;
}
