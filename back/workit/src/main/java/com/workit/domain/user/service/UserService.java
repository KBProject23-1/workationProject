package com.workit.domain.user.service;

import com.workit.domain.user.dto.request.AccountPasswordVerifyRequestDTO;
import com.workit.domain.user.dto.request.EmailVerificationConfirmRequestDTO;
import com.workit.domain.user.dto.request.EmailVerificationRequestDTO;
import com.workit.domain.user.dto.request.PhoneChangeRequestDTO;
import com.workit.domain.user.dto.request.ProfileOnboardingRequestDTO;
import com.workit.domain.user.dto.request.ProfileUpdateRequestDTO;
import com.workit.domain.user.dto.request.UserWithdrawalRequestDTO;
import com.workit.domain.user.dto.response.EmailChangeResponseDTO;
import com.workit.domain.user.dto.response.EmailVerificationConfirmResponseDTO;
import com.workit.domain.user.dto.response.EmailVerificationResponseDTO;
import com.workit.domain.user.dto.response.MyProfileResponseDTO;
import com.workit.domain.user.dto.response.PhoneChangeResponseDTO;
import com.workit.domain.user.dto.response.ProfileOnboardingResponseDTO;

// User 도메인 Service (회원 기본 정보 / 프로필 / 회원 상태 담당 — knowledge.md User Domain 책임)
public interface UserService {

    /**
     * 내 프로필 조회 — 로그인 사용자의 기본 정보 + 프로필 정보 조회
     *
     * 흐름:
     *   1. 로그인 사용자(users + user_profile LEFT JOIN) 조회 — 없음/비활성 → USER_NOT_FOUND(404)
     *   2. email/name/phoneNumber AES 복호화 (Service Layer 에서만 — Controller/Mapper 금지)
     *   3. nickname/companyName 반환 (프로필 미등록 시 null)
     *
     * @param userId JWT 인증된 로그인 사용자 id (@CurrentUser — Controller 에서 주입)
     * @return email, name, phoneNumber, nickname, companyName
     */
    MyProfileResponseDTO getMyProfile(Long userId);

    /**
     * 프로필 최초 등록 — 회원가입 및 보안 PIN 설정 완료 후 nickname/companyName 최초 저장
     *
     * 흐름:
     *   1. 요청 값 검증 — nickname 필수/길이(50자), companyName 길이(100자) → INVALID_PROFILE_REQUEST(400)
     *   2. 로그인 사용자 존재 + ACTIVE 상태 확인 → USER_NOT_FOUND(404)
     *   3. 이미 프로필 등록 확인 (user_profile 1:1) → PROFILE_ALREADY_EXISTS(409)
     *   4. nickname 중복 확인 (user_profile.nickname UNIQUE) → DUPLICATE_NICKNAME(409)
     *   5. user_profile insert → ProfileOnboardingResponseDTO 반환 (profileId 포함)
     *
     * name/phoneNumber 는 요청에서 받지 않는다 — DB 에 저장된 본인인증 정보를 그대로 사용
     *
     * @param userId  JWT 인증된 로그인 사용자 id (@CurrentUser — Controller 에서 주입)
     * @param request 프로필 최초 등록 요청 (nickname 필수, companyName 선택)
     * @return profileId, userId, nickname, companyName
     */
    ProfileOnboardingResponseDTO onboardProfile(Long userId, ProfileOnboardingRequestDTO request);

    /**
     * 프로필 수정 — 로그인 사용자의 nickname/companyName 부분 수정 (PATCH: 전달된 값만)
     *
     * 흐름:
     *   1. 로그인 사용자 존재 + ACTIVE 상태 확인 → USER_NOT_FOUND(404)
     *   2. 프로필 존재 확인 (최초 등록 전 사용자는 수정 불가) → PROFILE_NOT_FOUND(404)
     *   3. 요청 값 검증 — 수정 대상 필드 최소 1개, nickname 길이(50자), companyName 길이(100자)
     *      → INVALID_PROFILE_REQUEST(400)
     *   4. nickname 중복 확인 — 기존 nickname 과 동일하면 허용, 변경 시 다른 사용자 중복 조회
     *      → DUPLICATE_NICKNAME(409)
     *   5. 전달된 필드만 동적 UPDATE (name/phoneNumber/email 은 수정 불가 — 요청에서 받지 않음)
     *
     * name/phoneNumber/email 은 PASS 본인인증 기반 기존 값 유지 (재인증 API 경유 — knowledge.md)
     *
     * @param userId  JWT 인증된 로그인 사용자 id (@CurrentUser — Controller 에서 주입)
     * @param request 프로필 수정 요청 (nickname/companyName 중 하나 이상)
     */
    void updateProfile(Long userId, ProfileUpdateRequestDTO request);

