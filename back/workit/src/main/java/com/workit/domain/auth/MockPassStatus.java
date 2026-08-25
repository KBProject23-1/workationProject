package com.workit.domain.auth;

// Mock PASS 본인인증 세션 상태 (LoginType 과 동일하게 domain.auth 최상위에 배치)
//
// - 실제 PASS/PortOne 연동 전까지 백엔드가 Mock 인증 상태를 관리하기 위한 상태값
// - Mock 흐름: POST /api/v1/auth/pass 가 백엔드에서 인증 세션을 생성하면
//   VERIFIED(used=false) 세션이 저장되고, Provider 는 VERIFIED 이고 미사용인 세션만 수용한다.
public enum MockPassStatus {

    /** 예약 — 향후 인증 진행 단계가 생기면 사용 (현재 흐름에서는 생성 즉시 VERIFIED) */
    PENDING,

    /** 본인인증 완료 — POST /api/v1/auth/pass 가 세션 생성 시 도달 (Provider 도 이 상태만 수용) */
    VERIFIED
}
