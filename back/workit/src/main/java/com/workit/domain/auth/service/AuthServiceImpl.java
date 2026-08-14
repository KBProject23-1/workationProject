package com.workit.domain.auth.service;

import com.workit.domain.auth.LoginType;
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
import com.workit.domain.auth.dto.response.TermsResponseDTO;
import com.workit.domain.auth.dto.response.VerifyIdentityResponseDTO;
import com.workit.domain.auth.exception.AuthErrorCode;
import com.workit.domain.auth.mapper.AuthMapper;
import com.workit.domain.auth.provider.IdentityVerificationProvider;
import com.workit.domain.auth.provider.IdentityVerificationResult;
import com.workit.domain.auth.util.EmailMasker;
import com.workit.domain.auth.util.JwtTokenProvider;
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

import java.time.Instant;
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
    private final MockPassStore mockPassStore;
    private final WalletService walletService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenStore refreshTokenStore;
    private final LoginFailCounter loginFailCounter;
    private final PasswordResetTokenStore passwordResetTokenStore;

    /** 회원가입 시 초기 회원 상태 (knowledge.md: users.status 기본값) */
    private static final String USER_STATUS_ACTIVE = "ACTIVE";

    /**
     * 회원가입 시 자동 생성되는 기본 닉네임 접두어
     * - 닉네임 입력 기능 제거로 서버가 "워케이너{userId}" 형식으로 생성한다.
     * - user_profile.nickname 은 NOT NULL + UNIQUE — userId 기반 생성이라 중복 불가
     */
    private static final String DEFAULT_NICKNAME_PREFIX = "워케이너";

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
    @Transactional(readOnly = true)
    public VerifyIdentityResponseDTO verifyIdentityForSignup(String identityVerificationId) {

        // 1. 요청 값 검증 — null/빈 값 → INVALID_VERIFICATION_ID(400)
        //    (findId/signup 과 동일 — javax.validation 미사용 환경, Service Layer 에서 수행)
        if (identityVerificationId == null || identityVerificationId.trim().isEmpty()) {
            throw new BusinessException(AuthErrorCode.INVALID_VERIFICATION_ID);
        }

        // 2. PASS 본인인증 결과 검증 → CI 추출
        //    - 인증 실패 시 Provider 가 BusinessException(INVALID_VERIFICATION_ID) 을 던진다
        //    - CI 는 Mock PASS 세션에 저장된 결정값(동일 휴대폰 → 동일 CI) — 원문 로그 출력 금지
        IdentityVerificationResult result =
                identityVerificationProvider.verify(identityVerificationId);

        // 3. CI SHA-256 hash 변환 → 중복 가입 회원 조회 (user_auth.identity_ci_hash UNIQUE)
        //    - CI 원문이 아닌 hash 로만 조회한다 (knowledge.md: 검색용 hash 저장)
        //    - 동일 휴대폰 번호(CI) 로 이미 가입한 회원이 있으면 회원가입 진행 불가
        //      (docs: 409 DUPLICATE_USER — "이미 가입된 회원입니다. 로그인을 진행해주세요.")
        String ciHash = sha256Hex(result.getCi());
        if (authMapper.countByCiHash(ciHash) > 0) {
            throw new BusinessException(AuthErrorCode.DUPLICATE_USER);
        }

        // 4. 가입 가능 — 화면 표시용 이름만 반환 (name/phoneNumber/CI 는 signup 에 재전송하지 않는다)
        //    - 개인정보/CI 원문 로그 출력 금지 (knowledge.md)
        return VerifyIdentityResponseDTO.of(result.getName());
    }

    @Override
    @Transactional
    public SignupResponseDTO signup(SignupRequestDTO request) {

        // 1. 요청 값 검증 (javax.validation 미사용 환경 → Service Layer 에서 수행)
        validateSignupRequest(request);

        // 2. Mock PASS 인증 세션 검증 + 인증 정보 복원
        //    - identityVerificationId 로 Redis(mock:pass:{id}) 세션을 조회·검증한다
        //      (세션 없음 / TTL 만료 / status != VERIFIED / used == true → INVALID_VERIFICATION_ID)
        //    - identityVerificationId 는 MockPassService 가 발급한 값만 유효하므로
        //      프론트가 임의 생성/우회한 인증은 이 단계에서 차단된다
        //    - Provider 가 세션에 저장된 name / phoneNumber / CI 를 복호화해 반환한다
        String identityVerificationId = request.getIdentityVerificationId().trim();
        IdentityVerificationResult verificationResult =
                identityVerificationProvider.verify(identityVerificationId);

        // 3. CI 중복 가입 검증
        //    - CI 원문을 그대로 비교하지 않고 SHA-256 해시로 변환해 조회 (knowledge.md: 검색용 hash 저장)
        //    - 이미 가입된 회원이면 회원가입 진행 불가 (docs: 409 DUPLICATE_USER)
        String ciHash = sha256Hex(verificationResult.getCi());
        if (authMapper.countByCiHash(ciHash) > 0) {
            throw new BusinessException(AuthErrorCode.DUPLICATE_USER);
        }

        // 4. 이메일 처리
        //    - 검증(blank/길이/형식) → 소문자 정규화 → SHA-256 hash 생성 (EmailValidator 공통 정책)
        //    - users.email_hash 기준 중복 재검증 (이메일 원문 DB 조회 금지)
        String normalizedEmail = normalizeAndValidateEmail(request.getEmail());
        String emailHash = sha256Hex(normalizedEmail);
        if (authMapper.countByEmailHash(emailHash) > 0) {
            throw new BusinessException(AuthErrorCode.DUPLICATE_EMAIL);
        }

        // 5. 약관 동의 검증 (DB terms 마스터 기준)
        //    - agreedTermsIds 가 null/빈 배열이면 필수 약관 동의 자체가 없으므로 실패
        //    - DB 의 필수 약관(required = 1) ID 가 모두 포함되어야 가입 가능
        //    - 선택 약관은 포함하지 않아도 가입 가능
        //    - 중복 ID 는 제거해 저장 (user_terms_agreements 에 중복 행 방지)
        List<Long> agreedTermsIds = request.getAgreedTermsIds();
        if (agreedTermsIds == null || agreedTermsIds.isEmpty()) {
            throw new BusinessException(AuthErrorCode.MISSING_REQUIRED_TERMS);
        }
        List<Long> distinctAgreedTermIds = new ArrayList<>(new LinkedHashSet<>(agreedTermsIds));

        // 5-1. 존재하지 않는 약관 ID 검증
        //    - terms 마스터에 없는 ID 가 섞여 있으면 user_terms_agreements FK 위반으로
        //      500 이 발생하므로 insert 전에 INVALID_TERM_ID(400) 로 사전 차단한다
        //    - null 요소가 포함된 경우에도 IN 쿼리가 매칭되지 않아 크기 비교로 걸러진다
        List<Long> existingTermIds = authMapper.selectExistingTermIds(distinctAgreedTermIds);
        if (existingTermIds == null || existingTermIds.size() != distinctAgreedTermIds.size()) {
            throw new BusinessException(AuthErrorCode.INVALID_TERM_ID);
        }

        // 5-2. 필수 약관 누락 검증
        List<Long> requiredTermIds = authMapper.selectRequiredTermsIds();
        if (requiredTermIds == null || !distinctAgreedTermIds.containsAll(requiredTermIds)) {
            throw new BusinessException(AuthErrorCode.MISSING_REQUIRED_TERMS);
        }

        // 6. 저장 데이터 준비 (Service Layer 에서만 암호화/해시 수행 — Controller/Mapper 금지)
        //    - password: BCrypt 단방향 해시 (원문 저장/AES 사용 금지)
        //    - email/name/phone/CI: AES-256 양방향 암호화 + 검색용 SHA-256 hash
        //      (Mock PASS 세션에서 복원한 값 — Redis 에는 원문이 아닌 암호화본만 저장되어 있었다)
        String passwordHash = PasswordEncryptor.encode(request.getPassword());

        // 7. 회원 정보 DB 저장 (users → user_auth → user_profile → user_terms_agreements) + 전자지갑 생성
        //    - 사전 중복 체크(SELECT)와 실제 insert 사이의 Race Condition 은
        //      DB UNIQUE 제약(email_hash, identity_ci_hash, nickname)이 최종 방어선이 된다.
        //    - SELECT 체크를 통과했지만 동시 요청에 의해 UNIQUE 위반이 발생하면
        //      DuplicateKeyException → 409 로 변환해 깔끔한 응답을 반환한다.
        Long userId;
        try {
            userId = insertUserWithAuthAndProfile(verificationResult, ciHash, normalizedEmail, emailHash,
                    passwordHash, distinctAgreedTermIds);
        } catch (DuplicateKeyException e) {
            throw mapDuplicateKeyException(e);
        }

        // 8. Mock PASS 세션 사용 완료 처리 (1회성 — 같은 identityVerificationId 재사용 방지)
        //    - Redis 는 DB 트랜잭션의 일부가 아니므로, DB 커밋이 확정된 후(afterCommit)에만 처리한다.
        //    - 트랜잭션 롤백 시 세션이 미사용으로 남아 사용자가 동일 인증으로 재시도할 수 있다.
        markMockPassSessionUsedAfterCommit(identityVerificationId);

        // 9. 회원가입 완료 — 토큰을 발급하지 않는다 (자동 로그인 제거)
        //    - 변경 정책: 회원가입 완료 후 로그인 화면(/login)으로 이동해 다시 로그인한다.
        //    - Access/Refresh Token 발급, Refresh Session 저장, Cookie 설정을 하지 않는다.
        //    - 응답은 userId/name 만 반환한다 (완료 화면의 로그인 아이디 안내용).

        // 10. Audit 로그 — userId 만 기록 (개인정보 원문 로그 출력 금지 — knowledge.md)
        log.info("회원가입 완료 - userId={}", userId);

        return SignupResponseDTO.of(userId, verificationResult.getName());
    }

    /**
     * 로그인 공통 — Access/Refresh Token 발급 + LoginResponseDTO 생성
     * - Payload: sub(userId), role, tokenType, iat, exp — 개인정보 없음 (knowledge.md JWT Rules)
     * - name 은 Service Layer 에서만 복호화 (Controller/Mapper 금지)
     * - accessToken/refreshToken 은 JSON 본문에 포함하지 않는다. Controller 가
     *   accessToken / refreshToken HttpOnly Cookie 로만 내려주고, Cookie Max-Age 는 각 토큰 만료와 동일하다.
     * - pinSetupRequired: 기기 최초 로그인 여부 (user_device 에 deviceId 미등록) — 로그인 화면 PIN 등록 유도 분기용
     */
    private LoginResponseDTO createLoginResponse(Long userId, String nameEncrypt, boolean pinSetupRequired) {
        String accessToken = jwtTokenProvider.createAccessToken(userId);
        String refreshToken = jwtTokenProvider.createRefreshToken(userId);
        long accessTtlSeconds = jwtTokenProvider.getAccessTokenExpirationSeconds();
        long refreshTtlSeconds = jwtTokenProvider.getRefreshTokenExpirationSeconds();
        return LoginResponseDTO.builder()
                .userId(userId)
                .name(PersonalDataCipher.decrypt(nameEncrypt))
                .pinSetupRequired(pinSetupRequired)
                .accessToken(accessToken)
                .accessTokenMaxAgeSeconds(accessTtlSeconds)
                .refreshToken(refreshToken)
                .refreshTokenMaxAgeSeconds(refreshTtlSeconds)
                .build();
    }

    /**
     * users → user_auth → user_profile → user_terms_agreements insert + 전자지갑 생성 (동일 트랜잭션)
     * - 회원가입 전체 과정이 하나의 트랜잭션 — 하나라도 실패하면 전부 롤백된다
     *
     * @param verificationResult Mock PASS 세션에서 복원한 인증 정보 (name/phoneNumber/CI)
     * @return 생성된 userId (회원가입 응답에 사용)
     */
    private Long insertUserWithAuthAndProfile(IdentityVerificationResult verificationResult,
                                              String ciHash,
                                              String normalizedEmail,
                                              String emailHash,
                                              String passwordHash,
                                              List<Long> agreedTermIds) {
        // users insert (회원 기본 정보)
        // - name/phone 는 Mock PASS 세션에서 복원한 값을 AES-256 암호화해 저장
        // - phone_hash 는 SHA-256 계산 — users.phone_number_hash (UNIQUE)
        UserVO user = new UserVO();
        user.setEmailHash(emailHash);
        user.setEmailEncrypt(PersonalDataCipher.encrypt(normalizedEmail));
        user.setNameEncrypt(PersonalDataCipher.encrypt(verificationResult.getName()));
        user.setPhoneNumberHash(sha256Hex(verificationResult.getPhoneNumber()));
        user.setPhoneNumberEncrypt(PersonalDataCipher.encrypt(verificationResult.getPhoneNumber()));
        user.setStatus(USER_STATUS_ACTIVE);
        authMapper.insertUser(user);

        // user_auth insert (인증 정보 — 비밀번호/CI)
        UserAuthVO userAuth = new UserAuthVO();
        userAuth.setUserId(user.getId());
        userAuth.setPasswordHash(passwordHash);
        userAuth.setIdentityCiHash(ciHash);
        userAuth.setIdentityCiEncrypt(PersonalDataCipher.encrypt(verificationResult.getCi()));
        authMapper.insertUserAuth(userAuth);

        // user_profile insert (기본 닉네임 — 닉네임 입력 기능 제거, 서버가 자동 생성)
        // - user_profile.nickname NOT NULL + UNIQUE — userId 기반 생성이라 중복 불가
        UserProfileVO userProfile = new UserProfileVO();
        userProfile.setUserId(user.getId());
        userProfile.setNickname(DEFAULT_NICKNAME_PREFIX + user.getId());
        authMapper.insertUserProfile(userProfile);

        // user_terms_agreements insert (약관 동의 저장 — 동의한 약관 ID 목록 전체)
        authMapper.insertUserTerms(user.getId(), agreedTermIds);

        // 전자지갑 생성 (knowledge.md Signup Flow: 8. Create wallet)
        // 같은 트랜잭션 내에서 생성 — 지갑 생성 실패 시 DB insert 전체가 롤백된다
        walletService.createWallet(user.getId());

        return user.getId();
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
        if (message.contains("ux_users_phone") || message.contains("users.phone_number_hash")) {
            // 동일 휴대폰으로 이미 가입된 회원 — 1인 1계정 정책상 CI 중복과 동일하게 처리
            return new BusinessException(AuthErrorCode.DUPLICATE_USER);
        }
        // 식별되지 않은 UNIQUE 충돌 — 응답에 제약조건명/테이블명 노출 금지 (knowledge.md)
        return new BusinessException(AuthErrorCode.DUPLICATE_USER);
    }

    /**
     * DB 트랜잭션이 커밋된 후(afterCommit)에만 Mock PASS 세션을 사용 완료 처리한다.
     * - 트랜잭션이 진행 중일 때 used=true 로 바꾸면 롤백 시 사용자가 재시도할 수 없게 된다.
     * - 실제 트랜잭션 밖(테스트 등)에서는 즉시 처리한다.
     */
    private void markMockPassSessionUsedAfterCommit(String identityVerificationId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    mockPassStore.markUsed(identityVerificationId);
                }
            });
        } else {
            mockPassStore.markUsed(identityVerificationId);
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

        // 4. JWT 발급 + 응답 생성 (JwtTokenProvider 재사용 — Payload: sub(userId), role, tokenType, iat, exp)
        //    - pinSetupRequired: PASSWORD 로그인 + deviceId 전달 시 user_device 등록 여부로 판별
        //      (기기 최초 로그인 → true → 프론트에서 PIN 등록 화면 유도. PIN 로그인은 등록 기기에서만 성공하므로 항상 false)
        boolean pinSetupRequired = (loginType == LoginType.PASSWORD && !isBlank(request.getDeviceId()))
                && authMapper.countByUserIdAndDeviceId(loginUser.getId(), request.getDeviceId()) == 0;
        LoginResponseDTO response = createLoginResponse(loginUser.getId(), loginUser.getNameEncrypt(), pinSetupRequired);

        // 5. Refresh Token Redis 저장 (knowledge.md Refresh Token Security)
        //    - 원문이 아닌 SHA-256 hash 저장 — key: refresh:token:{userId}, TTL: refresh 만료와 동일
        //    - login 은 DB 쓰기가 없으므로 트랜잭션 없이 즉시 저장한다
        refreshTokenStore.save(loginUser.getId(),
                sha256Hex(response.getRefreshToken()),
                response.getRefreshTokenMaxAgeSeconds());

        return response;
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

        // 8. 응답 생성 — accessToken/refreshToken 은 JSON 본문에 포함하지 않는다.
        //    Controller 가 accessToken / refreshToken HttpOnly Cookie 로만 내려주고,
        //    Cookie Max-Age 는 각 토큰 만료와 동일하다 (Rotation 으로 신규 발급된 값).
        return RefreshTokenResponseDTO.builder()
                .accessToken(accessToken)
                .accessTokenMaxAgeSeconds(jwtTokenProvider.getAccessTokenExpirationSeconds())
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
        //    (signup 과 동일 — javax.validation 미사용 환경, Service Layer 에서 수행)
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
    // SELECT 만 수행하므로 읽기 전용 트랜잭션 (checkEmailAvailability 와 동일)
    @Transactional(readOnly = true)
    public void checkPasswordResetId(String loginId) {

        // 1. 요청 값 검증 — loginId 누락 → INVALID_PASSWORD_RESET_REQUEST(400)
        //    (verifyPasswordReset 과 동일 — javax.validation 미사용 환경, Service Layer 에서 수행)
        if (isBlank(loginId)) {
            throw new BusinessException(AuthErrorCode.INVALID_PASSWORD_RESET_REQUEST);
        }

        // 2. loginId(이메일 또는 휴대폰) 기준 회원 조회 — loginByPassword 와 동일한 판별 규칙 재사용
        //    - 개인정보 원문(email_encrypt/phone_encrypt)은 절대 조회하지 않는다 (knowledge.md: 검색용 hash)
        //    - 회원 없음 → USER_NOT_FOUND(404) — 아이디 입력 화면에서 재확인 안내
        LoginUserVO user = findUserByLoginId(loginId);
        if (user == null) {
            throw new BusinessException(AuthErrorCode.USER_NOT_FOUND);
        }

        // 3. 사용자 상태 확인 — ACTIVE 만 비밀번호 재설정 허용
        //    - 탈퇴(WITHDRAWN)/차단(BLOCKED) 등 비활성 회원은 계정 존재 여부를 노출하지 않고
        //      USER_NOT_FOUND(404) 로 처리 (findId/verifyPasswordReset 과 동일 정책)
        if (!USER_STATUS_ACTIVE.equals(user.getStatus())) {
            throw new BusinessException(AuthErrorCode.USER_NOT_FOUND);
        }

        // 4. Audit 로그 — userId 만 기록 (개인정보 원문 로그 출력 금지)
        log.info("비밀번호 재설정 아이디 확인 성공 - userId={}", user.getId());
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

        // 4-1. PASS 인증 이름과 가입 이름 일치 확인 — Mock CI 는 휴대폰 번호 기반(MOCK-CI-{sha256(phone)})
        //      이므로 휴대폰 번호를 알면 이름이 달라도 CI 가 일치한다. 계정 소유자 본인 확인을 위해
        //      PASS 인증에 입력된 이름과 가입 시 등록된 이름(users.name_encrypt 복호화)을 함께 대조한다.
        //      - 이름 불일치 → VERIFICATION_FAILED(400) (docs — 원인 비노출)
        String registeredName = PersonalDataCipher.decrypt(user.getNameEncrypt());
        if (!registeredName.equals(result.getName())) {
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
        //      (RedisMockPassStore 패턴과 동일)
        String passwordResetToken = UUID.randomUUID().toString();
        passwordResetTokenStore.save(passwordResetToken, user.getId());

        // 6-1. 만료 시각(expiresAt) 계산 — 프론트 5분 카운트다운 표시용
        //      - TTL 은 저장소가 소유하므로 저장소에서 조회해 계산한다 (Service 하드코딩 금지)
        long expiresAt = Instant.now().plus(passwordResetTokenStore.getTtl()).toEpochMilli();

        // 7. Audit 로그 — userId 만 기록 (토큰/개인정보 원문 로그 출력 금지)
        log.info("비밀번호 재설정 토큰 발급 - userId={}", user.getId());

        return PasswordVerifyResponseDTO.of(passwordResetToken, expiresAt);
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
    // 비밀번호 변경은 user_auth UPDATE(DB 쓰기) + Redis Refresh Token 폐기의 조합이므로
    // resetPassword 와 동일하게 트랜잭션 경계를 Service 에 두고, Redis 폐기는 DB 커밋 확정 후(afterCommit) 수행한다
    public void changePassword(Long userId, ChangePasswordRequestDTO request) {

        // 1. 요청 값 검증 (javax.validation 미사용 환경 → Service Layer 에서 수행)
        //    - currentPassword/newPassword 누락·빈 값 → INVALID_PASSWORD_CHANGE_REQUEST(400)
        //      (docs: INVALID_REQUEST "필수 입력값을 확인해 주세요.")
        if (request == null
                || isBlank(request.getCurrentPassword())
                || isBlank(request.getNewPassword())) {
            throw new BusinessException(AuthErrorCode.INVALID_PASSWORD_CHANGE_REQUEST);
        }

        // 2~4. 현재 비밀번호 본인 인증 — verifyCurrentPassword 공통 로직 재사용
        //      - 회원 존재/ACTIVE 확인 → USER_NOT_FOUND(404)
        //      - 현재 비밀번호 BCrypt 검증 → 불일치 시 AUTH_INVALID_PASSWORD(400)
        //      (회원 탈퇴 등 민감 작업과 동일한 검증을 공유 — 중복 구현 금지)
        verifyCurrentPassword(userId, request.getCurrentPassword());

        // 5. 신규 비밀번호 정책 검증 — 영문/숫자/특수문자 포함 8자 이상 (docs: WEAK_PASSWORD 422)
        //    - resetPassword 와 동일한 정책 재사용 (프로젝트 공통 비밀번호 규칙)
        validatePasswordPolicy(request.getNewPassword());

        // 6. 신규 비밀번호가 현재 비밀번호와 동일한지 확인 (docs: AUTH_SAME_PASSWORD 400)
        //    - BCrypt matches() 검증이므로 비밀번호 원문을 조회/복호화하지 않는다 (원문 저장 금지)
        //    - verifyCurrentPassword 가 검증한 현재 hash 를 재조회해 대조한다 (동일 트랜잭션 내 단순 재조회)
        //    - resetPin 의 SAME_AS_CURRENT_PIN 대조 방식과 동일 패턴
        String currentPasswordHash = authMapper.selectPasswordHashByUserId(userId);
        if (PasswordEncryptor.matches(request.getNewPassword(), currentPasswordHash)) {
            throw new BusinessException(AuthErrorCode.AUTH_SAME_PASSWORD);
        }

        // 7. 신규 비밀번호 BCrypt 암호화 — 원문 저장/복호화 금지 (knowledge.md: 비밀번호는 BCrypt 단방향 해시)
        String newPasswordHash = PasswordEncryptor.encode(request.getNewPassword());

        // 8. user_auth.password_hash 갱신
        //    - 갱신 행 수가 0 이면 해당 userId 의 인증 정보가 없다(회원 탈퇴 등) → 변경 불가
        int updated = authMapper.updatePasswordHash(userId, newPasswordHash);
        if (updated == 0) {
            throw new BusinessException(AuthErrorCode.USER_NOT_FOUND);
        }

        // 9. 기존 Refresh Token 전체 폐기 (knowledge.md: 비밀번호 변경 후 기존 Refresh Token 전체 폐기)
        //    - Redis(refresh:token:{userId}) 삭제 — DB 커밋 확정 후(afterCommit) 수행
        //      (트랜잭션 롤백 시 세션이 유지되어 사용자가 재시도할 수 있어야 한다)
        //    - Access Token 은 Stateless 이므로 만료까지 유지된다 (JWT 구조 변경/신규 발급 없음)
        deleteRefreshTokenAfterCommit(userId);

        // 10. Audit 로그 (knowledge.md Audit Log Policy: 비밀번호 변경 기록 대상)
        //     - userId 는 민감정보가 아니며, 비밀번호 원문/해시는 로그에 포함하지 않는다
        log.info("비밀번호 변경 성공 - userId={}", userId);
    }

    @Override
    // SELECT 만 수행하므로 읽기 전용 트랜잭션 (findId/checkPasswordResetId 와 동일)
    @Transactional(readOnly = true)
    public void verifyCurrentPassword(Long userId, String rawPassword) {

        // 1. JWT 로그인 사용자 조회 + 상태 확인 (users.status)
        //    - 탈퇴/차단/미존재 회원은 계정 존재 여부를 노출하지 않고 USER_NOT_FOUND(404) 로 처리
        //      (changePassword/setupPin 과 동일 정책 — docs: 404 USER_NOT_FOUND)
        LoginUserVO user = authMapper.findUserById(userId);
        if (user == null || !USER_STATUS_ACTIVE.equals(user.getStatus())) {
            throw new BusinessException(AuthErrorCode.USER_NOT_FOUND);
        }

        // 2. 현재 비밀번호 hash(BCrypt) 조회 — 본인 인증용
        //    - BCrypt 는 단방향 해시이므로 원문을 조회/복호화하지 않고 해시를 그대로 matches() 에 사용한다
        //      (knowledge.md: 비밀번호 원문 저장 금지, 검증은 BCrypt matches)
        String currentPasswordHash = authMapper.selectPasswordHashByUserId(userId);
        if (currentPasswordHash == null) {
            // user_auth 행이 없는 회원(회원 탈퇴 등 비정상 상태) → 인증 정보 없음
            throw new BusinessException(AuthErrorCode.USER_NOT_FOUND);
        }

        // 3. 현재 비밀번호 BCrypt 검증 — 본인 인증
        //    - 불일치 → AUTH_INVALID_PASSWORD(400) (docs: "현재 비밀번호가 올바르지 않습니다.")
        //    - 비밀번호 원문은 로그에 출력하지 않는다 (민감정보)
        if (!PasswordEncryptor.matches(rawPassword, currentPasswordHash)) {
            throw new BusinessException(AuthErrorCode.AUTH_INVALID_PASSWORD);
        }
    }

    @Override
    // Redis 삭제(side-effect)만 수행하므로 별도 @Transactional 을 사용하지 않는다
    // (logout 과 동일 — Redis 는 DB 트랜잭션에 참여하지 않음)
    public void revokeAllRefreshSessions(Long userId) {
        // 모든 Refresh Token 세션 폐기 — DB 커밋 확정 후(afterCommit) 수행
        // (changePassword 의 deleteRefreshTokenAfterCommit 패턴 재사용 — 트랜잭션 롤백 시 세션 유지)
        deleteRefreshTokenAfterCommit(userId);
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
        //      (findId 와 동일 정책)
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

        // 6. 신규 PIN 이 기존 PIN 과 동일한지 확인 (docs: SAME_AS_CURRENT_PIN 400)
        //    - PIN 은 기기별(user_device) 저장되므로 등록된 전체 기기의 pin_hash 와 대조한다
        //    - BCrypt matches() 검증이므로 PIN 원문을 조회/복호화하지 않는다 (원문 저장 금지)
        List<String> existingPinHashes = authMapper.selectPinHashesByUserId(userId);
        if (existingPinHashes != null) {
            for (String existingPinHash : existingPinHashes) {
                if (PasswordEncryptor.matches(request.getPinNumber(), existingPinHash)) {
                    throw new BusinessException(AuthErrorCode.SAME_AS_CURRENT_PIN);
                }
            }
        }

        // 7. BCrypt 암호화 — PIN 원문 저장/복호화 금지 (knowledge.md)
        String pinHash = PasswordEncryptor.encode(request.getPinNumber());

        // 8. user_device.pin_hash 갱신 (user_id 기준 — 등록된 전체 기기에 동일 적용)
        //    - 갱신 행 수가 0 이면 등록된 PIN(기기)이 없는 회원 → 재설정 불가
        int updated = authMapper.updateUserDevicePinHash(userId, pinHash);
        if (updated == 0) {
            throw new BusinessException(AuthErrorCode.PIN_NOT_REGISTERED);
        }

        // 9. PIN 실패 횟수 초기화 — 잠금 해제 (knowledge.md PIN Policy)
        //    - "잠금 해제: PIN 로그인 성공 시 초기화 또는 PASS 본인인증 후 PIN 재설정"
        //    - PASS 재인증으로 본인 확인이 완료된 시점이므로, 실패 횟수 5회로 잠긴 유저도
        //      신규 PIN 으로 다시 로그인할 수 있어야 한다 (resetPin 이 잠금 해제 수단)
        loginFailCounter.reset(userId);

        // 10. Audit 로그 (knowledge.md Audit Log Policy: PIN 변경 기록 대상)
        //     - userId 는 민감정보가 아니며, PIN 원문/해시는 로그에 포함하지 않는다
        log.info("PIN 재설정 성공 - userId={}", userId);
    }

    /**
     * PIN 설정 요청 값 검증 — 필수 값 누락/빈 값/길이 초과 → INVALID_PIN_SETUP_REQUEST(400)
     * - javax.validation 미사용 환경 → Service Layer 에서 수행 (signup/login 과 동일)
     * - pinNumber 가 비어 있으면 형식 검증(6자리) 이전에 차단된다
     * - deviceId/deviceName 은 ERD VARCHAR(100) 초과 시 DB 오류(500) 대신 400 으로 사전 차단
     *   (DB VARCHAR 컬럼 길이 초과 사전 차단 공통 패턴)
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
     * DB 트랜잭션이 커밋된 후(afterCommit)에만 사용자의 Refresh Token 세션을 삭제한다.
     * - 비밀번호 변경 시 기존 Refresh Token 전체 폐기 — 트랜잭션이 진행 중일 때 Redis 를 지우면
     *   롤백 시 세션이 사라진 상태로 남아 사용자가 재시도할 수 없게 된다.
     * - 실제 트랜잭션 밖(테스트 등)에서는 즉시 삭제한다 (deletePasswordResetTokenAfterCommit 과 동일 패턴).
     */
    private void deleteRefreshTokenAfterCommit(Long userId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    refreshTokenStore.delete(userId);
                }
            });
        } else {
            refreshTokenStore.delete(userId);
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
     * - identityVerificationId/email/password 누락 → INVALID_SIGNUP_REQUEST
     * - pin 은 이번 API 범위 제외 (별도 PIN 등록 API 에서 처리) — 검증/저장하지 않는다
     */
    private void validateSignupRequest(SignupRequestDTO request) {
        if (request == null
                || isBlank(request.getIdentityVerificationId())
                || isBlank(request.getEmail())
                || isBlank(request.getPassword())) {
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
