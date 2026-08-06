package com.workit.domain.auth.service;

import com.workit.domain.auth.LoginType;
import com.workit.domain.auth.dto.request.LoginRequestDTO;
import com.workit.domain.auth.dto.request.PasswordResetRequestDTO;
import com.workit.domain.auth.dto.request.PasswordVerifyRequestDTO;
import com.workit.domain.auth.dto.request.PinResetRequestDTO;
import com.workit.domain.auth.dto.request.PinSetupRequestDTO;
import com.workit.domain.auth.dto.request.SignupRequestDTO;
import com.workit.domain.auth.dto.response.EmailAvailabilityResponseDTO;
import com.workit.domain.auth.dto.response.FindIdResponseDTO;
import com.workit.domain.auth.dto.response.IdentityVerificationResponseDTO;
import com.workit.domain.auth.dto.response.LoginResponseDTO;
import com.workit.domain.auth.dto.response.PasswordVerifyResponseDTO;
import com.workit.domain.auth.dto.response.RefreshTokenResponseDTO;
import com.workit.domain.auth.dto.response.TermsListResponseDTO;
import com.workit.domain.auth.dto.response.TermsResponseDTO;
import com.workit.domain.auth.exception.AuthErrorCode;
import com.workit.domain.auth.mapper.AuthMapper;
import com.workit.domain.auth.provider.IdentityVerificationProvider;
import com.workit.domain.auth.provider.IdentityVerificationResult;
import com.workit.domain.auth.util.EmailMasker;
import com.workit.domain.auth.util.JwtTokenProvider;
import com.workit.domain.auth.util.SignupTokenProvider;
import com.workit.domain.auth.vo.LoginUserVO;
import com.workit.domain.auth.vo.UserAuthVO;
import com.workit.domain.auth.vo.UserDeviceVO;
import com.workit.domain.auth.vo.UserProfileVO;
import com.workit.domain.auth.vo.UserVO;
import com.workit.domain.wallet.service.WalletService;
import com.workit.exception.BusinessException;
import com.workit.global.util.EmailValidator;
import com.workit.global.util.PasswordEncryptor;
import com.workit.global.util.PersonalDataCipher;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.format.DateTimeFormatter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthMapper authMapper;
    private final IdentityVerificationProvider identityVerificationProvider;
    private final SignupTokenProvider signupTokenProvider;
    private final SignupVerificationStore signupVerificationStore;
    private final WalletService walletService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenStore refreshTokenStore;
    private final LoginFailCounter loginFailCounter;
    private final PasswordResetTokenStore passwordResetTokenStore;

    /** 회원가입 시 초기 회원 상태 (knowledge.md: users.status 기본값) */
    private static final String USER_STATUS_ACTIVE = "ACTIVE";

    /** user_profile.nickname VARCHAR(50) — 초과 시 DB 오류(500) 대신 400 으로 처리 */
    private static final int NICKNAME_MAX_LENGTH = 50;

    /** 아이디 찾기 응답 가입일 포맷 (docs: createdAt "2026-07-24") */
    private static final DateTimeFormatter CREATED_AT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 비밀번호 정책 (docs: WEAK_PASSWORD → "비밀번호는 영문, 숫자, 특수문자를 포함하여 8자 이상이어야 합니다.")
     * - 영문/숫자/특수문자를 각각 1개 이상 포함하고 전체 길이 8자 이상 (순서 무관)
     */
    private static final Pattern PASSWORD_POLICY_PATTERN = Pattern.compile(
            "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{8,}$");

    /**
     * PIN 실패 최대 허용 횟수 (docs: PIN_LOCK_EXCEEDED → "핀번호 입력 횟수가 5회 초과")
     * - 실패 횟수가 이 값 이상이 되면 PIN 로그인 영구 잠금 (Redis auth:fail:{userId})
     * - 잠금 해제: PIN 로그인 성공 시 초기화 또는 PASS 본인인증 후 PIN 재설정(별도 API) — 자동 해제 없음
     */
    private static final int MAX_PIN_FAIL_COUNT = 5;

    /**
     * PIN 형식 정책 (docs: INVALID_PIN_FORMAT → "핀번호는 6자리 숫자") — 정확히 6자리 숫자만 허용
     * - 5자리/7자리/문자 포함 → 형식 오류 (400)
     */
    private static final Pattern PIN_FORMAT_PATTERN = Pattern.compile("^\\d{6}$");

    /**
     * device_id / device_name 최대 길이 (ERD: VARCHAR(100))
     * - DB 컬럼 길이 초과로 인한 500 오류 방지 — Service Layer 에서 사전 검증 (docs)
     */
    private static final int DEVICE_MAX_LENGTH = 100;

    /** OAuth2 관례 토큰 인증 방식 (token_info.grant_type) */
    private static final String GRANT_TYPE_BEARER = "Bearer";

    @Override
    @Transactional(readOnly = true)
    public TermsListResponseDTO getTermsList() {

        List<TermsResponseDTO> terms = authMapper.selectTermsList()
                .stream()
                .map(TermsResponseDTO::from)
                .collect(Collectors.toList());

        return TermsListResponseDTO.of(terms);
    }

    @Override
    @Transactional(readOnly = true)
    public EmailAvailabilityResponseDTO checkEmailAvailability(String email) {

        // 1. 이메일 검증 + 정규화 (null/blank → trim → 최대 길이 → lowercase → 형식)
        //    - 실패 시 INVALID_EMAIL_FORMAT (docs: 400) — EmailValidator 공통 정책 (signup 과 동일)
        //    - 이메일 원문 로그 출력 금지 — 로그에 이메일 값 미포함
        String normalizedEmail = normalizeAndValidateEmail(email);

        // 2. 검색용 SHA-256 hash 생성 후 users.email_hash 기준 중복 조회
        //    - email_encrypt(AES 원문) 복호화 금지, 원문 검색 금지 (knowledge.md: 검색용 hash 저장)
        //    - 소문자 정규화 후 hash — email_hash 기준 UNIQUE 제약과 중복 체크가 대소문자에 무관하게 동작하도록
        //      회원가입 완료 시에도 동일하게 소문자 정규화 후 hash 해야 한다
        String emailHash = sha256Hex(normalizedEmail);
        boolean available = authMapper.countByEmailHash(emailHash) == 0;

        // 3. 중복 여부 반환 (중복이어도 성공 응답, 판단은 프론트 가입 흐름에서 처리)
        return EmailAvailabilityResponseDTO.of(available);
    }

    @Override
    // DB 는 SELECT 만 수행하지만 Redis 임시 데이터 저장(side-effect)이 있으므로
    // readOnly=true 로 오인되지 않도록 일반 @Transactional 을 사용한다
    @Transactional
    public IdentityVerificationResponseDTO verifyIdentity(String identityVerificationId) {

        // 0. 요청 값 검증 (javax.validation 미사용 환경 → Service Layer 에서 수행)
        //    null/빈 값은 Provider 에서도 검증하지만, 가입 가능 여부 판단 전에 명시적으로 처리한다
        if (identityVerificationId == null || identityVerificationId.trim().isEmpty()) {
            throw new BusinessException(AuthErrorCode.INVALID_VERIFICATION_ID);
        }

        // 1. Provider 로 PASS 인증 결과 검증 (현재 Mock — 실패 시 BusinessException)
        IdentityVerificationResult result = identityVerificationProvider.verify(identityVerificationId);

        // 2. CI 중복 가입 검증
        //    - CI 원문을 그대로 비교하지 않고 SHA-256 해시로 변환해 조회 (knowledge.md: 검색용 hash 저장)
        //    - 이미 가입된 회원이면 회원가입 진행 불가 (docs: 409 DUPLICATE_USER)
        String ciHash = sha256Hex(result.getCi());
        if (authMapper.countByCiHash(ciHash) > 0) {
            throw new BusinessException(AuthErrorCode.DUPLICATE_USER);
        }

        // 3. 회원가입 임시 데이터 생성 + Redis 임시 저장
        //    - JWT Payload 에 개인정보를 담지 않는 대신, 회원가입 완료 시 복원할 데이터를
        //      signup:verification:{temporaryUserKey} 키로 짧은 TTL 동안 보관한다
        //    - CI/name/phone 은 원문 대신 AES-256 암호화본만 저장 (knowledge.md: Redis 회원 정보 원문 저장 금지)
        String temporaryUserKey = UUID.randomUUID().toString();
        SignupVerificationData verificationData = SignupVerificationData.builder()
                .verificationId(identityVerificationId)
                .ciHash(ciHash)
                .encryptedCi(PersonalDataCipher.encrypt(result.getCi()))
                .encryptedName(PersonalDataCipher.encrypt(result.getName()))
                .encryptedPhone(PersonalDataCipher.encrypt(result.getPhoneNumber()))
                .build();
        signupVerificationStore.save(temporaryUserKey, verificationData);

        // 4. 회원가입 전용 임시 JWT 발급 (Payload: sub, temporaryUserKey, iat, exp — 개인정보 없음)
        String identityToken = signupTokenProvider.issue(temporaryUserKey);

        // 5. 응답 생성 — API Contract 유지 (identityToken, name)
        return IdentityVerificationResponseDTO.of(identityToken, result.getName());
    }

    @Override
    @Transactional
    public void signup(SignupRequestDTO request) {

        // 1. 요청 값 검증 (javax.validation 미사용 환경 → Service Layer 에서 수행)
        validateSignupRequest(request);

        // 2. 회원가입 전용 JWT 검증
        //    - 서명/만료 검증 + sub == signup-verification 확인 (SignupTokenProvider.verifySignupToken)
        //    - 만료: EXPIRED_SIGNUP_TOKEN(401), 위변조/용도 오류: INVALID_SIGNUP_TOKEN(400)
        //    - JWT Payload 에서 temporaryUserKey 추출 (Payload 에 개인정보 없음)
        Claims claims = verifySignupToken(request.getIdentityToken());
        String temporaryUserKey = claims.get("temporaryUserKey", String.class);
        if (temporaryUserKey == null || temporaryUserKey.trim().isEmpty()) {
            throw new BusinessException(AuthErrorCode.INVALID_SIGNUP_TOKEN);
        }

        // 3. Redis 임시 인증 데이터 조회 (없으면 인증 만료로 간주 — 다시 본인인증 필요)
        //    - 회원가입 완료 전까지는 TTL(기본 10분) 내 데이터가 존재해야 한다
        SignupVerificationData verificationData = signupVerificationStore.find(temporaryUserKey);
        if (verificationData == null) {
            throw new BusinessException(AuthErrorCode.SIGNUP_VERIFICATION_NOT_FOUND);
        }

        // 4. CI 중복 재검증 (Race Condition 방지)
        //    - 본인인증 완료 ~ 최종 가입 완료 사이 시간차 동안 동일 CI 로 가입될 수 있으므로 완료 시점에 다시 검증
        //    - user_auth.identity_ci_hash (UNIQUE) 기준 조회
        if (authMapper.countByCiHash(verificationData.getCiHash()) > 0) {
            throw new BusinessException(AuthErrorCode.DUPLICATE_USER);
        }

        // 5. 이메일 처리
        //    - 검증(blank/길이/형식) → 소문자 정규화 → SHA-256 hash 생성 (EmailValidator 공통 정책)
        //    - users.email_hash 기준 중복 재검증 (이메일 원문 DB 조회 금지)
        String normalizedEmail = normalizeAndValidateEmail(request.getEmail());
        String emailHash = sha256Hex(normalizedEmail);
        if (authMapper.countByEmailHash(emailHash) > 0) {
            throw new BusinessException(AuthErrorCode.DUPLICATE_EMAIL);
        }

        // 6. 닉네임 검증 + 중복 검증 (user_profile.nickname UNIQUE, VARCHAR(50))
        String nickname = request.getNickname().trim();
        if (nickname.length() > NICKNAME_MAX_LENGTH) {
            throw new BusinessException(AuthErrorCode.INVALID_SIGNUP_REQUEST);
        }
        if (authMapper.countByNickname(nickname) > 0) {
            throw new BusinessException(AuthErrorCode.DUPLICATE_NICKNAME);
        }

        // 7. 약관 동의 검증 (DB terms 마스터 기준)
        //    - agreedTermsIds 가 null/빈 배열이면 필수 약관 동의 자체가 없으므로 실패
        //    - DB 의 필수 약관(required = 1) ID 가 모두 포함되어야 가입 가능
        //    - 선택 약관은 포함하지 않아도 가입 가능
        //    - 중복 ID 는 제거해 저장 (user_terms_agreements 에 중복 행 방지)
        List<Long> agreedTermsIds = request.getAgreedTermsIds();
        if (agreedTermsIds == null || agreedTermsIds.isEmpty()) {
            throw new BusinessException(AuthErrorCode.MISSING_REQUIRED_TERMS);
        }
        List<Long> distinctAgreedTermIds = new ArrayList<>(new LinkedHashSet<>(agreedTermsIds));

        // 7-1. 존재하지 않는 약관 ID 검증
        //    - terms 마스터에 없는 ID 가 섞여 있으면 user_terms_agreements FK 위반으로
        //      500 이 발생하므로 insert 전에 INVALID_TERM_ID(400) 로 사전 차단한다
        //    - null 요소가 포함된 경우에도 IN 쿼리가 매칭되지 않아 크기 비교로 걸러진다
        List<Long> existingTermIds = authMapper.selectExistingTermIds(distinctAgreedTermIds);
        if (existingTermIds == null || existingTermIds.size() != distinctAgreedTermIds.size()) {
            throw new BusinessException(AuthErrorCode.INVALID_TERM_ID);
        }

        // 7-2. 필수 약관 누락 검증
        List<Long> requiredTermIds = authMapper.selectRequiredTermsIds();
        if (requiredTermIds == null || !distinctAgreedTermIds.containsAll(requiredTermIds)) {
            throw new BusinessException(AuthErrorCode.MISSING_REQUIRED_TERMS);
        }

        // 8. 저장 데이터 준비 (Service Layer 에서만 암호화/해시 수행 — Controller/Mapper 금지)
        //    - password: BCrypt 단방향 해시 (원문 저장/AES 사용 금지)
        //    - email/name/phone: AES-256 양방향 암호화 + 검색용 SHA-256 hash
        //    - CI: AES-256 암호화 + SHA-256 hash (Redis 에서 복원)
        String passwordHash = PasswordEncryptor.encode(request.getPassword());

        // 9. 회원 정보 DB 저장 (users → user_auth → user_profile → user_terms_agreements) + 전자지갑 생성
        //    - 사전 중복 체크(SELECT)와 실제 insert 사이의 Race Condition 은
        //      DB UNIQUE 제약(email_hash, identity_ci_hash, nickname)이 최종 방어선이 된다.
        //    - SELECT 체크를 통과했지만 동시 요청에 의해 UNIQUE 위반이 발생하면
        //      DuplicateKeyException → 409 로 변환해 깔끔한 응답을 반환한다.
        try {
            insertUserWithAuthAndProfile(verificationData, normalizedEmail, emailHash, passwordHash,
                    nickname, distinctAgreedTermIds);
        } catch (DuplicateKeyException e) {
            throw mapDuplicateKeyException(e);
        }

        // 10. 회원가입 완료 후 Redis 임시 데이터 삭제 (1회성 — 재사용 방지)
        //    - Redis 는 DB 트랜잭션의 일부가 아니므로, DB 커밋이 확정된 후(afterCommit)에만 삭제한다.
        //    - 트랜잭션 롤백 시 Redis 데이터가 그대로 남아 사용자가 동일 인증으로 재시도할 수 있다.
        deleteVerificationDataAfterCommit(temporaryUserKey);
    }

    /**
     * users → user_auth → user_profile → user_terms_agreements insert + 전자지갑 생성 (동일 트랜잭션)
     * - 회원가입 전체 과정이 하나의 트랜잭션 — 하나라도 실패하면 전부 롤백된다
     */
    private void insertUserWithAuthAndProfile(SignupVerificationData verificationData,
                                              String normalizedEmail,
                                              String emailHash,
                                              String passwordHash,
                                              String nickname,
                                              List<Long> agreedTermIds) {
        // users insert (회원 기본 정보)
        // - phone 은 Redis 의 AES 암호화본을 그대로 사용 (원문 재암호화 불필요)
        // - phone_hash 는 복호화 후 SHA-256 계산 — users.phone_number_hash (UNIQUE)
        String phoneNumber = PersonalDataCipher.decrypt(verificationData.getEncryptedPhone());
        UserVO user = new UserVO();
        user.setEmailHash(emailHash);
        user.setEmailEncrypt(PersonalDataCipher.encrypt(normalizedEmail));
        user.setNameEncrypt(verificationData.getEncryptedName());
        user.setPhoneNumberHash(sha256Hex(phoneNumber));
        user.setPhoneNumberEncrypt(verificationData.getEncryptedPhone());
        user.setStatus(USER_STATUS_ACTIVE);
        authMapper.insertUser(user);

        // user_auth insert (인증 정보 — 비밀번호/CI)
        UserAuthVO userAuth = new UserAuthVO();
        userAuth.setUserId(user.getId());
        userAuth.setPasswordHash(passwordHash);
        userAuth.setIdentityCiHash(verificationData.getCiHash());
        userAuth.setIdentityCiEncrypt(verificationData.getEncryptedCi());
        authMapper.insertUserAuth(userAuth);

        // user_profile insert (닉네임)
        UserProfileVO userProfile = new UserProfileVO();
        userProfile.setUserId(user.getId());
        userProfile.setNickname(nickname);
        authMapper.insertUserProfile(userProfile);

        // user_terms_agreements insert (약관 동의 저장 — 동의한 약관 ID 목록 전체)
        authMapper.insertUserTerms(user.getId(), agreedTermIds);

        // 전자지갑 생성 (knowledge.md Signup Flow: 8. Create wallet)
        // 같은 트랜잭션 내에서 생성 — 지갑 생성 실패 시 DB insert 전체가 롤백된다
        walletService.createWallet(user.getId());
    }

    /**
     * DB UNIQUE 제약 위반(DuplicateKeyException)을 도메인 에러 코드로 매핑한다.
     * - 사용자 간 Race Condition 으로 인한 UNIQUE 충돌 시 500 대신 명확한 409 를 반환하기 위함
     */
    private BusinessException mapDuplicateKeyException(DuplicateKeyException e) {
        String message = String.valueOf(e.getMessage());
        if (message.contains("ux_users_email") || message.contains("users.email_hash")) {
            return new BusinessException(AuthErrorCode.DUPLICATE_EMAIL);
        }
        if (message.contains("ux_users_pass_ci") || message.contains("user_auth.identity_ci_hash")) {
            return new BusinessException(AuthErrorCode.DUPLICATE_USER);
        }
        if (message.contains("ux_user_profile_nickname") || message.contains("user_profile.nickname")) {
            return new BusinessException(AuthErrorCode.DUPLICATE_NICKNAME);
        }
        if (message.contains("ux_users_phone") || message.contains("users.phone_number_hash")) {
            // 동일 휴대폰으로 이미 가입된 회원 — 1인 1계정 정책상 CI 중복과 동일하게 처리
            return new BusinessException(AuthErrorCode.DUPLICATE_USER);
        }
        // 식별되지 않은 UNIQUE 충돌 — 응답에 제약조건명/테이블명 노출 금지 (knowledge.md)
        return new BusinessException(AuthErrorCode.DUPLICATE_USER);
    }

    /**
     * DB 트랜잭션이 커밋된 후(afterCommit)에만 Redis 임시 데이터를 삭제한다.
     * - 트랜잭션이 진행 중일 때 Redis 를 지우면 롤백 시 사용자가 재시도할 수 없게 된다.
     * - 실제 트랜잭션 밖(테스트 등)에서는 즉시 삭제한다.
     */
    private void deleteVerificationDataAfterCommit(String temporaryUserKey) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    signupVerificationStore.delete(temporaryUserKey);
                }
            });
        } else {
            signupVerificationStore.delete(temporaryUserKey);
        }
    }

    @Override
    // DB 는 SELECT 만 수행하고 Redis 저장(side-effect)은 DB 트랜잭션과 무관하게 즉시 반영하므로
    // 별도 @Transactional 을 사용하지 않는다 (Redis 는 DB 트랜잭션에 참여하지 않음)
    public LoginResponseDTO login(LoginRequestDTO request) {

        // 1. 요청 값 검증 — 필수 값 누락은 INVALID_LOGIN_REQUEST(400), 잘못된 방식은 INVALID_LOGIN_TYPE(400)
        LoginType loginType = validateLoginRequest(request);

        // 2. 방식별 인증 — 성공하면 인증 완료된 회원 정보 반환
        //    - PASSWORD: email/phone SHA-256 hash 조회 + password BCrypt 검증
        //    - PIN     : deviceId 조회 + 잠금 확인 + pin BCrypt 검증
        LoginUserVO loginUser = (loginType == LoginType.PASSWORD)
                ? loginByPassword(request)
                : loginByPin(request);

        // 3. 회원 상태 확인 — ACTIVE 만 로그인 허용
        //    - WITHDRAWN/BLOCKED 등은 실패 원인을 노출하지 않고 INVALID_CREDENTIALS(401) 처리
        //      (계정 상태가 외부에 노출되지 않도록 통일)
        if (!USER_STATUS_ACTIVE.equals(loginUser.getStatus())) {
            throw new BusinessException(AuthErrorCode.INVALID_CREDENTIALS);
        }

        // 4. JWT 발급 (기존 JwtTokenProvider 재사용)
        //    - Payload: sub(userId), role, tokenType, iat, exp — 개인정보 없음
        String accessToken = jwtTokenProvider.createAccessToken(loginUser.getId());
        String refreshToken = jwtTokenProvider.createRefreshToken(loginUser.getId());

        // 5. Refresh Token Redis 저장 (knowledge.md Refresh Token Security)
        //    - 원문이 아닌 SHA-256 hash 저장 — key: refresh:token:{userId}, TTL: refresh 만료와 동일
        long refreshTtlSeconds = jwtTokenProvider.getRefreshTokenExpirationSeconds();
        refreshTokenStore.save(loginUser.getId(), sha256Hex(refreshToken), refreshTtlSeconds);

        // 6. 응답 생성 — name 은 Service Layer 에서만 복호화 (Controller/Mapper 금지)
        //    - refreshToken 은 JSON 본문에 포함하지 않고 Controller 가 HttpOnly Cookie 로만 내려준다
        return LoginResponseDTO.builder()
                .userId(loginUser.getId())
                .name(PersonalDataCipher.decrypt(loginUser.getNameEncrypt()))
                .tokenInfo(LoginResponseDTO.TokenInfo.of(
                        GRANT_TYPE_BEARER,
                        accessToken,
                        jwtTokenProvider.getAccessTokenExpirationSeconds()))
                .refreshToken(refreshToken)
                .refreshTokenMaxAgeSeconds(refreshTtlSeconds)
                .build();
    }

    @Override
    // DB 는 SELECT 만 수행하고 Redis 조회/저장(side-effect)은 DB 트랜잭션과 무관하게 즉시 반영하므로
    // 별도 @Transactional 을 사용하지 않는다 (login 과 동일 — Redis 는 DB 트랜잭션에 참여하지 않음)
    public RefreshTokenResponseDTO refreshAccessToken(String refreshToken) {

        // 1. 요청 값 검증 — 쿠키 누락/빈 값 → INVALID_REFRESH_TOKEN(401)
        //    (docs: 재발급 실패는 원인 구분 없이 INVALID_REFRESH_TOKEN 으로 통일)
        if (isBlank(refreshToken)) {
            throw new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 2. Refresh Token 검증 — 서명/만료 + tokenType == REFRESH 확인
        //    - 만료(ExpiredJwtException)와 위변조/형식 오류/Access Token 오용(JwtException) 모두
        //      INVALID_REFRESH_TOKEN(401) 로 통일 (docs — ExpiredJwtException 은 JwtException 의 하위 타입)
        //    - Refresh Token 원문은 로그에 출력하지 않는다 (JWT 로그 유출 방지)
        Claims claims;
        try {
            claims = jwtTokenProvider.parseRefreshToken(refreshToken);
        } catch (JwtException e) {
            throw new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 3. sub(userId) 추출 — 이미 검증된 claims 에서 직접 추출 (중복 파싱 방지)
        Long userId = extractUserIdFromRefreshClaims(claims);

        // 4. 회원 상태 확인 — ACTIVE 만 재발급 허용 (login 과 동일 정책)
        //    - 탈퇴/차단/미존재 회원은 실패 원인을 노출하지 않고 INVALID_REFRESH_TOKEN(401) 처리
        LoginUserVO loginUser = authMapper.findUserById(userId);
        if (loginUser == null || !USER_STATUS_ACTIVE.equals(loginUser.getStatus())) {
            throw new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 5. Redis hash 비교 — Refresh Token Rotation (knowledge.md Refresh Token Security)
        //    - 저장 hash 없음(로그아웃/TTL 만료) → 재발급 불가
        //    - 불일치(클라이언트 토큰 != 저장 토큰) → 토큰 재사용 감지 → 세션 전체 revoke
        //    - 알려진 한계: find→비교→save/delete 는 check-then-act 라 동일 토큰의 동시 요청은
        //      둘 다 통과할 수 있다(마지막 save 가 승리). 기존 단순 RedisTemplate 스타일에 맞춰
        //      원자적 처리(Lua 등)는 적용하지 않는다 — 필요 시 별도 검토
        String storedHash = refreshTokenStore.find(userId);
        String presentedHash = sha256Hex(refreshToken);
        if (storedHash == null || !storedHash.equals(presentedHash)) {
            if (storedHash != null) {
                refreshTokenStore.delete(userId);
            }
            throw new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 6. 신규 토큰 발급 (Rotation) + Redis hash 교체
        //    - Access Token: 짧은 만료(기본 15분) — 무중단 서비스용 재발급
        //    - Refresh Token: 새로 발급해 기존 hash 를 교체 (재사용 불가)
        String accessToken = jwtTokenProvider.createAccessToken(userId);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(userId);
        long refreshTtlSeconds = jwtTokenProvider.getRefreshTokenExpirationSeconds();
        refreshTokenStore.save(userId, sha256Hex(newRefreshToken), refreshTtlSeconds);

        // 7. Audit 로그 (knowledge.md Audit Log Policy: Refresh Token 재발급 기록 대상)
        //    - userId 는 민감정보가 아니며, JWT/개인정보 원문은 로그에 포함하지 않는다
        log.info("Refresh Token 재발급 성공 - userId={}", userId);

        // 8. 응답 생성 — token_info 는 로그인과 동일 구조, refreshToken 은 쿠키 전용
        return RefreshTokenResponseDTO.builder()
                .tokenInfo(LoginResponseDTO.TokenInfo.of(
                        GRANT_TYPE_BEARER,
                        accessToken,
                        jwtTokenProvider.getAccessTokenExpirationSeconds()))
                .refreshToken(newRefreshToken)
                .refreshTokenMaxAgeSeconds(refreshTtlSeconds)
                .build();
    }

    @Override
    // Redis 삭제(side-effect)만 수행하므로 별도 @Transactional 을 사용하지 않는다
    // (login/refreshAccessToken 과 동일 — Redis 는 DB 트랜잭션에 참여하지 않음)
    public void logout(String refreshToken) {

        // 1. 요청 값 검증 — 쿠키 누락/빈 값 → INVALID_REFRESH_TOKEN(401)
        //    (refreshAccessToken 과 동일 정책 — 실패 원인 구분 노출 금지)
        if (isBlank(refreshToken)) {
            throw new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 2. Refresh Token 검증 — 서명/만료 + tokenType == REFRESH 확인
        //    - 만료(ExpiredJwtException)와 위변조/형식 오류/Access Token 오용(JwtException) 모두
        //      INVALID_REFRESH_TOKEN(401) 로 통일 (refreshAccessToken 과 동일)
        //    - Refresh Token 원문은 로그에 출력하지 않는다 (JWT 로그 유출 방지)
        Claims claims;
        try {
            claims = jwtTokenProvider.parseRefreshToken(refreshToken);
        } catch (JwtException e) {
            throw new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 3. sub(userId) 추출 — 검증된 claims 에서 직접 추출 (중복 파싱 방지)
        Long userId = extractUserIdFromRefreshClaims(claims);

        // 4. Redis hash 비교 — 로그아웃 대상 세션 확인
        //    - 저장 hash 없음(이미 로그아웃/TTL 만료) → 삭제할 세션이 없으므로 실패
        //    - 불일치(클라이언트 토큰 != 저장 토큰) → 위변조/재사용 의심 → 세션 revoke 후 실패
        //      (refreshAccessToken 의 Rotation 재사용 감지와 동일 정책 — 탈취 토큰의 세션 폐기)
        String storedHash = refreshTokenStore.find(userId);
        String presentedHash = sha256Hex(refreshToken);
        if (storedHash == null) {
            throw new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }
        if (!storedHash.equals(presentedHash)) {
            refreshTokenStore.delete(userId);
            throw new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 5. Refresh Token 삭제 — 세션 무효화 (이후 재발급 불가)
        refreshTokenStore.delete(userId);

        // 6. Audit 로그 (knowledge.md Audit Log Policy 스타일 유지)
        //    - userId 는 민감정보가 아니며, JWT/개인정보 원문은 로그에 포함하지 않는다
        log.info("로그아웃 성공 - userId={}", userId);
    }

    @Override
    // SELECT 만 수행하므로 읽기 전용 트랜잭션 (checkEmailAvailability 와 동일)
    @Transactional(readOnly = true)
    public FindIdResponseDTO findId(String identityVerificationId) {

        // 1. 요청 값 검증 — null/빈 값 → INVALID_VERIFICATION_ID(400)
        //    (verifyIdentity 와 동일 — javax.validation 미사용 환경, Service Layer 에서 수행)
        if (identityVerificationId == null || identityVerificationId.trim().isEmpty()) {
            throw new BusinessException(AuthErrorCode.INVALID_VERIFICATION_ID);
        }

        // 2. PASS 본인인증 결과 검증 → CI 추출
        //    - 인증 실패 시 Provider 가 BusinessException(INVALID_VERIFICATION_ID) 을 던진다
        //    - CI 는 개인식별값 — 원문 로그 출력 금지 (knowledge.md)
        IdentityVerificationResult result = identityVerificationProvider.verify(identityVerificationId);

        // 3. CI SHA-256 hash 변환 → 가입 회원 조회 (user_auth.identity_ci_hash UNIQUE)
        //    - CI 원문이 아닌 hash 로만 조회한다 (knowledge.md: 검색용 hash 저장)
        //    - 가입된 회원이 없으면 404 (docs: USER_NOT_FOUND)
        String ciHash = sha256Hex(result.getCi());
        UserVO user = authMapper.selectUserByCiHash(ciHash);
        if (user == null) {
            throw new BusinessException(AuthErrorCode.USER_NOT_FOUND);
        }

        // 3-1. 회원 상태 확인 — ACTIVE 만 아이디 찾기 허용
        //    - 탈퇴(WITHDRAWN)/차단(BLOCKED) 등 비활성 회원은 계정 존재 여부를 노출하지 않고
        //      USER_NOT_FOUND(404) 로 처리 (login/refreshAccessToken 과 동일 정책 — knowledge.md)
        if (!USER_STATUS_ACTIVE.equals(user.getStatus())) {
            throw new BusinessException(AuthErrorCode.USER_NOT_FOUND);
        }

        // 4. 이메일 복호화 → 마스킹 (Service Layer 에서만 복호화 — Controller/Mapper 금지)
        //    - 원문 이메일은 응답에 포함하지 않고 마스킹본만 반환 (docs: 개인정보 보호)
        String maskedEmail = EmailMasker.mask(PersonalDataCipher.decrypt(user.getEmailEncrypt()));

        // 5. 가입일 yyyy-MM-dd 포맷 (docs 응답 예시: "2026-07-24")
        //    - created_at 은 NOT NULL(DEFAULT CURRENT_TIMESTAMP) 이므로 직접 포맷
        String createdAt = user.getCreatedAt().format(CREATED_AT_FORMATTER);

        // 6. Audit 로그 — userId 만 기록 (이메일 원문/마스킹본 로그 출력 금지)
        log.info("아이디 찾기 성공 - userId={}", user.getId());

        return FindIdResponseDTO.of(maskedEmail, createdAt);
    }

    @Override
    // DB 는 SELECT 만 수행하고 Redis 저장(side-effect)은 DB 트랜잭션과 무관하게 즉시 반영하므로
    // 별도 @Transactional 을 사용하지 않는다 (login 과 동일 — Redis 는 DB 트랜잭션에 참여하지 않음)
    public PasswordVerifyResponseDTO verifyPasswordReset(PasswordVerifyRequestDTO request) {

        // 1. 요청 값 검증 — loginId/identityVerificationId 누락 → INVALID_PASSWORD_RESET_REQUEST(400)
        //    (javax.validation 미사용 환경 → Service Layer 에서 수행 — findId 와 동일)
        if (request == null
                || isBlank(request.getLoginId())
                || isBlank(request.getIdentityVerificationId())) {
            throw new BusinessException(AuthErrorCode.INVALID_PASSWORD_RESET_REQUEST);
        }

        // 2. loginId(이메일 또는 휴대폰) 기준 회원 조회 — loginByPassword 와 동일한 판별 규칙 재사용
        //    - 개인정보 원문(email_encrypt/phone_encrypt)은 절대 조회하지 않는다 (knowledge.md: 검색용 hash)
        //    - 회원 없음 → USER_NOT_FOUND(404) (docs)
        LoginUserVO user = findUserByLoginId(request.getLoginId());
        if (user == null) {
            throw new BusinessException(AuthErrorCode.USER_NOT_FOUND);
        }

        // 3. PASS 본인인증 결과 검증 → CI 추출
        //    - 인증 실패 시 Provider 가 BusinessException(INVALID_VERIFICATION_ID) 을 던진다
        //    - CI 는 개인식별값 — 원문 로그 출력 금지 (knowledge.md)
        IdentityVerificationResult result =
                identityVerificationProvider.verify(request.getIdentityVerificationId());

        // 4. CI SHA-256 hash 대조 — 입력한 계정과 본인인증(PASS) 정보가 일치해야 한다
        //    - CI 원문이 아닌 hash 로만 비교 (knowledge.md: 검색용 hash 저장)
        //    - 불일치 → VERIFICATION_FAILED(400) (docs — 원인 비노출)
        String ciHash = sha256Hex(result.getCi());
        if (!ciHash.equals(user.getIdentityCiHash())) {
            throw new BusinessException(AuthErrorCode.VERIFICATION_FAILED);
        }

        // 5. 사용자 상태 확인 — ACTIVE 만 비밀번호 재설정 허용
        //    - 탈퇴(WITHDRAWN)/차단(BLOCKED) 등 비활성 회원은 계정 존재 여부를 노출하지 않고
        //      USER_NOT_FOUND(404) 로 처리 (findId/refreshAccessToken 과 동일 정책)
        if (!USER_STATUS_ACTIVE.equals(user.getStatus())) {
            throw new BusinessException(AuthErrorCode.USER_NOT_FOUND);
        }

        // 6. passwordResetToken 발급 + Redis 5분 TTL 저장
        //    - UUID 는 예측 불가능한 1회성 토큰 — key: password:reset:{token}, value: userId
        //    - TTL 은 저장소가 설정값(기본 5분)을 내부 적용한다 — Service 에서 하드코딩/전달하지 않는다
        //      (RedisSignupVerificationStore 패턴과 동일)
        String passwordResetToken = UUID.randomUUID().toString();
        passwordResetTokenStore.save(passwordResetToken, user.getId());

        // 7. Audit 로그 — userId 만 기록 (토큰/개인정보 원문 로그 출력 금지)
        log.info("비밀번호 재설정 토큰 발급 - userId={}", user.getId());

        return PasswordVerifyResponseDTO.of(passwordResetToken);
    }

    @Override
    @Transactional
    // 비밀번호 변경은 user_auth UPDATE(DB 쓰기) + Redis 토큰 삭제의 조합이므로
    // signup 과 동일하게 트랜잭션 경계를 Service 에 두고, Redis 삭제는 DB 커밋 확정 후(afterCommit) 수행한다
    public void resetPassword(PasswordResetRequestDTO request) {

        // 1. 요청 값 검증 — passwordResetToken 누락 → RESET_TIMEOUT_OR_INVALID_TOKEN(400)
        //    (docs: 만료/존재하지 않음/잘못된 접근을 하나의 에러 코드로 통일)
        if (request == null || isBlank(request.getPasswordResetToken())) {
            throw new BusinessException(AuthErrorCode.RESET_TIMEOUT_OR_INVALID_TOKEN);
        }

        // 2. Redis(password:reset:{token}) 검증 — 저장된 userId 조회
        //    - 없으면 TTL(5분) 만료 또는 사용 완료/위조 토큰 → RESET_TIMEOUT_OR_INVALID_TOKEN(400)
        //    - 실패 원인을 구분해 노출하지 않아 토큰 유효성/탈취 여부를 숨긴다
        Long userId = passwordResetTokenStore.find(request.getPasswordResetToken());
        if (userId == null) {
            throw new BusinessException(AuthErrorCode.RESET_TIMEOUT_OR_INVALID_TOKEN);
        }

        // 3. 비밀번호 정책 검증 — 영문/숫자/특수문자 포함 8자 이상 (docs: WEAK_PASSWORD 422)
        //    - newPassword 누락/빈 값도 정책 미달로 간주 (프론트 1차 검증 이전 서버 차단)
        validatePasswordPolicy(request.getNewPassword());

        // 4. BCrypt 암호화 — 원문 저장/복호화 금지 (knowledge.md: 비밀번호는 BCrypt 단방향 해시)
        String newPasswordHash = PasswordEncryptor.encode(request.getNewPassword());

        // 5. user_auth.password_hash 갱신
        //    - 갱신 행 수가 0 이면 해당 userId 의 인증 정보가 없다(회원 탈퇴 등) → 재설정 흐름 무효 처리
        int updated = authMapper.updatePasswordHash(userId, newPasswordHash);
        if (updated == 0) {
            // 일회성 토큰 정책 — 갱신 대상이 없으면(회원 탈퇴 등) 재시도를 막기 위해 토큰도 즉시 폐기한다.
            // DB 갱신이 발생하지 않은 상태이므로 afterCommit 없이 바로 삭제한다.
            passwordResetTokenStore.delete(request.getPasswordResetToken());
            throw new BusinessException(AuthErrorCode.RESET_TIMEOUT_OR_INVALID_TOKEN);
        }

        // 6. 사용 완료 후 Redis 토큰 삭제 — 1회성 (DB 커밋 확정 후 삭제 — signup 의 Redis 정리 패턴과 동일)
        deletePasswordResetTokenAfterCommit(request.getPasswordResetToken());

        // 7. Audit 로그 (knowledge.md Audit Log Policy: 비밀번호 변경 기록 대상)
        //    - userId 는 민감정보가 아니며, 비밀번호/토큰 원문은 로그에 포함하지 않는다
        log.info("비밀번호 재설정 성공 - userId={}", userId);
    }

    @Override
    @Transactional
    // user_device INSERT(DB 쓰기) 하나의 작업이므로 단순 트랜잭션 경계를 Service 에 둔다
    // (signup 과 동일 — 검증/암호화는 전부 Service Layer 에서 수행)
    public void setupPin(Long userId, PinSetupRequestDTO request) {

        // 1. 요청 값 검증 (javax.validation 미사용 환경 → Service Layer 에서 수행)
        //    - pinNumber/deviceId/deviceName 누락·빈 값 → INVALID_PIN_SETUP_REQUEST(400)
        validatePinSetupRequest(request);

        // 2. 회원 존재 + 상태 확인 (JWT 인증된 userId 기준)
        //    - 탈퇴/차단/미존재 회원은 계정 존재 여부를 노출하지 않고 USER_NOT_FOUND(404) 로 처리
        //      (refreshAccessToken 과 동일 정책 — docs: 404 USER_NOT_FOUND)
        LoginUserVO user = authMapper.findUserById(userId);
        if (user == null || !USER_STATUS_ACTIVE.equals(user.getStatus())) {
            throw new BusinessException(AuthErrorCode.USER_NOT_FOUND);
        }

        // 3. 기존 PIN 등록 여부 확인 (user_device: user_id + device_id, UNIQUE)
        //    - 이미 등록된 기기면 재등록 불가 → PIN_ALREADY_EXISTS(409)
        //      (knowledge.md: 409 CONFLICT - 중복 데이터)
        if (authMapper.countByUserIdAndDeviceId(userId, request.getDeviceId()) > 0) {
            throw new BusinessException(AuthErrorCode.PIN_ALREADY_EXISTS);
        }

        // 4. PIN 형식 검증 — 6자리 숫자 (docs: INVALID_PIN_FORMAT 400)
        validatePinFormat(request.getPinNumber());

        // 5. BCrypt 암호화 — PIN 원문 저장/복호화 금지
        //    (knowledge.md: PIN 번호는 BCrypt 단방향 암호화)
        String pinHash = PasswordEncryptor.encode(request.getPinNumber());

        // 6. user_device insert (ERD 컬럼: user_id, device_id, device_name, pin_hash)
        //    - last_login_at 은 최초 설정 시점에는 null (PIN 로그인 성공 시 갱신 예정)
        //    - created_at 은 DB 기본값(CURRENT_TIMESTAMP) 사용
        UserDeviceVO userDevice = new UserDeviceVO();
        userDevice.setUserId(userId);
        userDevice.setDeviceId(request.getDeviceId());
        userDevice.setDeviceName(request.getDeviceName());
        userDevice.setPinHash(pinHash);
        authMapper.insertUserDevice(userDevice);

        // 7. Audit 로그 — userId 만 기록 (PIN 원문/해시 로그 출력 금지 — knowledge.md)
        log.info("PIN 최초 설정 성공 - userId={}", userId);
    }

    @Override
    @Transactional
    // user_device UPDATE(DB 쓰기) 하나의 작업이므로 트랜잭션 경계를 Service 에 둔다
    // (setupPin 과 동일 — 검증/암호화는 전부 Service Layer 에서 수행)
    public void resetPin(Long userId, PinResetRequestDTO request) {

        // 1. 요청 값 검증 (javax.validation 미사용 환경 → Service Layer 에서 수행)
        //    - identityVerificationId 누락·빈 값 → INVALID_VERIFICATION_ID(400)
        //      (findId/verifyIdentity 와 동일 정책)
        //    - pinNumber 누락·빈 값은 형식 검증(validatePinFormat)에서 INVALID_PIN_FORMAT 으로 차단된다
        if (request == null || isBlank(request.getIdentityVerificationId())) {
            throw new BusinessException(AuthErrorCode.INVALID_VERIFICATION_ID);
        }

        // 2. JWT 로그인 사용자 조회 + 상태 확인 (users JOIN user_auth — identity_ci_hash 포함)
        //    - 탈퇴/차단/미존재 회원은 계정 존재 여부를 노출하지 않고 USER_NOT_FOUND(404) 로 처리
        //      (setupPin/refreshAccessToken 과 동일 정책 — docs: 404 USER_NOT_FOUND)
        LoginUserVO user = authMapper.selectUserAuthById(userId);
        if (user == null || !USER_STATUS_ACTIVE.equals(user.getStatus())) {
            throw new BusinessException(AuthErrorCode.USER_NOT_FOUND);
        }

        // 3. PASS 본인인증 결과 검증 → CI 추출
        //    - 인증 실패 시 Provider 가 BusinessException(INVALID_VERIFICATION_ID) 을 던진다
        //    - identityVerificationId/CI 는 개인식별값 — 원문 로그 출력 금지 (knowledge.md)
        IdentityVerificationResult result =
                identityVerificationProvider.verify(request.getIdentityVerificationId());

        // 4. CI SHA-256 hash 대조 — 로그인 사용자와 PASS 인증 사용자가 동일 인물이어야 한다
        //    - CI 원문이 아닌 hash 로만 비교 (knowledge.md: 검색용 hash 저장)
        //    - 불일치 → VERIFICATION_FAILED(400) (docs — 원인 비노출)
        String ciHash = sha256Hex(result.getCi());
        if (!ciHash.equals(user.getIdentityCiHash())) {
            throw new BusinessException(AuthErrorCode.VERIFICATION_FAILED);
        }

        // 5. 신규 PIN 형식 검증 — 6자리 숫자 (docs: INVALID_PIN_FORMAT 400)
        validatePinFormat(request.getPinNumber());

        // 6. BCrypt 암호화 — PIN 원문 저장/복호화 금지 (knowledge.md)
        String pinHash = PasswordEncryptor.encode(request.getPinNumber());

    // 7. user_device.pin_hash 갱신 (user_id 기준 — 등록된 전체 기기에 동일 적용)
    //    - 갱신 행 수가 0 이면 등록된 PIN(기기)이 없는 회원 → 재설정 불가
    int updated = authMapper.updateUserDevicePinHash(userId, pinHash);

        if (updated == 0) {
            throw new BusinessException(AuthErrorCode.PIN_NOT_REGISTERED);
        }

        // 8. PIN 실패 횟수 초기화 — 잠금 해제 (knowledge.md PIN Policy)
        //    - "잠금 해제: PIN 로그인 성공 시 초기화 또는 PASS 본인인증 후 PIN 재설정"
        //    - PASS 재인증으로 본인 확인이 완료된 시점이므로, 실패 횟수 5회로 잠긴 유저도
        //      신규 PIN 으로 다시 로그인할 수 있어야 한다 (resetPin 이 잠금 해제 수단)
        loginFailCounter.reset(userId);

        // 9. Audit 로그 (knowledge.md Audit Log Policy: PIN 변경 기록 대상)
        //    - userId 는 민감정보가 아니며, PIN 원문/해시는 로그에 포함하지 않는다
        log.info("PIN 재설정 성공 - userId={}", userId);
    }

    /**
     * PIN 설정 요청 값 검증 — 필수 값 누락/빈 값/길이 초과 → INVALID_PIN_SETUP_REQUEST(400)
     * - javax.validation 미사용 환경 → Service Layer 에서 수행 (signup/login 과 동일)
     * - pinNumber 가 비어 있으면 형식 검증(6자리) 이전에 차단된다
     * - deviceId/deviceName 은 ERD VARCHAR(100) 초과 시 DB 오류(500) 대신 400 으로 사전 차단
     *   (signup 의 nickname 최대 길이 검증과 동일 패턴)
     */
    private void validatePinSetupRequest(PinSetupRequestDTO request) {
        if (request == null
                || isBlank(request.getPinNumber())
                || isBlank(request.getDeviceId())
                || isBlank(request.getDeviceName())) {
            throw new BusinessException(AuthErrorCode.INVALID_PIN_SETUP_REQUEST);
        }
        // DB 컬럼 길이 초과(VARCHAR(100))로 인한 500 오류 방지 — 저장될 원문(trim 전) 길이 기준
        if (request.getDeviceId().length() > DEVICE_MAX_LENGTH
                || request.getDeviceName().length() > DEVICE_MAX_LENGTH) {
            throw new BusinessException(AuthErrorCode.INVALID_PIN_SETUP_REQUEST);
        }
    }

    /**
     * PIN 형식 검증 — 정확히 6자리 숫자 (docs: INVALID_PIN_FORMAT 400)
     * - 5자리/7자리/문자 포함 → 실패
     * - null/빈 값은 요청 검증(validatePinSetupRequest)에서 이미 차단된다
     */
    private void validatePinFormat(String pinNumber) {
        if (!PIN_FORMAT_PATTERN.matcher(pinNumber).matches()) {
            throw new BusinessException(AuthErrorCode.INVALID_PIN_FORMAT);
        }
    }

    /**
     * loginId(이메일 또는 휴대폰)로 회원을 조회한다.
     * - '@' 포함 → 이메일: trim → lowercase → SHA-256 hash → email_hash 조회
     * - 그 외    → 휴대폰: trim → 하이픈 제거 → SHA-256 hash → phone_number_hash 조회
     * - 개인정보 원문(email_encrypt/phone_encrypt)은 절대 조회하지 않는다 (knowledge.md)
     * - PASSWORD 로그인(loginByPassword)과 비밀번호 재설정(verifyPasswordReset)이 공통 사용한다
     *
     * @return 매칭되는 회원이 없으면 null
     */
    private LoginUserVO findUserByLoginId(String loginId) {
        String normalized = loginId.trim();
        if (normalized.contains("@")) {
            return authMapper.findUserByEmailHash(sha256Hex(normalized.toLowerCase(Locale.ROOT)));
        }
        String phoneNumber = normalized.replace("-", "");
        return authMapper.findUserByPhoneHash(sha256Hex(phoneNumber));
    }

    /**
     * 비밀번호 정책 검증 — 영문/숫자/특수문자를 각각 1개 이상 포함하고 8자 이상이어야 한다.
     * - docs: WEAK_PASSWORD(422) "비밀번호는 영문, 숫자, 특수문자를 포함하여 8자 이상이어야 합니다."
     * - newPassword 누락(null/빈 값)도 정책 미달로 간주
     */
    private void validatePasswordPolicy(String newPassword) {
        if (newPassword == null || !PASSWORD_POLICY_PATTERN.matcher(newPassword).matches()) {
            throw new BusinessException(AuthErrorCode.WEAK_PASSWORD);
        }
    }

    /**
     * DB 트랜잭션이 커밋된 후(afterCommit)에만 Redis 재설정 토큰을 삭제한다.
     * - 트랜잭션이 진행 중일 때 Redis 를 지우면 롤백 시 사용자가 동일 토큰으로 재시도할 수 없게 된다.
     * - 실제 트랜잭션 밖(테스트 등)에서는 즉시 삭제한다 (deleteVerificationDataAfterCommit 과 동일 패턴).
     */
    private void deletePasswordResetTokenAfterCommit(String passwordResetToken) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    passwordResetTokenStore.delete(passwordResetToken);
                }
            });
        } else {
            passwordResetTokenStore.delete(passwordResetToken);
        }
    }

    /**
     * 검증된 Refresh Token claims 에서 userId(sub) 를 추출한다.
     * - JwtTokenProvider.extractUserId 는 토큰을 다시 파싱하므로, 이미 검증된 claims 에서 직접 추출한다
     * - sub 누락/빈 값/비숫자 형식 → INVALID_REFRESH_TOKEN (정상 발급 토큰이 아니므로 위변조로 간주)
     */
    private Long extractUserIdFromRefreshClaims(Claims claims) {
        String subject = claims.getSubject();
        if (isBlank(subject)) {
            throw new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }
        try {
            return Long.valueOf(subject);
        } catch (NumberFormatException e) {
            throw new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }
    }

    /**
     * PASSWORD 로그인 — email/phone SHA-256 hash 조회 + password BCrypt 검증
     * - 회원 조회는 findUserByLoginId 공통 헬퍼를 재사용한다 (비밀번호 재설정과 동일한 판별 규칙)
     * - 원문(email_encrypt/phone_encrypt)은 절대 조회하지 않는다 (knowledge.md)
     *
     * @throws BusinessException INVALID_CREDENTIALS — 회원 없음 또는 password 불일치 (원인 비노출)
     */
    private LoginUserVO loginByPassword(LoginRequestDTO request) {
        LoginUserVO loginUser = findUserByLoginId(request.getLoginId());

        if (loginUser == null) {
            throw new BusinessException(AuthErrorCode.INVALID_CREDENTIALS);
        }
        if (!PasswordEncryptor.matches(request.getPassword(), loginUser.getPasswordHash())) {
            throw new BusinessException(AuthErrorCode.INVALID_CREDENTIALS);
        }
        return loginUser;
    }

    /**
     * PIN 로그인 — deviceId 조회 + 잠금 확인 + pin BCrypt 검증
     *
     * 흐름 (knowledge.md PIN Login Policy):
     *   1. deviceId 조회 → 등록된 기기 없음 → INVALID_CREDENTIALS(401)
     *   2. 잠금 확인 (Redis auth:fail:{userId} 실패 횟수 >= MAX) → PIN_LOCK_EXCEEDED(403)
     *   3. pin BCrypt 검증 → 불일치 시 실패 횟수 증가 후 INVALID_CREDENTIALS(401)
     *   4. 성공 시 실패 횟수 초기화
     *
     * 잠금 해제: PIN 로그인 성공 시 초기화 또는 PASS 본인인증 후 PIN 재설정(별도 API) — 자동 해제 없음
     *
     * @throws BusinessException INVALID_CREDENTIALS / PIN_LOCK_EXCEEDED
     */
    private LoginUserVO loginByPin(LoginRequestDTO request) {
        LoginUserVO loginUser = authMapper.findUserByDeviceId(request.getDeviceId());
        if (loginUser == null) {
            throw new BusinessException(AuthErrorCode.INVALID_CREDENTIALS);
        }

        // 잠금 확인 — 잠금 상태에서는 PIN 검증 없이 즉시 거부 (추가 시도 방지)
        if (loginFailCounter.getCount(loginUser.getId()) >= MAX_PIN_FAIL_COUNT) {
            throw new BusinessException(AuthErrorCode.PIN_LOCK_EXCEEDED);
        }

        // PIN 검증 — 실패 시 영구 카운터 증가 (잠금은 자동 해제되지 않는다)
        if (!PasswordEncryptor.matches(request.getPinNumber(), loginUser.getPinHash())) {
            loginFailCounter.increment(loginUser.getId());
            throw new BusinessException(AuthErrorCode.INVALID_CREDENTIALS);
        }

        // 성공 — 실패 횟수 초기화
        loginFailCounter.reset(loginUser.getId());
        return loginUser;
    }

    /**
     * 로그인 요청 값 검증
     * - loginType 누락 / PASSWORD 필수 값(loginId, password) 누락 / PIN 필수 값(pinNumber, deviceId) 누락
     *   → INVALID_LOGIN_REQUEST(400)
     * - 잘못된 loginType → INVALID_LOGIN_TYPE(400)
     * - password/pinNumber 원문은 로그에 출력하지 않는다 (DTO @ToString.Exclude)
     *
     * @return 검증을 통과한 LoginType (호출부에서 재변환 없이 사용)
     */
    private LoginType validateLoginRequest(LoginRequestDTO request) {
        if (request == null || isBlank(request.getLoginType())) {
            throw new BusinessException(AuthErrorCode.INVALID_LOGIN_REQUEST);
        }
        LoginType loginType = parseLoginType(request.getLoginType());
        if (loginType == LoginType.PASSWORD) {
            if (isBlank(request.getLoginId()) || isBlank(request.getPassword())) {
                throw new BusinessException(AuthErrorCode.INVALID_LOGIN_REQUEST);
            }
        } else {
            if (isBlank(request.getPinNumber()) || isBlank(request.getDeviceId())) {
                throw new BusinessException(AuthErrorCode.INVALID_LOGIN_REQUEST);
            }
        }
        return loginType;
    }

    /**
     * loginType 문자열 → LoginType enum 변환
     * - 대소문자 무관 처리 ("password"/"Password" 허용)
     * - PASSWORD/PIN 외 값 → INVALID_LOGIN_TYPE(400)
     */
    private LoginType parseLoginType(String value) {
        try {
            return LoginType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(AuthErrorCode.INVALID_LOGIN_TYPE);
        }
    }

    // ---------- private helpers ----------

    /**
     * 회원가입 요청 값 검증
     * - identityToken/email/password/nickname 누락 → INVALID_SIGNUP_REQUEST
     * - pin 은 이번 API 범위 제외 (별도 PIN 등록 API 에서 처리) — 검증/저장하지 않는다
     */
    private void validateSignupRequest(SignupRequestDTO request) {
        if (request == null
                || isBlank(request.getIdentityToken())
                || isBlank(request.getEmail())
                || isBlank(request.getPassword())
                || isBlank(request.getNickname())) {
            throw new BusinessException(AuthErrorCode.INVALID_SIGNUP_REQUEST);
        }
    }

    /**
     * 이메일 검증 + 정규화 (check-email / signup 공통 정책)
     * - EmailValidator.normalize: null/blank → trim → 최대 길이(254) → lowercase → 형식 검증
     * - 실패 시 IllegalArgumentException 을 INVALID_EMAIL_FORMAT 으로 변환 (docs: 400)
     * - 반환값은 소문자 정규화된 이메일 (email_hash 가 대소문자 무관하게 동작하도록)
     */
    private String normalizeAndValidateEmail(String email) {
        try {
            return EmailValidator.normalize(email);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(AuthErrorCode.INVALID_EMAIL_FORMAT);
        }
    }

    /**
     * 회원가입 전용 JWT 검증 — 서명/만료 + sub == signup-verification 확인
     *
     * @throws BusinessException EXPIRED_SIGNUP_TOKEN(만료) / INVALID_SIGNUP_TOKEN(위변조·용도 오류)
     */
    private Claims verifySignupToken(String token) {
        try {
            return signupTokenProvider.verifySignupToken(token);
        } catch (ExpiredJwtException e) {
            throw new BusinessException(AuthErrorCode.EXPIRED_SIGNUP_TOKEN);
        } catch (JwtException e) {
            throw new BusinessException(AuthErrorCode.INVALID_SIGNUP_TOKEN);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * 검색용 SHA-256 해시 (소문자 hex)
     * - knowledge.md: 검색 필요한 개인정보는 원본 AES 암호화 + 검색용 SHA-256 hash 별도 저장
     * - spring-core DigestUtils 에 sha256DigestAsHex 가 없는 버전이므로 JDK 표준 MessageDigest 사용
     * - CI 는 평문 로그 출력 금지 — 해시 입력값 로그 금지
     */
    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }
}