    /**
     * 회원 탈퇴 — 로그인 사용자가 현재 비밀번호를 재확인한 뒤 Soft Delete 처리
     *
     * 흐름 (knowledge.md Withdrawal Policy):
     *   1. 요청 값 검증 — password 필수 (null/빈 값 → COMMON_INVALID_REQUEST 400)
     *   2. 로그인 사용자 존재/상태 확인 — 없음 → USER_NOT_FOUND(404),
     *      이미 WITHDRAWN → USER_ALREADY_WITHDRAWN(409), 기타 비활성 → USER_NOT_FOUND(404)
     *   3. 현재 비밀번호 검증 — AuthService.verifyCurrentPassword 위임
     *      (BCrypt matches — 불일치 → AUTH_INVALID_PASSWORD 400)
     *   4. 전자지갑 잔액 확인 — WalletService.getBalance 위임,
     *      잔액 > 0 → WALLET_BALANCE_REMAINING(409) 으로 탈퇴 차단 (BigDecimal.compareTo)
     *   5. users.status = WITHDRAWN + deleted_at 기록 (Soft Delete)
     *   6. 모든 Refresh Token 세션 revoke — AuthService.revokeAllRefreshSessions 위임
     *      (Redis 는 DB 트랜잭션과 분리 — DB 커밋 확정 후 처리)
     *
     * 금융 거래/결제/지갑 데이터는 삭제하지 않는다. 잔액 환불/0 원 처리도 하지 않는다.
     * 비밀번호 원문은 로그에 출력하지 않는다.
     *
     * @param userId  JWT 인증된 로그인 사용자 id (@CurrentUser — Controller 에서 주입)
     * @param request 회원 탈퇴 요청 (password 필수)
     */
    void withdraw(Long userId, UserWithdrawalRequestDTO request);

    /**
     * 계정 설정 진입용 비밀번호 재인증 — 로그인 사용자가 현재 비밀번호를 재입력해 본인임을 확인
     *
     * 흐름:
     *   1. 요청 값 검증 — password 필수 (null/빈 값/공백 → COMMON_INVALID_REQUEST 400)
     *   2. 로그인 사용자 존재/상태 확인 — 없음 → USER_NOT_FOUND(404),
     *      이미 WITHDRAWN → USER_ALREADY_WITHDRAWN(409), 기타 비활성 → USER_NOT_FOUND(404)
     *   3. 현재 비밀번호 검증 — AuthService.verifyCurrentPassword 위임
     *      (BCrypt matches — 불일치 → AUTH_INVALID_PASSWORD 400)
     *
     * 재인증 성공 여부를 Redis/DB/Session 등에 별도로 저장하지 않으며,
     * Access Token / Refresh Token 을 새로 발급하지 않는다 (읽거나 관리하지도 않는다).
     * 성공 후 프론트가 계정 설정 화면으로 이동하며, 민감 작업(휴대폰/이메일/비밀번호 변경, 탈퇴)은
     * 각 API 에서 별도의 인증 절차를 수행한다 (프론트 재인증 상태 신뢰 금지).
     *
     * @param userId  JWT 인증된 로그인 사용자 id (@CurrentUser — Controller 에서 주입)
     * @param request 비밀번호 재인증 요청 (password 필수)
     */
    void verifyAccountPassword(Long userId, AccountPasswordVerifyRequestDTO request);

