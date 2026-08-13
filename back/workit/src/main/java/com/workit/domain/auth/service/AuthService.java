package com.workit.domain.auth.service;

import com.workit.domain.auth.dto.request.ChangePasswordRequestDTO;
import com.workit.domain.auth.dto.request.LoginRequestDTO;
import com.workit.domain.auth.dto.request.PasswordResetRequestDTO;
import com.workit.domain.auth.dto.request.PasswordVerifyRequestDTO;
import com.workit.domain.auth.dto.request.PinResetRequestDTO;
import com.workit.domain.auth.dto.request.PinSetupRequestDTO;
import com.workit.domain.auth.dto.request.SignupRequestDTO;
import com.workit.domain.auth.dto.response.EmailAvailabilityResponseDTO;
import com.workit.domain.auth.dto.response.FindIdResponseDTO;
import com.workit.domain.auth.dto.response.LoginResponseDTO;
import com.workit.domain.auth.dto.response.PasswordVerifyResponseDTO;
import com.workit.domain.auth.dto.response.RefreshTokenResponseDTO;
import com.workit.domain.auth.dto.response.SignupResponseDTO;
import com.workit.domain.auth.dto.response.TermsListResponseDTO;
import com.workit.domain.auth.dto.response.VerifyIdentityResponseDTO;

public interface AuthService {

    /** 필수/선택 약관 목록 조회 */
    TermsListResponseDTO getTermsList();

    /**
     * 회원가입 이메일 중복 확인
     * email 검증 → SHA-256 hash 생성 → users.email_hash 기준 조회 → 사용 가능 여부 반환
     * - 중복이어도 4xx 가 아니라 available=false 로 반환 (회원가입 화면 실시간 체크용)
     */
    EmailAvailabilityResponseDTO checkEmailAvailability(String email);

    /**
     * 회원가입 본인인증 검증 및 회원 중복 체크 (docs: POST /api/v1/auth/signup/verify-identity)
     *
     * PASS 인증(POST /auth/pass) 완료 후 계정정보 입력 전에 호출한다.
     *
     * 흐름:
     *   1. 요청 값 검증 — null/빈 값 → INVALID_VERIFICATION_ID(400)
     *   2. PASS 본인인증 결과 검증 → CI 추출 (Provider 실패 시 INVALID_VERIFICATION_ID)
     *   3. CI SHA-256 hash 변환 → user_auth.identity_ci_hash 기준 중복 가입 조회
     *      - 동일 휴대폰(CI) 으로 이미 가입한 회원이 있으면 409 DUPLICATE_USER — 가입 진행 차단
     *   4. 중복 없음 → 화면 표시용 name 반환 (identityVerificationId 는 signup 에서 재사용)
     *
     * @param identityVerificationId PASS 인증 후 발급받은 고유 ID (없으면 null)
     * @return 화면 표시용 이름 (개인정보 미포함)
     */
    VerifyIdentityResponseDTO verifyIdentityForSignup(String identityVerificationId);

    /**
     * 최종 회원가입 완료 (토큰 미발급 — 자동 로그인 없음)
     *
     * 흐름:
     *   1. Mock PASS 인증 세션 검증 — identityVerificationId 로 Redis(mock:pass:{id}) 조회
     *      (세션 없음/TTL 만료/status != VERIFIED/used == true → INVALID_VERIFICATION_ID)
     *   2. 세션에서 name / phoneNumber / CI 복원 (AES 복호화)
     *   3. CI / 이메일 중복 재검증 (Race Condition 방지)
     *   4. users → user_auth → user_profile insert (동일 트랜잭션)
     *      - user_profile.nickname 은 서버가 기본값(워케이너{userId}) 자동 생성 (닉네임 입력 기능 제거)
     *   5. 전자지갑 생성
     *   6. 회원가입 완료 후 Mock PASS 세션 사용 완료 처리 (used=true — 1회성)
     *   7. 회원가입 완료 — Access/Refresh Token 을 발급하지 않는다
     *      (변경 정책: 회원가입 후 로그인 화면으로 이동해 다시 로그인)
     *
     * @param request 회원가입 요청 (identityVerificationId, email, password, agreedTermsIds)
     * @return 회원가입 완료 응답 (userId, name — 토큰/쿠키 없음)
     */
    SignupResponseDTO signup(SignupRequestDTO request);

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
     *   6. LoginResponseDTO 반환 (accessToken/refreshToken 은 HttpOnly Cookie 전용 — JSON 제외)
     *
     * @param request 로그인 요청 (loginType, loginId, password, pinNumber, deviceId)
     * @return 로그인 성공 응답 (userId, name, pinSetupRequired) + 쿠키용 accessToken/refreshToken
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
     *   6. RefreshTokenResponseDTO 반환 (accessToken/refreshToken 은 HttpOnly Cookie 전용 — JSON 제외)
     *
     * @param refreshToken HttpOnly Cookie 에서 받은 Refresh Token (없으면 null)
     * @return 재발급 결과 + 쿠키용 신규 accessToken/refreshToken
     */
    RefreshTokenResponseDTO refreshAccessToken(String refreshToken);

