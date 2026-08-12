package com.workit.domain.auth.service;

import com.workit.domain.auth.dto.request.MockPassCompleteRequestDTO;
import com.workit.domain.auth.dto.response.MockPassStatusResponseDTO;

// Mock PASS 본인인증 서비스 (개발/테스트용 — 실제 PortOne 연동 전까지)
//
// 책임:
// - identityVerificationId 를 백엔드가 직접 생성한다 (프론트 생성/전달 금지)
// - Mock CI 를 생성하고, name/phoneNumber/CI 를 AES-256 암호화해
//   VERIFIED(used=false) 세션으로 Redis(mock:pass:{id}) 에 저장한 뒤 identityVerificationId 를 반환한다
//
// 프론트 흐름:
//   이름/휴대폰 번호 입력 → POST /api/v1/auth/pass (complete)
//   → 백엔드가 인증 세션 생성 → identityVerificationId 발급
//   → 회원가입(POST /api/v1/auth/signup) / 아이디 찾기(POST /api/v1/auth/find-id) 등에서
//     identityVerificationId 만 전달하면 백엔드가 Redis 세션에서 인증 정보를 복원한다
public interface MockPassService {

    /**
     * Mock 본인인증 처리 — 백엔드가 인증 세션을 생성하고 identityVerificationId 를 발급한다
     *
     * 흐름:
     *   1. 필수 값 검증 (name/phoneNumber 누락·빈 값 → INVALID_PASS_REQUEST)
     *   2. 형식 검증 (phoneNumber: 숫자 10~11자리 → INVALID_PASS_REQUEST)
     *   3. identityVerificationId(UUID) / Mock CI 생성
     *   4. name/phoneNumber/CI AES-256 암호화 후 VERIFIED(used=false) 세션 저장
     *      (Redis TTL 기본 10분 — mock.pass.ttl.minutes)
     *   5. identityVerificationId / status(VERIFIED) 반환 (개인정보는 응답에 미포함)
     *
     * @param request 본인인증 요청 (name, phoneNumber)
     * @return identityVerificationId / status(VERIFIED)
     */
    MockPassStatusResponseDTO complete(MockPassCompleteRequestDTO request);
}
