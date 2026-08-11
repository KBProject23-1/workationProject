package com.workit.domain.auth;

// Mock PASS 본인인증 세션 상태 (LoginType 과 동일하게 domain.auth 최상위에 배치)
//
// - 실제 PASS/PortOne 연동 전까지 백엔드가 Mock 인증 상태를 관리하기 위한 상태값
// - Mock 흐름은 프론트가 생성한 identityVerificationId 를 POST /api/v1/auth/pass 로 등록하면
//   VERIFIED 세션이 생성되고, verify-identity 는 VERIFIED 세션만 수용한다.
public enum MockPassStatus {

    /** 예약 — 향후 인증 진행 단계가 생기면 사용 (현재 흐름에서는 등록 즉시 VERIFIED) */
    PENDING,

    /** 본인인증 완료 — POST /api/v1/auth/pass 등록 성공 시 도달 (MockIdentityVerificationProvider 도 이 상태만 수용) */
    VERIFIED
}
