package com.workit.domain.auth.dto.request;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// Mock PASS 인증 완료 등록 요청
// POST /api/v1/auth/pass
//
// - identityVerificationId 는 프론트가 생성해 전송한다 (실제 PASS 의 PortOne 반환값을 흉내)
// - 실제 주민등록번호를 받지 않는다 — 테스트용 개인정보(이름/휴대폰)만 사용
// - javax.validation 의존성이 없는 프로젝트 구조이므로 Service Layer 에서 필수 값 검증을 수행한다
@Getter
@Setter
@ToString
public class MockPassCompleteRequestDTO {

    /** Mock 인증 고유 번호 (mock-xxxxxxxx — 프론트 생성) */
    private String identityVerificationId;

    /** 이름 (예: 홍길동) */
    private String name;

    /** 휴대폰 번호 (예: 01012345678 — 숫자만) */
    private String phoneNumber;
}
