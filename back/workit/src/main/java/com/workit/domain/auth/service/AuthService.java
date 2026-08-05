package com.workit.domain.auth.service;

import com.workit.domain.auth.dto.request.SignupRequestDTO;
import com.workit.domain.auth.dto.response.EmailAvailabilityResponseDTO;
import com.workit.domain.auth.dto.response.IdentityVerificationResponseDTO;
import com.workit.domain.auth.dto.response.TermsListResponseDTO;

public interface AuthService {

    /** 필수/선택 약관 목록 조회 */
    TermsListResponseDTO getTermsList();

    /**
     * PASS 본인인증 결과 검증
     * Provider 검증 → CI SHA-256 중복 체크 → 임시 데이터 Redis 저장 →
     * 회원가입 전용 임시 JWT(identityToken)/name 반환
     */
    IdentityVerificationResponseDTO verifyIdentity(String identityVerificationId);

    /**
     * 회원가입 이메일 중복 확인
     * email 검증 → SHA-256 hash 생성 → users.email_hash 기준 조회 → 사용 가능 여부 반환
     * - 중복이어도 4xx 가 아니라 available=false 로 반환 (회원가입 화면 실시간 체크용)
     */
    EmailAvailabilityResponseDTO checkEmailAvailability(String email);

    /**
     * 최종 회원가입 완료
     *
     * 흐름:
     *   1. identityToken(회원가입 전용 JWT) 검증 — 서명/만료(sub == signup-verification)
     *   2. JWT 에서 temporaryUserKey 추출 → Redis(signup:verification:{key}) 임시 인증 데이터 조회
     *   3. CI / 이메일 / 닉네임 중복 재검증 (Race Condition 방지)
     *   4. users → user_auth → user_profile insert (동일 트랜잭션)
     *   5. 회원가입 완료 후 Redis 임시 데이터 삭제
     *   6. 전자지갑 생성
     *
     * @param request 회원가입 요청 (identityToken, email, password, nickname)
     */
    void signup(SignupRequestDTO request);
}
