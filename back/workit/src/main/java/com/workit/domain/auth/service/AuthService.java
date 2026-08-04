package com.workit.domain.auth.service;

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
}
