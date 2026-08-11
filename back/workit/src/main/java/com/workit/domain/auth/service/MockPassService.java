package com.workit.domain.auth.service;

import com.workit.domain.auth.dto.request.MockPassCompleteRequestDTO;
import com.workit.domain.auth.dto.response.MockPassStatusResponseDTO;

// Mock PASS 본인인증 서비스 (개발/테스트용 — 실제 PortOne 연동 전까지)
//
// 책임:
// - 프론트가 생성한 identityVerificationId 를 VERIFIED 세션으로 등록
//   (Mock 인증 완료를 백엔드가 기록 — verify-identity 는 VERIFIED 세션만 수용하므로
//    흐름을 건너뛰고 인증 ID 만 전송하는 방식은 성립하지 않는다)
//
// 프론트 흐름: Mock PASS 팝업(통신사/약관 → 이름/휴대폰/보안문자) 완료
//   → identityVerificationId 생성 → POST /api/v1/auth/pass (complete)
//   → VERIFIED 세션 → POST /api/v1/auth/signup/verify-identity → identityToken 발급
public interface MockPassService {

    /**
     * Mock 인증 완료 등록 — 프론트가 생성한 identityVerificationId 를 VERIFIED 세션으로 저장
     *
     * 흐름:
     *   1. 필수 값 검증 (identityVerificationId 누락 → INVALID_VERIFICATION_ID,
     *      name/phoneNumber 누락 → INVALID_PASS_REQUEST)
     *   2. 형식 검증 (identityVerificationId: mock- 접두어 + 최대 길이,
     *      phoneNumber: 숫자 10~11자리 → INVALID_PASS_REQUEST)
     *   3. name/phoneNumber AES-256 암호화 후 VERIFIED 세션 저장 (Redis TTL 기본 10분)
     *
     * @param request 인증 완료 요청 (identityVerificationId, name, phoneNumber)
     * @return identityVerificationId / status(VERIFIED) / name
     */
    MockPassStatusResponseDTO complete(MockPassCompleteRequestDTO request);
}
