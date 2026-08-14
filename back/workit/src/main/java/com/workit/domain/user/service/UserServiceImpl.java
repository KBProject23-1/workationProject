package com.workit.domain.user.service;

import com.workit.domain.auth.service.AuthService;
import com.workit.domain.user.dto.request.ProfileOnboardingRequestDTO;
import com.workit.domain.user.dto.request.ProfileUpdateRequestDTO;
import com.workit.domain.user.dto.request.UserWithdrawalRequestDTO;
import com.workit.domain.user.dto.response.MyProfileResponseDTO;
import com.workit.domain.user.dto.response.ProfileOnboardingResponseDTO;
import com.workit.domain.user.exception.UserErrorCode;
import com.workit.domain.user.mapper.UserMapper;
import com.workit.domain.user.vo.MyProfileVO;
import com.workit.domain.user.vo.UserProfileVO;
import com.workit.domain.wallet.exception.WalletErrorCode;
import com.workit.domain.wallet.service.WalletService;
import com.workit.exception.BusinessException;
import com.workit.exception.CommonErrorCode;
import com.workit.global.util.PersonalDataCipher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

// User 도메인 Service 구현체
// - 검증/복호화/트랜잭션 경계는 전부 Service Layer 에서 수행 (Controller/Mapper 에서 금지)
// - 개인정보(이메일/이름/전화번호)는 Service 에서만 복호화하며 로그 출력 금지 (knowledge.md)
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    // 비밀번호 검증/Refresh 세션 revoke 는 Auth 도메인 책임 — Service 위임 (knowledge.md: User Domain 에 인증 로직 금지)
    private final AuthService authService;

    // 지갑 잔액 조회는 Wallet 도메인 책임 — Service 위임 (WalletMapper 직접 호출 금지)
    private final WalletService walletService;

    /** 회원 서비스 이용 가능 상태 (knowledge.md: users.status 기본값) */
    private static final String USER_STATUS_ACTIVE = "ACTIVE";

    /** 회원 탈퇴 상태 (knowledge.md Withdrawal Policy: users.status = WITHDRAWN) */
    private static final String USER_STATUS_WITHDRAWN = "WITHDRAWN";

    /** user_profile.nickname VARCHAR(50) — 초과 시 DB 오류(500) 대신 400 으로 처리 */
    private static final int NICKNAME_MAX_LENGTH = 50;

    /** user_profile.company_name VARCHAR(100) — 초과 시 DB 오류(500) 대신 400 으로 처리 */
    private static final int COMPANY_NAME_MAX_LENGTH = 100;

    @Override
    // SELECT 만 수행하므로 읽기 전용 트랜잭션 (AuthServiceImpl.checkEmailAvailability 와 동일)
    @Transactional(readOnly = true)
    public MyProfileResponseDTO getMyProfile(Long userId) {

        // 1. 로그인 사용자 기본 정보 + 프로필 조회 (users + user_profile LEFT JOIN)
        //    - 없음/비활성(탈퇴/차단) 회원은 계정 존재 여부를 노출하지 않고 USER_NOT_FOUND(404) 로 처리
        //      (findId/refreshAccessToken 과 동일 정책 — knowledge.md)
        MyProfileVO profile = userMapper.selectMyProfileByUserId(userId);
        if (profile == null || !USER_STATUS_ACTIVE.equals(profile.getStatus())) {
            throw new BusinessException(UserErrorCode.USER_NOT_FOUND);
        }

        // 2. 개인정보 복호화 — Service Layer 에서만 수행 (Controller/Mapper 금지 — knowledge.md)
        //    - email: 회원가입 시 입력한 로그인 ID
        //    - name/phoneNumber: PASS 본인인증으로 저장된 실명/휴대폰 번호
        String email = PersonalDataCipher.decrypt(profile.getEmailEncrypt());
        String name = PersonalDataCipher.decrypt(profile.getNameEncrypt());
        String phoneNumber = PersonalDataCipher.decrypt(profile.getPhoneNumberEncrypt());

        // 3. 응답 생성 — nickname/companyName 은 프로필 미등록 시 null (docs: 최초 등록 전 null 가능)
        //    - 개인정보 원문(복호화 값) 로그 출력 금지 (knowledge.md)
        log.info("내 프로필 조회 성공 - userId={}", profile.getUserId());

        return MyProfileResponseDTO.of(email, name, phoneNumber,
                profile.getNickname(), profile.getCompanyName());
    }

    @Override
    @Transactional
    // user_profile INSERT(DB 쓰기) 하나의 작업이므로 트랜잭션 경계를 Service 에 둔다
    // (AuthServiceImpl.setupPin 과 동일 — 검증은 전부 Service Layer 에서 수행)
    public ProfileOnboardingResponseDTO onboardProfile(Long userId, ProfileOnboardingRequestDTO request) {

        // 1. 로그인 사용자 존재 + 상태 확인 (검증 순서: 사용자 확인 → 프로필 기등록 → 요청 값)
        //    - 없음/비활성(탈퇴/차단) 회원은 계정 존재 여부를 노출하지 않고 USER_NOT_FOUND(404) 로 처리
        //      (getMyProfile/findId 와 동일 정책)
        MyProfileVO existing = userMapper.selectMyProfileByUserId(userId);
        if (existing == null || !USER_STATUS_ACTIVE.equals(existing.getStatus())) {
            throw new BusinessException(UserErrorCode.USER_NOT_FOUND);
        }

        // 2. 이미 프로필 등록 여부 확인 — user_profile.user_id UNIQUE(1:1)
        //    - 이미 등록된 사용자는 최초 등록 API 호출 불가 → PROFILE_ALREADY_EXISTS(409)
        //      (ERD: ux_user_profile_user_id — 한 유저당 프로필은 단 하나)
        if (existing.getProfileId() != null) {
            throw new BusinessException(UserErrorCode.PROFILE_ALREADY_EXISTS);
        }

        // 3. 요청 값 검증 — nickname 필수/길이, companyName 길이 (javax.validation 미사용 환경 → Service Layer)
        //    - 실패 시 INVALID_PROFILE_REQUEST(400) (docs)
        validateOnboardingRequest(request);

        // 4. nickname 중복 확인 — user_profile.nickname UNIQUE
        //    - 사용 중인 닉네임이면 DUPLICATE_NICKNAME(409) (docs)
        String nickname = request.getNickname().trim();
        if (userMapper.countByNickname(nickname) > 0) {
            throw new BusinessException(UserErrorCode.DUPLICATE_NICKNAME);
        }

        // 5. user_profile insert (nickname/company_name 저장)
        //    - name/phoneNumber 는 요청에서 받지 않으며, 회원가입 시 저장된 users 테이블 값을 그대로 사용
        //    - companyName 은 선택값 — 빈 문자열은 null 로 정규화
        //    - 사전 중복 체크(SELECT)와 실제 insert 사이의 Race Condition 은
        //      DB UNIQUE 제약(nickname, user_id)이 최종 방어선 — DuplicateKeyException → 409 로 변환
        UserProfileVO userProfile = new UserProfileVO();
        userProfile.setUserId(userId);
        userProfile.setNickname(nickname);
        userProfile.setCompanyName(normalizeCompanyName(request.getCompanyName()));
        try {
            userMapper.insertUserProfile(userProfile);
        } catch (DuplicateKeyException e) {
            throw mapDuplicateKeyException(e);
        }

        // 6. Audit 로그 — userId 만 기록 (닉네임/회사명 등 로그 출력 금지 — knowledge.md)
        log.info("프로필 최초 등록 성공 - userId={}", userId);

        // 7. 응답 생성 — profileId 는 insert 후 채워진 PK (useGeneratedKeys)
        //    - insert 성공 후 userProfile 필드 값을 그대로 사용 (저장된 trim/정규화 값)
        return ProfileOnboardingResponseDTO.of(
                userProfile.getId(), userId, userProfile.getNickname(), userProfile.getCompanyName());
    }

    @Override
    @Transactional
    // user_profile UPDATE(DB 쓰기) 하나의 작업이므로 트랜잭션 경계를 Service 에 둔다
    // (onboardProfile 과 동일 — 검증은 전부 Service Layer 에서 수행)
    public void updateProfile(Long userId, ProfileUpdateRequestDTO request) {

        // 1. 로그인 사용자 존재 + 상태 확인
        //    - 없음/비활성(탈퇴/차단) 회원은 계정 존재 여부를 노출하지 않고 USER_NOT_FOUND(404) 로 처리
        //      (getMyProfile/onboardProfile 과 동일 정책)
        MyProfileVO existing = userMapper.selectMyProfileByUserId(userId);
        if (existing == null || !USER_STATUS_ACTIVE.equals(existing.getStatus())) {
            throw new BusinessException(UserErrorCode.USER_NOT_FOUND);
        }

        // 2. 프로필 존재 확인 — 최초 등록되지 않은 사용자는 수정 불가 → PROFILE_NOT_FOUND(404)
        //    (docs: 내 프로필 정보 수정 — 프로필 미등록 시 수정 API 호출 불가)
        if (existing.getProfileId() == null) {
            throw new BusinessException(UserErrorCode.PROFILE_NOT_FOUND);
        }

        // 3. 요청 값 검증 — 수정 대상 필드 최소 1개, nickname 길이, companyName 길이 → INVALID_PROFILE_REQUEST(400)
        //    - PATCH 방식: nickname 또는 companyName 중 전달된 것만 수정
        //    - companyName 은 null/빈 값 전달도 수정 대상 (소속 회사 삭제 → NULL 저장)
        //    - name/phoneNumber/email 은 요청에서 받지 않는다 (DTO 에 미존재 — 개인정보 수정 금지)
        String nickname = validateUpdateRequest(request);
        // null/빈 문자열은 NULL 저장(삭제) 요청으로 정규화 — 실제 UPDATE 여부는 updateCompanyName 으로 판단
        String companyName = isBlank(request.getCompanyName()) ? null : request.getCompanyName().trim();
        boolean updateCompanyName = request.isCompanyNameProvided();

        // 4. nickname 중복 확인 (nickname 변경 요청이 있는 경우에만)
        //    - 기존 nickname 과 동일하면 허용 (본인 유지)
        //    - 다른 nickname 으로 변경 시 다른 사용자의 중복 조회 → DUPLICATE_NICKNAME(409)
        if (nickname != null && !nickname.equals(existing.getNickname())
                && userMapper.countByNicknameExcludingUserId(nickname, userId) > 0) {
            throw new BusinessException(UserErrorCode.DUPLICATE_NICKNAME);
        }

        // 5. 전달된 필드만 동적 UPDATE — updateCompanyName/nickname null 여부를 Mapper XML <if> 로 판단한다
        //    - companyName 이 요청에 포함되면 값(또는 NULL)으로 저장, 미포함이면 기존 값 유지
        //    - name/phoneNumber/email 은 수정 대상이 아니며 기존 PASS 인증 정보 유지
        //    - 사전 중복 체크(SELECT)와 실제 UPDATE 사이의 Race Condition 은
        //      DB UNIQUE 제약(nickname)이 최종 방어선 — DuplicateKeyException → 409 로 변환
        UserProfileVO userProfile = new UserProfileVO();
        userProfile.setUserId(userId);
        userProfile.setNickname(nickname);
        userProfile.setCompanyName(companyName);
        userProfile.setUpdateCompanyName(updateCompanyName);
        try {
            int updated = userMapper.updateUserProfile(userProfile);
            // 조회(SELECT)와 수정(UPDATE) 사이 프로필이 소실된 경우(이론적) — 안전장치
            if (updated == 0) {
                throw new BusinessException(UserErrorCode.PROFILE_NOT_FOUND);
            }
        } catch (DuplicateKeyException e) {
            throw mapDuplicateKeyException(e);
        }

        // 6. Audit 로그 — userId 만 기록 (닉네임/회사명 등 로그 출력 금지 — knowledge.md)
        log.info("프로필 수정 성공 - userId={}", userId);
    }

    @Override
    @Transactional
    // 회원 탈퇴는 하나의 Business UseCase — users UPDATE(DB 쓰기) 하나이지만
    // 검증(비밀번호/잔액)과 상태 변경을 하나의 Transaction Boundary 로 관리한다
    // (knowledge.md Transaction Rules — Redis 세션 revoke 는 DB 트랜잭션과 분리: afterCommit)
    public void withdraw(Long userId, UserWithdrawalRequestDTO request) {

        // 1. 요청 값 검증 — password 필수 (null/빈 값 → 400)
        //    (javax.validation 미사용 환경 → Service Layer 에서 수행 — ChangePasswordRequestDTO 와 동일)
        //    - 비밀번호 원문은 로그에 출력하지 않는다 (민감정보)
        if (request == null || isBlank(request.getPassword())) {
            throw new BusinessException(CommonErrorCode.COMMON_INVALID_REQUEST);
        }

        // 2. 로그인 사용자 존재 + 상태 확인 (users.status)
        //    - 없음 → USER_NOT_FOUND(404)
        //    - 이미 WITHDRAWN → USER_ALREADY_WITHDRAWN(409) (재탈퇴 차단 — 상태 충돌)
        //    - BLOCKED/PENDING 등 기타 비활성 → 계정 존재 여부를 노출하지 않고 USER_NOT_FOUND(404)
        //      (getMyProfile/changePassword 와 동일 정책)
        MyProfileVO user = userMapper.selectMyProfileByUserId(userId);
        if (user == null) {
            throw new BusinessException(UserErrorCode.USER_NOT_FOUND);
        }
        if (USER_STATUS_WITHDRAWN.equals(user.getStatus())) {
            throw new BusinessException(UserErrorCode.USER_ALREADY_WITHDRAWN);
        }
        if (!USER_STATUS_ACTIVE.equals(user.getStatus())) {
            throw new BusinessException(UserErrorCode.USER_NOT_FOUND);
        }

        // 3. 현재 비밀번호 검증 — Auth 도메인 위임 (BCrypt matches, 불일치 → AUTH_INVALID_PASSWORD 400)
        //    - changePassword 와 동일한 검증 로직을 재사용한다 (중복 구현 금지)
        //    - 비밀번호 검증 전에 잔액/탈퇴 여부를 노출하지 않아 본인 확인 우선 (민감 작업 정책)
        authService.verifyCurrentPassword(userId, request.getPassword());

        // 4. 전자지갑 잔액 확인 — Wallet 도메인 위임 (순수 조회, 생성/변경 부작용 없음)
        //    - 잔액이 0 보다 크면 탈퇴 차단 → WALLET_BALANCE_REMAINING(409)
        //      (BigDecimal.compareTo 사용 — knowledge.md: 금액 비교는 compareTo)
        //    - 지갑 미존재/잔액 NULL 은 0 으로 간주 (탈퇴 허용)
        //    - 잔액을 환불하거나 0 으로 만들지 않는다
        BigDecimal balance = walletService.getBalance(userId);
        if (balance != null && balance.compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException(WalletErrorCode.WALLET_BALANCE_REMAINING);
        }

        // 5. users.status = WITHDRAWN + deleted_at 기록 (Soft Delete)
        //    - 금융 거래/결제/지갑 데이터는 삭제하지 않는다 (users 테이블만 UPDATE)
        //    - WHERE status != 'WITHDRAWN' — 조회-갱신 사이 동시 탈퇴 요청(Race Condition)이면
        //      0 row 반환 → USER_ALREADY_WITHDRAWN(409) 로 최종 방어
        int updated = userMapper.updateUserStatusToWithdrawn(userId);
        if (updated == 0) {
            throw new BusinessException(UserErrorCode.USER_ALREADY_WITHDRAWN);
        }

        // 6. 모든 Refresh Token 세션 revoke — Auth 도메인 위임
        //    - Redis(refresh:token:{userId}) 삭제 — DB 커밋 확정 후(afterCommit) 수행
        //      (Redis 는 DB 트랜잭션과 동일한 트랜잭션으로 취급하지 않는 기존 정책)
        //    - Access Token 은 Stateless — Access Token Blacklist 미사용 (만료까지 유지)
        authService.revokeAllRefreshSessions(userId);

        // 7. Audit 로그 (knowledge.md Audit Log Policy: 회원 탈퇴 기록 대상)
        //    - userId 만 기록 — 비밀번호/토큰/개인정보 원문은 로그에 포함하지 않는다
        log.info("회원 탈퇴 성공 - userId={}", userId);
    }

    /**
     * 프로필 수정 요청 값 검증 → INVALID_PROFILE_REQUEST(400)
     * - javax.validation 미사용 환경 → Service Layer 에서 수행 (Auth 도메인과 동일)
     * - 수정 대상 필드(nickname/companyName)가 최소 하나 이상 존재해야 함 (PATCH 특성)
     * - nickname: 존재 시 저장될 값(trim 후) 기준 VARCHAR(50) 초과 금지
     * - companyName: 존재 시 저장될 값(trim 후) 기준 VARCHAR(100) 초과 금지
     *
     * @return 수정 대상 nickname (미전달 시 null — UPDATE 제외 대상)
     */
    private String validateUpdateRequest(ProfileUpdateRequestDTO request) {
        if (request == null) {
            throw new BusinessException(UserErrorCode.INVALID_PROFILE_REQUEST);
        }

        boolean hasNickname = !isBlank(request.getNickname());
        // companyName 은 null/빈 값(삭제 요청)도 수정 대상으로 본다 — 요청 본문 포함 여부 기준
        boolean hasCompanyName = request.isCompanyNameProvided();

        // PATCH: 수정 대상 필드가 최소 하나 이상 존재해야 함
        //   (nickname 미전달 + companyName 미전달이면 수정할 내용이 없음 → 400)
        if (!hasNickname && !hasCompanyName) {
            throw new BusinessException(UserErrorCode.INVALID_PROFILE_REQUEST);
        }

        // DB 컬럼 길이 초과(VARCHAR(50)/VARCHAR(100))로 인한 500 오류 방지 — trim 후 길이 기준
        if (hasNickname && request.getNickname().trim().length() > NICKNAME_MAX_LENGTH) {
            throw new BusinessException(UserErrorCode.INVALID_PROFILE_REQUEST);
        }
        // null/빈 값(삭제 요청)은 길이 검증 제외 — 값이 있는 경우에만 VARCHAR(100) 검증
        if (hasCompanyName && !isBlank(request.getCompanyName())
                && request.getCompanyName().trim().length() > COMPANY_NAME_MAX_LENGTH) {
            throw new BusinessException(UserErrorCode.INVALID_PROFILE_REQUEST);
        }

        return hasNickname ? request.getNickname().trim() : null;
    }

    /**
     * 프로필 최초 등록 요청 값 검증 — nickname 필수/길이, companyName 길이 → INVALID_PROFILE_REQUEST(400)
     * - javax.validation 미사용 환경 → Service Layer 에서 수행 (Auth 도메인과 동일)
     * - nickname: 필수 (null/빈 값 금지), 저장될 값(trim 후) 기준 VARCHAR(50) 초과 금지
     *   (signup 의 닉네임 길이 검증과 동일 패턴 — AuthServiceImpl)
     * - companyName: 선택 (null 허용), 저장될 값(trim 후) 기준 VARCHAR(100) 초과 금지
     */
    private void validateOnboardingRequest(ProfileOnboardingRequestDTO request) {
        if (request == null || isBlank(request.getNickname())) {
            throw new BusinessException(UserErrorCode.INVALID_PROFILE_REQUEST);
        }
        // DB 컬럼 길이 초과(VARCHAR(50))로 인한 500 오류 방지 — 저장될 값(trim 후) 길이 기준
        if (request.getNickname().trim().length() > NICKNAME_MAX_LENGTH) {
            throw new BusinessException(UserErrorCode.INVALID_PROFILE_REQUEST);
        }
        // companyName 은 선택값 — null/빈 값이 아닌 경우에만 길이 검증 (VARCHAR(100))
        if (request.getCompanyName() != null
                && !isBlank(request.getCompanyName())
                && request.getCompanyName().trim().length() > COMPANY_NAME_MAX_LENGTH) {
            throw new BusinessException(UserErrorCode.INVALID_PROFILE_REQUEST);
        }
    }

    /**
     * companyName 정규화 — null/빈 문자열은 null 로 저장 (선택값, ERD: company_name VARCHAR(100) NULL)
     */
    private String normalizeCompanyName(String companyName) {
        if (isBlank(companyName)) {
            return null;
        }
        return companyName.trim();
    }

    /**
     * DB UNIQUE 제약 위반(DuplicateKeyException)을 도메인 에러 코드로 매핑한다.
     * - 동시 요청으로 인한 UNIQUE 충돌 시 500 대신 명확한 409 를 반환 (signup 의 mapDuplicateKeyException 과 동일 패턴)
     * - 사전 중복 체크(SELECT)를 통과했지만 동시 요청에 의해 UNIQUE 위반이 발생하는 Race Condition 방어선
     */
    private BusinessException mapDuplicateKeyException(DuplicateKeyException e) {
        String message = String.valueOf(e.getMessage());
        if (message.contains("ux_user_profile_nickname") || message.contains("user_profile.nickname")) {
            return new BusinessException(UserErrorCode.DUPLICATE_NICKNAME);
        }
        if (message.contains("ux_user_profile_user_id") || message.contains("user_profile.user_id")) {
            // 동일 사용자의 동시 최초 등록 요청 — 1:1 프로필 정책상 재등록으로 처리
            return new BusinessException(UserErrorCode.PROFILE_ALREADY_EXISTS);
        }
        // 식별되지 않은 UNIQUE 충돌 — 응답에 제약조건명/테이블명 노출 금지 (knowledge.md)
        return new BusinessException(UserErrorCode.DUPLICATE_NICKNAME);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