    /**
     * 로그아웃 — Refresh Token 세션 폐기
     *
     * 흐름:
     *   1. 쿠키에서 받은 Refresh Token 검증 — 서명/만료 + tokenType == REFRESH (JwtTokenProvider.parseRefreshToken)
     *   2. sub(userId) 추출
     *   3. SHA-256 변환 → Redis(refresh:token:{userId}) 저장 hash 와 비교
     *      - 저장 hash 없음(이미 로그아웃/TTL 만료) → INVALID_REFRESH_TOKEN(401)
     *      - 불일치(위변조/재사용 의심) → 세션 revoke(delete) + INVALID_REFRESH_TOKEN(401)
     *      - 일치 → Refresh Token 삭제 — 이후 재발급 불가
     *   4. Cookie 만료(Max-Age=0) 처리는 Controller 가 수행
     *
     * @param refreshToken HttpOnly Cookie 에서 받은 Refresh Token (없으면 null)
     */
    void logout(String refreshToken);

    /**
     * 아이디 찾기 — PASS 본인인증 기반 가입 이메일(로그인 ID) 조회
     *
     * 흐름:
     *   1. 요청 값 검증 — null/빈 값 → INVALID_VERIFICATION_ID(400)
     *   2. PASS 본인인증 결과 검증 → CI 추출 (Provider 실패 시 BusinessException)
     *   3. CI SHA-256 hash 변환 → user_auth.identity_ci_hash 기준 가입 회원 조회
     *      - 없음 또는 비활성(탈퇴/차단) 회원 → USER_NOT_FOUND(404)
     *   4. email_encrypt AES 복호화 → 마스킹 처리 (docs: user****@example.com)
     *   5. 가입일 yyyy-MM-dd 포맷 → FindIdResponseDTO 반환
     *
     * @param identityVerificationId PASS 인증 후 발급받은 포트원 고유 ID (없으면 null)
     * @return 마스킹된 이메일 + 가입일
     */
    FindIdResponseDTO findId(String identityVerificationId);

    /**
     * 비밀번호 재설정 사전 단계 — 아이디 존재 확인
     *
     * 흐름:
     *   1. 요청 값 검증 — null/빈 값 → INVALID_PASSWORD_RESET_REQUEST(400)
     *   2. loginId(이메일/휴대폰) SHA-256 hash 로 회원 조회 → 없음/비활성(탈퇴/차단) → USER_NOT_FOUND(404)
     *   3. 존재하면 200 SUCCESS — PASS 본인인증 단계로 진행
     *
     * @param loginId 로그인 ID (이메일 또는 하이픈 없는 휴대폰 번호)
     */
    void checkPasswordResetId(String loginId);

    /**
     * 비밀번호 재설정 1단계 — 본인 확인 및 인증 토큰 발급
     *
     * 흐름:
     *   1. 요청 값 검증 (loginId/identityVerificationId 누락 → INVALID_PASSWORD_RESET_REQUEST)
     *   2. loginId(이메일/휴대폰) SHA-256 hash 로 회원 조회 → 없음/비활성 → USER_NOT_FOUND(404)
     *   3. PASS 본인인증 결과 검증 → CI 추출 (Provider 실패 시 BusinessException)
     *   4. CI SHA-256 hash 변환 → 회원의 identity_ci_hash 와 대조 → 불일치 → VERIFICATION_FAILED(400)
     *   5. UUID passwordResetToken 생성 → Redis(password:reset:{token})에 5분 TTL 저장
     *   6. passwordResetToken 반환
     *
     * @param request 비밀번호 재설정 1단계 요청 (loginId, identityVerificationId)
     * @return Redis 에 저장된 5분 유효 임시 토큰
     */
    PasswordVerifyResponseDTO verifyPasswordReset(PasswordVerifyRequestDTO request);

    /**
     * 비밀번호 재설정 2단계 — 비밀번호 변경
     *
     * 흐름:
     *   1. 요청 값 검증 (passwordResetToken 누락 → RESET_TIMEOUT_OR_INVALID_TOKEN)
     *   2. Redis(password:reset:{token}) 검증 — 없으면(만료/사용 완료/위조) RESET_TIMEOUT_OR_INVALID_TOKEN(400)
     *   3. 비밀번호 정책 검증 (영문/숫자/특수문자 포함 8자 이상 → WEAK_PASSWORD 422)
     *   4. 신규 비밀번호 BCrypt 암호화
     *   5. user_auth.password_hash 갱신 (없으면 RESET_TIMEOUT_OR_INVALID_TOKEN)
     *   6. 사용 완료 후 Redis 토큰 삭제 (1회성)
     *
     * @param request 비밀번호 재설정 2단계 요청 (passwordResetToken, newPassword)
     */
    void resetPassword(PasswordResetRequestDTO request);

