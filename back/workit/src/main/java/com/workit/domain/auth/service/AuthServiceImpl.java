package com.workit.domain.auth.service;

import com.workit.domain.auth.LoginType;
import com.workit.domain.auth.dto.request.LoginRequestDTO;
import com.workit.domain.auth.dto.request.SignupRequestDTO;
import com.workit.domain.auth.dto.response.EmailAvailabilityResponseDTO;
import com.workit.domain.auth.dto.response.IdentityVerificationResponseDTO;
import com.workit.domain.auth.dto.response.LoginResponseDTO;
import com.workit.domain.auth.dto.response.RefreshTokenResponseDTO;
import com.workit.domain.auth.dto.response.TermsListResponseDTO;
import com.workit.domain.auth.dto.response.TermsResponseDTO;
import com.workit.domain.auth.exception.AuthErrorCode;
import com.workit.domain.auth.mapper.AuthMapper;
import com.workit.domain.auth.provider.IdentityVerificationProvider;
import com.workit.domain.auth.provider.IdentityVerificationResult;
import com.workit.domain.auth.util.JwtTokenProvider;
import com.workit.domain.auth.util.SignupTokenProvider;
import com.workit.domain.auth.vo.LoginUserVO;
import com.workit.domain.auth.vo.UserAuthVO;
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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
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

    /** 회원가입 시 초기 회원 상태 (knowledge.md: users.status 기본값) */
    private static final String USER_STATUS_ACTIVE = "ACTIVE";

    /** user_profile.nickname VARCHAR(50) — 초과 시 DB 오류(500) 대신 400 으로 처리 */
    private static final int NICKNAME_MAX_LENGTH = 50;

    /**
     * PIN 실패 최대 허용 횟수 (docs: PIN_LOCK_EXCEEDED → "핀번호 입력 횟수가 5회 초과")
     * - 실패 횟수가 이 값 이상이 되면 PIN 로그인 영구 잠금 (Redis auth:fail:{userId})
     * - 잠금 해제: PIN 로그인 성공 시 초기화 또는 PASS 본인인증 후 PIN 재설정(별도 API) — 자동 해제 없음
     */
    private static final int MAX_PIN_FAIL_COUNT = 5;

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
     *
     * loginId 판별:
     * - '@' 포함 → 이메일: trim → lowercase → SHA-256 hash → email_hash 조회
     * - 그 외    → 휴대폰: trim → 하이픈 제거 → SHA-256 hash → phone_number_hash 조회
     *
     * 원문(email_encrypt/phone_encrypt)은 절대 조회하지 않는다 (knowledge.md)
     *
     * @throws BusinessException INVALID_CREDENTIALS — 회원 없음 또는 password 불일치 (원인 비노출)
     */
    private LoginUserVO loginByPassword(LoginRequestDTO request) {
        String loginId = request.getLoginId().trim();

        LoginUserVO loginUser;
        if (loginId.contains("@")) {
            String emailHash = sha256Hex(loginId.toLowerCase(Locale.ROOT));
            loginUser = authMapper.findUserByEmailHash(emailHash);
        } else {
            String phoneNumber = loginId.replace("-", "");
            String phoneHash = sha256Hex(phoneNumber);
            loginUser = authMapper.findUserByPhoneHash(phoneHash);
        }

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
