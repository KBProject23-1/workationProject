package com.workit.domain.auth.service;

import com.workit.domain.auth.dto.request.LoginRequestDTO;
import com.workit.domain.auth.dto.request.SignupRequestDTO;
import com.workit.domain.auth.dto.response.EmailAvailabilityResponseDTO;
import com.workit.domain.auth.dto.response.IdentityVerificationResponseDTO;
import com.workit.domain.auth.dto.response.LoginResponseDTO;
import com.workit.domain.auth.dto.response.RefreshTokenResponseDTO;
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

    /**
     * 통합 로그인 (PASSWORD / PIN)
     *
     * 흐름:
     *   1. 요청 값 검증 (INVALID_LOGIN_REQUEST / INVALID_LOGIN_TYPE)
     *   2. PASSWORD: email/phone SHA-256 hash 조회 → password BCrypt 검증
     *      PIN: deviceId 조회 → PIN 실패 횟수 잠금 확인 → pin BCrypt 검증
     *   3. 회원 상태(ACTIVE) 확인
     *   4. Access Token / Refresh Token 발급
     *   5. Refresh Token SHA-256 hash 를 Redis(refresh:token:{userId})에 TTL 저장
     *   6. LoginResponseDTO 반환 (refreshToken 은 HttpOnly Cookie 전용 — JSON 제외)
     *
     * @param request 로그인 요청 (loginType, loginId, password, pinNumber, deviceId)
     * @return 로그인 성공 응답 (userId, name, token_info) + 쿠키용 refreshToken
     */
    LoginResponseDTO login(LoginRequestDTO request);

    /**
     * Refresh Token 기반 Access Token 재발급 (Refresh Token Rotation 적용)
     *
     * 흐름:
     *   1. 쿠키에서 받은 Refresh Token 검증 — 서명/만료 + tokenType == REFRESH (JwtTokenProvider.parseRefreshToken)
     *   2. sub(userId) 추출 → 회원 존재 + ACTIVE 상태 확인 (findUserById)
     *   3. Redis(refresh:token:{userId}) 저장 hash 와 비교
     *      - 저장 hash 없음(로그아웃/만료) → INVALID_REFRESH_TOKEN(401)
     *      - 불일치(재사용 감지) → 세션 revoke(delete) + INVALID_REFRESH_TOKEN(401)
     *   4. 신규 Access Token + 신규 Refresh Token 발급 (Rotation)
     *   5. 신규 Refresh Token SHA-256 hash 를 Redis 에 교체 저장 (TTL 동일)
     *   6. RefreshTokenResponseDTO 반환 (refreshToken 은 HttpOnly Cookie 전용 — JSON 제외)
     *
     * @param refreshToken HttpOnly Cookie 에서 받은 Refresh Token (없으면 null)
     * @return 재발급 응답 (token_info) + 쿠키용 신규 refreshToken
     */
    RefreshTokenResponseDTO refreshAccessToken(String refreshToken);
}