    /**
     * 비밀번호 변경 — 로그인 사용자가 현재 비밀번호를 재입력해 본인 인증을 수행한 뒤 새 비밀번호로 변경
     *
     * 흐름:
     *   1. 요청 값 검증 (currentPassword/newPassword 누락·빈 값 → INVALID_PASSWORD_CHANGE_REQUEST 400)
     *   2. JWT 로그인 사용자 조회 + 상태 확인 (users) — 없음/비활성 → USER_NOT_FOUND 404
     *   3. user_auth.password_hash(BCrypt) 조회 — 없음 → USER_NOT_FOUND 404
     *   4. 현재 비밀번호 BCrypt 검증 — 불일치 → AUTH_INVALID_PASSWORD 400
     *   5. 신규 비밀번호 정책 검증 (영문/숫자/특수문자 포함 8자 이상 → WEAK_PASSWORD 422)
     *   6. 신규 비밀번호가 현재 비밀번호와 동일한지 BCrypt 대조 — 동일 → AUTH_SAME_PASSWORD 400
     *   7. 신규 비밀번호 BCrypt 암호화 (knowledge.md: 비밀번호 원문 저장 금지)
     *   8. user_auth.password_hash 갱신
     *   9. 기존 Refresh Token 전체 폐기 (Redis refresh:token:{userId} 삭제 — DB 커밋 확정 후)
     *      → 비밀번호 변경 후 기존 세션으로는 재발급 불가 (knowledge.md: 비밀번호 변경 후 기존 Refresh Token 전체 폐기)
     *
     * Access Token 은 Stateless 이므로 만료까지 유지된다 (knowledge.md: Logout 과 동일 — JWT 구조 변경/신규 발급 없음)
     *
     * @param userId  JWT 인증된 로그인 사용자 id (@CurrentUser — Controller 에서 주입)
     * @param request 비밀번호 변경 요청 (currentPassword, newPassword)
     */
    void changePassword(Long userId, ChangePasswordRequestDTO request);

    /**
     * PIN 번호 최초 설정 — 로그인 사용자의 기기(PIN) 등록
     *
     * 흐름:
     *   1. 요청 값 검증 (pinNumber/deviceId/deviceName 누락·100자 초과 → INVALID_PIN_SETUP_REQUEST 400)
     *   2. 회원 존재 + ACTIVE 상태 확인 (JWT 인증 userId 기준 → USER_NOT_FOUND 404)
     *   3. 기존 PIN 등록 여부 확인 (user_id + device_id) — 등록됨 → PIN_ALREADY_EXISTS 409
     *   4. PIN 형식 검증 (6자리 숫자 → INVALID_PIN_FORMAT 400)
     *   5. PIN BCrypt 단방향 암호화 (knowledge.md: PIN 원문 저장 금지)
     *   6. user_device insert
     *
     * @param userId  JWT 인증된 로그인 사용자 id (@CurrentUser — Controller 에서 주입)
     * @param request PIN 설정 요청 (pinNumber, deviceId, deviceName)
     */
    void setupPin(Long userId, PinSetupRequestDTO request);

    /**
     * 보안 PIN 번호 재설정 — 로그인 사용자가 PASS 본인인증을 다시 수행한 뒤 신규 PIN 으로 변경
     *
     * 흐름:
     *   1. 요청 값 검증 (identityVerificationId 누락·빈 값 → INVALID_VERIFICATION_ID 400)
     *   2. JWT 로그인 사용자 조회 (users + user_auth — identity_ci_hash 포함) — 없음/비활성 → USER_NOT_FOUND 404
     *   3. PASS 본인인증 결과 검증 → CI 추출 (실패 시 INVALID_VERIFICATION_ID)
     *   4. CI SHA-256 hash 대조 — 로그인 사용자의 identity_ci_hash 와 불일치 → VERIFICATION_FAILED 400
     *   5. 신규 PIN 형식 검증 (6자리 숫자 → INVALID_PIN_FORMAT 400)
     *   6. 신규 PIN 이 기존 PIN 과 동일한지 BCrypt 대조 (등록 기기 pin_hash 전체) — 동일 → SAME_AS_CURRENT_PIN 400
     *   7. PIN BCrypt 단방향 암호화 (knowledge.md: PIN 원문 저장 금지)
     *   8. user_device.pin_hash 갱신 (user_id 기준 등록 기기 전체) — 갱신 대상 없음 → PIN_NOT_REGISTERED 400
     *   9. PIN 실패 횟수 초기화 — 잠금 해제 (knowledge.md: "PASS 본인인증 후 PIN 재설정" = 잠금 해제 수단)
     *
     * Redis 임시 토큰/비밀번호 재설정 토큰은 사용하지 않는다 (PASS 인증 성공 시 즉시 변경)
     *
     * @param userId  JWT 인증된 로그인 사용자 id (@CurrentUser — Controller 에서 주입)
     * @param request PIN 재설정 요청 (identityVerificationId, pinNumber)
     */
    void resetPin(Long userId, PinResetRequestDTO request);
}