    /**
     * 휴대폰 번호 변경 — Mock PASS 본인인증 결과를 검증해 인증된 휴대폰 번호로 변경
     *
     * 흐름 (docs: 휴대폰 번호 변경):
     *   1. 요청 값 검증 — identityVerificationId 필수 (null/빈 값 → INVALID_VERIFICATION_ID 400)
     *   2. 로그인 사용자 존재/상태 확인 — 없음 → USER_NOT_FOUND(404),
     *      이미 WITHDRAWN → USER_ALREADY_WITHDRAWN(409), 기타 비활성 → USER_NOT_FOUND(404)
     *   3. PASS 본인인증 결과 검증 — IdentityVerificationProvider.verify 재사용
     *      (세션 없음/TTL 만료/status != VERIFIED/used == true → INVALID_VERIFICATION_ID 400)
     *   4. 본인인증 이름과 DB 사용자 이름 대조 — PASS 인증이 현재 사용자 본인 인증인지 확인
     *      (불일치 → 다른 사용자에게 발급된 identityVerificationId → VERIFICATION_FAILED 400)
     *   5. 인증된 휴대폰 번호 조회 (프론트가 전달한 phoneNumber 는 사용하지 않는다)
     *   6. 현재 휴대폰 번호와 동일 → PHONE_SAME_AS_CURRENT(400)
     *   7. 다른 사용자 등록 여부 확인 (users.phone_number_hash UNIQUE, 본인 제외)
     *      → PHONE_ALREADY_IN_USE(409)
     *   8. users.phone_number_hash/phone_number_encrypt 갱신 (SHA-256 + AES-256 — Service Layer)
     *   9. 변경된 휴대폰 번호 응답 (Access/Refresh Token 발급·관리 없음)
     *
     * @param userId  JWT 인증된 로그인 사용자 id (@CurrentUser — Controller 에서 주입)
     * @param request 휴대폰 번호 변경 요청 (identityVerificationId 필수 — phoneNumber 는 받지 않음)
     * @return 변경된 휴대폰 번호 (updatedPhone)
     */
    PhoneChangeResponseDTO changePhone(Long userId, PhoneChangeRequestDTO request);

    /**
     * 이메일 인증번호 발송 — 계정 설정에서 이메일 변경 전, 변경할 새 이메일로 인증번호를 발송한다
     *
     * 흐름 (docs: 이메일 인증번호 발송):
     *   1. 로그인 사용자 존재/상태 확인 — 없음 → USER_NOT_FOUND(404),
     *      이미 WITHDRAWN → USER_ALREADY_WITHDRAWN(409), 기타 비활성 → USER_NOT_FOUND(404)
     *   2. 요청 이메일 검증 — 필수 + 형식 (EmailValidator 공통 정책) → INVALID_EMAIL_REQUEST(400)
     *   3. 현재 사용자의 이메일과 동일 → EMAIL_SAME_AS_CURRENT(400)
     *   4. 다른 사용자가 이미 사용 중인 이메일(users.email_hash UNIQUE, 본인 제외)
     *      → EMAIL_ALREADY_IN_USE(409)
     *   5. Mock 이메일 인증번호 발급 — EmailVerificationService 위임 (6자리 숫자 생성 + 임시 저장)
     *      - 실제 이메일은 발송하지 않으며 [MOCK EMAIL] 로그로 인증번호를 확인한다 (개발 환경)
     *
     * 인증번호는 DB 에 저장하지 않는다 (docs: 인증번호를 DB 에 저장할 필요 없음 — Mock 임시 저장소 사용).
     * Access/Refresh Token 을 읽거나 관리하지 않으며 발급도 하지 않는다 (docs 보안 주의사항).
     *
     * @param userId  JWT 인증된 로그인 사용자 id (@CurrentUser — Controller 에서 주입)
     * @param request 이메일 인증번호 발송 요청 (email 필수)
     * @return 인증번호를 발송한 이메일 (정규화된 값 — docs 응답 data.email)
     */
    EmailVerificationResponseDTO sendEmailVerification(Long userId, EmailVerificationRequestDTO request);

    /**
     * 이메일 인증번호 확인 — 이메일 변경 전, 발송된 인증번호가 올바른지 검증하고 인증 완료 상태를 저장한다
     *
     * 흐름 (docs: 이메일 인증번호 확인):
     *   1. 로그인 사용자 존재/상태 확인 — 없음 → USER_NOT_FOUND(404),
     *      이미 WITHDRAWN → USER_ALREADY_WITHDRAWN(409), 기타 비활성 → USER_NOT_FOUND(404)
     *   2. 요청 값 검증 — email 필수 + 형식(EmailValidator 공통 정책) → INVALID_EMAIL_REQUEST(400),
     *      verificationCode 필수(null/빈 값/공백) → EMAIL_VERIFICATION_CODE_INVALID(400)
     *   3. Mock 이메일 인증번호 검증 — EmailVerificationService 위임
     *      - 인증정보 없음 → EMAIL_VERIFICATION_NOT_FOUND(400)
     *      - 인증번호 만료(5분) → EMAIL_VERIFICATION_CODE_EXPIRED(400)
     *      - 이미 인증 완료된 인증번호 재사용 → EMAIL_ALREADY_VERIFIED(400)
     *      - 인증번호 불일치 → EMAIL_VERIFICATION_CODE_INVALID(400)
     *   4. 인증 성공 시 해당 이메일을 인증 완료 상태(VERIFIED)로 저장 —
     *      이후 이메일 변경 API 가 인증 완료된 이메일을 조회/사용한다
     *
     * 이 API 는 인증번호 검증 → 인증 완료 상태 저장까지만 담당하며, 실제 이메일 변경은 하지 않는다
     * (이메일 변경은 별도의 PATCH /api/v1/users/me/email API 에서 처리 — docs).
     * Access/Refresh Token 을 읽거나 관리하지 않으며 발급도 하지 않는다 (docs 보안 주의사항).
     *
     * @param userId  JWT 인증된 로그인 사용자 id (@CurrentUser — Controller 에서 주입)
     * @param request 이메일 인증번호 확인 요청 (email, verificationCode 필수)
     * @return 이메일 인증 완료 여부 (성공 시 true)
     */
    EmailVerificationConfirmResponseDTO confirmEmailVerification(Long userId, EmailVerificationConfirmRequestDTO request);

    /**
     * 이메일 변경 — 이메일 인증번호 확인을 완료한 사용자의 이메일을 인증된 이메일로 변경한다
     *
     * 흐름 (docs: 이메일 변경):
     *   1. 로그인 사용자 존재/상태 확인 — 없음 → USER_NOT_FOUND(404),
     *      이미 WITHDRAWN → USER_ALREADY_WITHDRAWN(409), 기타 비활성 → USER_NOT_FOUND(404)
     *   2. 이메일 인증 완료 정보 조회 — EmailVerificationService.getVerifiedEmail 위임
     *      (Request Body 를 받지 않으므로 인증 완료된 이메일은 서버가 인증 세션에서 조회한다)
     *      - 인증정보 없음/만료/인증 미완료 → EMAIL_VERIFICATION_REQUIRED(400)
     *   3. 인증 완료된 이메일이 현재 이메일과 동일 → EMAIL_SAME_AS_CURRENT(400)
     *   4. 다른 사용자가 이미 사용 중인 이메일(users.email_hash UNIQUE, 본인 제외)
     *      → EMAIL_ALREADY_IN_USE(409)
     *   5. users.email_hash/email_encrypt 갱신 (SHA-256 + AES-256 — Service Layer)
     *   6. 이메일 변경 성공 후 인증 세션 소비(삭제) — 동일 인증 결과 재사용 방지
     *      (Redis 삭제는 DB 커밋 확정 후 처리)
     *   7. 변경된 이메일 응답 (Access/Refresh Token 발급·관리 없음)
     *
     * 클라이언트가 이메일 주소를 전달하지 않는다 — 인증 완료된 이메일은 서버가
     * EmailVerificationStore 에서 조회한 값만 사용한다 (docs 보안 조건: 인증하지 않은 이메일 변경 차단).
     * 비밀번호를 다시 받지 않으며, Access/Refresh Token 을 읽거나 관리하지 않는다 (docs 주의사항).
     *
     * @param userId JWT 인증된 로그인 사용자 id (@CurrentUser — Controller 에서 주입)
     * @return 변경된 이메일 (updatedEmail)
     */
    EmailChangeResponseDTO changeEmail(Long userId);
}
