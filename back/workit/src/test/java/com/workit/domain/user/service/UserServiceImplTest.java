package com.workit.domain.user.service;

import com.workit.domain.auth.exception.AuthErrorCode;
import com.workit.domain.auth.provider.IdentityVerificationProvider;
import com.workit.domain.auth.provider.IdentityVerificationResult;
import com.workit.domain.auth.service.AuthService;
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
import com.workit.domain.user.exception.UserErrorCode;
import com.workit.domain.user.mapper.UserMapper;
import com.workit.domain.user.vo.MyProfileVO;
import com.workit.domain.user.vo.UserProfileVO;
import com.workit.domain.wallet.exception.WalletErrorCode;
import com.workit.domain.wallet.service.WalletService;
import com.workit.exception.BusinessException;
import com.workit.exception.CommonErrorCode;
import com.workit.global.util.PersonalDataCipher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// UserServiceImpl (내 프로필 조회 / 프로필 최초 등록) 테스트
// - UserMapper 를 Mockito @Mock 으로 주입한다 (Service 계층 검증에 집중)
// - PersonalDataCipher 는 시스템 프로퍼티로 AES 키를 주입해 실제 복호화를 검증한다
//   (AuthServiceImplTest 와 동일 방식)
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    /** 테스트용 AES 키 (32바이트) — PersonalDataCipher 키 로드 규칙 1순위(시스템 프로퍼티) 사용 */
    private static final String TEST_AES_KEY = "0123456789abcdef0123456789abcdef";

    /** 암호화된 테스트 개인정보 값 (AES 키는 고정이므로 encrypt 결과는 결정적) */
    private static final String EMAIL = "user@example.com";
    private static final String NAME = "홍길동";
    private static final String PHONE_NUMBER = "01012345678";

    /** PASS 인증을 통해 변경할 새 휴대폰 번호 (프론트는 전달하지 않는다 — Provider 결과 값) */
    private static final String NEW_PHONE_NUMBER = "01098765432";

    /** 이메일 인증을 완료해 변경할 새 이메일 (프론트는 전달하지 않는다 — 인증 세션에서 조회한 값) */
    private static final String NEW_EMAIL = "new@example.com";

    @Mock
    private UserMapper userMapper;

    // 회원 탈퇴 시 비밀번호 검증/Refresh 세션 revoke 를 AuthService 로 위임한다 (Mock 주입)
    @Mock
    private AuthService authService;

    // 회원 탈퇴 시 지갑 잔액 조회를 WalletService 로 위임한다 (Mock 주입)
    @Mock
    private WalletService walletService;

    // 휴대폰 번호 변경 시 PASS 본인인증 결과 검증을 IdentityVerificationProvider 로 위임한다 (Mock 주입)
    @Mock
    private IdentityVerificationProvider identityVerificationProvider;

    // 이메일 인증번호 발송 시 Mock Email Verification Service 로 위임한다 (Mock 주입)
    @Mock
    private EmailVerificationService emailVerificationService;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        // AES 키 주입 후 PersonalDataCipher 재로드 (AuthServiceImplTest 와 동일 패턴)
        System.setProperty("personal.data.aes.key", TEST_AES_KEY);
        PersonalDataCipher.reloadKey();

        userService = new UserServiceImpl(userMapper, authService, walletService,
                identityVerificationProvider, emailVerificationService);
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("personal.data.aes.key");
        PersonalDataCipher.reloadKey();
    }

    // ---------- 테스트 데이터 헬퍼 ----------

    /** ACTIVE 상태 + 프로필 미등록 회원 (LEFT JOIN 결과 — profileId/nickname null) */
    private MyProfileVO activeUserWithoutProfile() {
        MyProfileVO vo = new MyProfileVO();
        vo.setUserId(501L);
        vo.setStatus("ACTIVE");
        vo.setEmailEncrypted(PersonalDataCipher.encrypt(EMAIL));
        vo.setNameHash(sha256Hex(NAME));
        vo.setNameEncrypted(PersonalDataCipher.encrypt(NAME));
        vo.setPhoneNumberEncrypted(PersonalDataCipher.encrypt(PHONE_NUMBER));
        return vo;
    }

    /** ACTIVE 상태 + 프로필 등록 완료 회원 */
    private MyProfileVO activeUserWithProfile() {
        MyProfileVO vo = activeUserWithoutProfile();
        vo.setProfileId(12L);
        vo.setNickname("지갑대장홍길동");
        vo.setCompanyName("6인조테크");
        return vo;
    }

    // ---------- 내 프로필 조회 ----------

    @Test
    @DisplayName("내 프로필 조회 성공 - 개인정보 복호화 + nickname/companyName 반환")
    void getMyProfile_success() {
        // Given — 프로필 등록 완료 회원
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(activeUserWithProfile());

        // When
        MyProfileResponseDTO result = userService.getMyProfile(501L);

        // Then — Service Layer 에서 복호화된 원문 값
        assertEquals(EMAIL, result.getEmail());
        assertEquals(NAME, result.getName());
        assertEquals(PHONE_NUMBER, result.getPhoneNumber());
        assertEquals("지갑대장홍길동", result.getNickname());
        assertEquals("6인조테크", result.getCompanyName());
    }

    @Test
    @DisplayName("내 프로필 조회 성공 - 프로필 미등록 회원은 nickname/companyName 이 null")
    void getMyProfile_withoutProfile() {
        // Given — 프로필 미등록 회원 (LEFT JOIN 결과 nickname/companyName null)
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(activeUserWithoutProfile());

        // When
        MyProfileResponseDTO result = userService.getMyProfile(501L);

        // Then — 기본 정보는 복호화되어 반환, 프로필 값은 null (docs: 최초 등록 전 null 가능)
        assertEquals(EMAIL, result.getEmail());
        assertEquals(NAME, result.getName());
        assertEquals(PHONE_NUMBER, result.getPhoneNumber());
        assertNull(result.getNickname());
        assertNull(result.getCompanyName());
    }

    @Test
    @DisplayName("내 프로필 조회 - 회원 없음 → USER_NOT_FOUND")
    void getMyProfile_userNotFound() {
        // Given — Mapper 가 null 반환 (회원 없음)
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(null);

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.getMyProfile(501L));
        assertEquals(UserErrorCode.USER_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("내 프로필 조회 - 비활성 회원(탈퇴/차단) → USER_NOT_FOUND (계정 존재 여부 비노출)")
    void getMyProfile_withdrawnUser() {
        // Given — WITHDRAWN 상태 회원
        MyProfileVO withdrawn = activeUserWithoutProfile();
        withdrawn.setStatus("WITHDRAWN");
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(withdrawn);

        // When & Then — 탈퇴 회원도 USER_NOT_FOUND 로 통일 처리
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.getMyProfile(501L));
        assertEquals(UserErrorCode.USER_NOT_FOUND, ex.getErrorCode());
    }

    // ---------- 프로필 최초 등록 ----------

    @Test
    @DisplayName("프로필 최초 등록 성공 - insert 호출 + profileId/userId/nickname/companyName 반환")
    void onboardProfile_success() {
        // Given — 프로필 미등록 ACTIVE 회원 + nickname 미사용
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(activeUserWithoutProfile());
        when(userMapper.countByNickname("지갑대장홍길동")).thenReturn(0);

        ProfileOnboardingRequestDTO request = new ProfileOnboardingRequestDTO();
        request.setNickname("지갑대장홍길동");
        request.setCompanyName("6인조테크");

        // When
        ProfileOnboardingResponseDTO result = userService.onboardProfile(501L, request);

        // Then — insert 호출 확인 (ArgumentCaptor 로 저장 값 검증)
        ArgumentCaptor<UserProfileVO> captor = ArgumentCaptor.forClass(UserProfileVO.class);
        verify(userMapper).insertUserProfile(captor.capture());
        UserProfileVO saved = captor.getValue();
        assertEquals(501L, saved.getUserId());
        assertEquals("지갑대장홍길동", saved.getNickname());
        assertEquals("6인조테크", saved.getCompanyName());

        assertEquals(501L, result.getUserId());
        assertEquals("지갑대장홍길동", result.getNickname());
        assertEquals("6인조테크", result.getCompanyName());
    }

    @Test
    @DisplayName("프로필 최초 등록 성공 - companyName 미입력 시 null 저장 + insert 호출")
    void onboardProfile_withoutCompanyName() {
        // Given — 프로필 미등록 ACTIVE 회원 + nickname 미사용
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(activeUserWithoutProfile());
        when(userMapper.countByNickname("tester")).thenReturn(0);

        ProfileOnboardingRequestDTO request = new ProfileOnboardingRequestDTO();
        request.setNickname("tester");

        // When
        ProfileOnboardingResponseDTO result = userService.onboardProfile(501L, request);

        // Then — companyName 은 null 로 저장
        ArgumentCaptor<UserProfileVO> captor = ArgumentCaptor.forClass(UserProfileVO.class);
        verify(userMapper).insertUserProfile(captor.capture());
        assertNull(captor.getValue().getCompanyName());

        assertNull(result.getCompanyName());
        assertEquals("tester", result.getNickname());
    }

    @Test
    @DisplayName("프로필 최초 등록 - nickname 누락 → INVALID_PROFILE_REQUEST + insert 미호출")
    void onboardProfile_missingNickname() {
        // Given — 프로필 미등록 ACTIVE 회원 (사용자 확인 통과) + nickname 없는 요청
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(activeUserWithoutProfile());

        ProfileOnboardingRequestDTO nullRequest = new ProfileOnboardingRequestDTO();
        ProfileOnboardingRequestDTO blankRequest = new ProfileOnboardingRequestDTO();
        blankRequest.setNickname("   ");

        // When & Then — 모두 INVALID_PROFILE_REQUEST
        assertThrows(BusinessException.class, () -> userService.onboardProfile(501L, null));
        assertThrows(BusinessException.class, () -> userService.onboardProfile(501L, nullRequest));
        assertThrows(BusinessException.class, () -> userService.onboardProfile(501L, blankRequest));

        // Then — 저장 Mapper 미호출
        verify(userMapper, never()).insertUserProfile(any(UserProfileVO.class));
    }

    @Test
    @DisplayName("프로필 최초 등록 - nickname 중복 → DUPLICATE_NICKNAME + insert 미호출")
    void onboardProfile_duplicateNickname() {
        // Given — 프로필 미등록 ACTIVE 회원 + nickname 이미 사용 중
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(activeUserWithoutProfile());
        when(userMapper.countByNickname("지갑대장홍길동")).thenReturn(1);

        ProfileOnboardingRequestDTO request = new ProfileOnboardingRequestDTO();
        request.setNickname("지갑대장홍길동");

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.onboardProfile(501L, request));
        assertEquals(UserErrorCode.DUPLICATE_NICKNAME, ex.getErrorCode());

        // Then — 저장 Mapper 미호출
        verify(userMapper, never()).insertUserProfile(any(UserProfileVO.class));
    }

    @Test
    @DisplayName("프로필 최초 등록 - 이미 프로필 등록된 사용자 → PROFILE_ALREADY_EXISTS + insert 미호출")
    void onboardProfile_alreadyRegistered() {
        // Given — 프로필 등록 완료 회원 (profileId 존재)
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(activeUserWithProfile());

        ProfileOnboardingRequestDTO request = new ProfileOnboardingRequestDTO();
        request.setNickname("새닉네임");

        // When & Then — 최초 등록 API 재호출 불가
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.onboardProfile(501L, request));
        assertEquals(UserErrorCode.PROFILE_ALREADY_EXISTS, ex.getErrorCode());

        // Then — 저장 Mapper 미호출
        verify(userMapper, never()).insertUserProfile(any(UserProfileVO.class));
    }

    @Test
    @DisplayName("프로필 최초 등록 - 사용자 없음 → USER_NOT_FOUND + insert 미호출")
    void onboardProfile_userNotFound() {
        // Given — 회원 없음
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(null);

        ProfileOnboardingRequestDTO request = new ProfileOnboardingRequestDTO();
        request.setNickname("지갑대장홍길동");

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.onboardProfile(501L, request));
        assertEquals(UserErrorCode.USER_NOT_FOUND, ex.getErrorCode());

        // Then — 저장 Mapper 미호출
        verify(userMapper, never()).insertUserProfile(any(UserProfileVO.class));
    }

    @Test
    @DisplayName("프로필 최초 등록 - 비활성 회원(탈퇴) → USER_NOT_FOUND + insert 미호출")
    void onboardProfile_withdrawnUser() {
        // Given — WITHDRAWN 상태 회원
        MyProfileVO withdrawn = activeUserWithoutProfile();
        withdrawn.setStatus("WITHDRAWN");
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(withdrawn);

        ProfileOnboardingRequestDTO request = new ProfileOnboardingRequestDTO();
        request.setNickname("지갑대장홍길동");

        // When & Then — 탈퇴 회원도 USER_NOT_FOUND 로 통일 처리
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.onboardProfile(501L, request));
        assertEquals(UserErrorCode.USER_NOT_FOUND, ex.getErrorCode());

        verify(userMapper, never()).insertUserProfile(any(UserProfileVO.class));
    }

    @Test
    @DisplayName("프로필 최초 등록 - nickname 길이 초과(51자) → INVALID_PROFILE_REQUEST + insert 미호출")
    void onboardProfile_nicknameTooLong() {
        // Given — 프로필 미등록 ACTIVE 회원 (사용자 확인 통과) + VARCHAR(50) 초과 닉네임
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(activeUserWithoutProfile());

        ProfileOnboardingRequestDTO request = new ProfileOnboardingRequestDTO();
        request.setNickname(buildString(51, 'a'));

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.onboardProfile(501L, request));
        assertEquals(UserErrorCode.INVALID_PROFILE_REQUEST, ex.getErrorCode());

        // Then — 저장 Mapper 미호출
        verify(userMapper, never()).insertUserProfile(any(UserProfileVO.class));
    }

    @Test
    @DisplayName("프로필 최초 등록 - nickname 50자 정상 → insert 호출")
    void onboardProfile_nicknameMaxLength() {
        // Given — VARCHAR(50) 최대 길이 닉네임 + 미사용
        String nickname = buildString(50, 'a');
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(activeUserWithoutProfile());
        when(userMapper.countByNickname(nickname)).thenReturn(0);

        ProfileOnboardingRequestDTO request = new ProfileOnboardingRequestDTO();
        request.setNickname(nickname);

        // When
        ProfileOnboardingResponseDTO result = userService.onboardProfile(501L, request);

        // Then — 정상 저장
        verify(userMapper).insertUserProfile(any(UserProfileVO.class));
        assertEquals(nickname, result.getNickname());
    }

    @Test
    @DisplayName("프로필 최초 등록 - companyName 길이 초과(101자) → INVALID_PROFILE_REQUEST + insert 미호출")
    void onboardProfile_companyNameTooLong() {
        // Given — 프로필 미등록 ACTIVE 회원 (사용자 확인 통과) + VARCHAR(100) 초과 회사명
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(activeUserWithoutProfile());

        ProfileOnboardingRequestDTO request = new ProfileOnboardingRequestDTO();
        request.setNickname("지갑대장홍길동");
        request.setCompanyName(buildString(101, 'b'));

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.onboardProfile(501L, request));
        assertEquals(UserErrorCode.INVALID_PROFILE_REQUEST, ex.getErrorCode());

        verify(userMapper, never()).insertUserProfile(any(UserProfileVO.class));
    }

    // ---------- 프로필 수정 ----------

    @Test
    @DisplayName("프로필 수정 성공 - nickname 만 수정 (PATCH: companyName 은 유지)")
    void updateProfile_nicknameOnly() {
        // Given — 프로필 등록 완료 회원 (기존 nickname: 지갑대장홍길동, companyName: 6인조테크)
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(activeUserWithProfile());
        when(userMapper.updateUserProfile(any(UserProfileVO.class))).thenReturn(1);

        ProfileUpdateRequestDTO request = new ProfileUpdateRequestDTO();
        request.setNickname("새로운닉네임");

        // When
        userService.updateProfile(501L, request);

        // Then — nickname 만 전달되어 동적 UPDATE (companyName 은 미전달 → XML <if> 로 제외)
        ArgumentCaptor<UserProfileVO> captor = ArgumentCaptor.forClass(UserProfileVO.class);
        verify(userMapper).updateUserProfile(captor.capture());
        UserProfileVO saved = captor.getValue();
        assertEquals(501L, saved.getUserId());
        assertEquals("새로운닉네임", saved.getNickname());
        assertNull(saved.getCompanyName());
        assertFalse(saved.isUpdateCompanyName());
    }

    @Test
    @DisplayName("프로필 수정 성공 - companyName 만 수정 (PATCH: nickname 은 유지)")
    void updateProfile_companyNameOnly() {
        // Given — 프로필 등록 완료 회원
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(activeUserWithProfile());
        when(userMapper.updateUserProfile(any(UserProfileVO.class))).thenReturn(1);

        ProfileUpdateRequestDTO request = new ProfileUpdateRequestDTO();
        request.setCompanyName("구글코리아");

        // When
        userService.updateProfile(501L, request);

        // Then — companyName 만 전달되어 동적 UPDATE (nickname 은 미전달 → XML <if> 로 제외)
        ArgumentCaptor<UserProfileVO> captor = ArgumentCaptor.forClass(UserProfileVO.class);
        verify(userMapper).updateUserProfile(captor.capture());
        UserProfileVO saved = captor.getValue();
        assertEquals(501L, saved.getUserId());
        assertNull(saved.getNickname());
        assertEquals("구글코리아", saved.getCompanyName());
        assertTrue(saved.isUpdateCompanyName());
    }

    @Test
    @DisplayName("프로필 수정 성공 - nickname/companyName 둘 다 수정 + trim 적용")
    void updateProfile_bothFields() {
        // Given — 프로필 등록 완료 회원
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(activeUserWithProfile());
        when(userMapper.updateUserProfile(any(UserProfileVO.class))).thenReturn(1);

        ProfileUpdateRequestDTO request = new ProfileUpdateRequestDTO();
        request.setNickname("  새닉네임  ");
        request.setCompanyName("  구글코리아  ");

        // When
        userService.updateProfile(501L, request);

        // Then — trim 후 두 필드 모두 전달 (companyName 은 UPDATE 포함)
        ArgumentCaptor<UserProfileVO> captor = ArgumentCaptor.forClass(UserProfileVO.class);
        verify(userMapper).updateUserProfile(captor.capture());
        UserProfileVO saved = captor.getValue();
        assertEquals("새닉네임", saved.getNickname());
        assertEquals("구글코리아", saved.getCompanyName());
        assertTrue(saved.isUpdateCompanyName());
    }

    @Test
    @DisplayName("프로필 수정 성공 - companyName 삭제(null/빈 값 전달) → NULL 저장 + update 호출")
    void updateProfile_clearCompanyName() {
        // Given — 프로필 등록 완료 회원 (기존 companyName: 6인조테크)
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(activeUserWithProfile());
        when(userMapper.updateUserProfile(any(UserProfileVO.class))).thenReturn(1);

        // When — nickname 없이 companyName 만 명시적 null(빈 값) 전달 (소속 회사 삭제)
        ProfileUpdateRequestDTO request = new ProfileUpdateRequestDTO();
        request.setCompanyName("");

        userService.updateProfile(501L, request);

        // Then — company_name = NULL 로 저장되도록 updateCompanyName=true + companyName null 전달
        ArgumentCaptor<UserProfileVO> captor = ArgumentCaptor.forClass(UserProfileVO.class);
        verify(userMapper).updateUserProfile(captor.capture());
        UserProfileVO saved = captor.getValue();
        assertEquals(501L, saved.getUserId());
        assertNull(saved.getNickname());
        assertNull(saved.getCompanyName());
        assertTrue(saved.isUpdateCompanyName());
    }

    @Test
    @DisplayName("프로필 수정 성공 - 기존 nickname 과 동일한 값이면 중복 확인 없이 허용")
    void updateProfile_sameNicknameAllowed() {
        // Given — 프로필 등록 완료 회원 (기존 nickname: 지갑대장홍길동)
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(activeUserWithProfile());
        when(userMapper.updateUserProfile(any(UserProfileVO.class))).thenReturn(1);

        ProfileUpdateRequestDTO request = new ProfileUpdateRequestDTO();
        request.setNickname("지갑대장홍길동"); // 기존 nickname 과 동일

        // When
        userService.updateProfile(501L, request);

        // Then — 중복 조회 없이(본인 유지 허용) UPDATE 만 호출
        verify(userMapper, never()).countByNicknameExcludingUserId(any(), any());
        verify(userMapper).updateUserProfile(any(UserProfileVO.class));
    }

    @Test
    @DisplayName("프로필 수정 - UPDATE 영향 row 0 (조회 후 프로필 소실) → PROFILE_NOT_FOUND")
    void updateProfile_updateNoRows() {
        // Given — 프로필 등록 완료 회원 + UPDATE 가 0 row 반환 (이론적 안전장치)
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(activeUserWithProfile());
        when(userMapper.updateUserProfile(any(UserProfileVO.class))).thenReturn(0);

        ProfileUpdateRequestDTO request = new ProfileUpdateRequestDTO();
        request.setNickname("새로운닉네임");

        // When & Then — 조회-수정 사이 프로필 소실로 간주
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.updateProfile(501L, request));
        assertEquals(UserErrorCode.PROFILE_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("프로필 수정 - 사용자 없음 → USER_NOT_FOUND + update 미호출")
    void updateProfile_userNotFound() {
        // Given — 회원 없음
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(null);

        ProfileUpdateRequestDTO request = new ProfileUpdateRequestDTO();
        request.setNickname("새로운닉네임");

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.updateProfile(501L, request));
        assertEquals(UserErrorCode.USER_NOT_FOUND, ex.getErrorCode());

        // Then — 저장 Mapper 미호출
        verify(userMapper, never()).updateUserProfile(any(UserProfileVO.class));
    }

    @Test
    @DisplayName("프로필 수정 - 비활성 회원(탈퇴) → USER_NOT_FOUND + update 미호출")
    void updateProfile_withdrawnUser() {
        // Given — WITHDRAWN 상태 회원
        MyProfileVO withdrawn = activeUserWithProfile();
        withdrawn.setStatus("WITHDRAWN");
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(withdrawn);

        ProfileUpdateRequestDTO request = new ProfileUpdateRequestDTO();
        request.setNickname("새로운닉네임");

        // When & Then — 탈퇴 회원도 USER_NOT_FOUND 로 통일 처리
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.updateProfile(501L, request));
        assertEquals(UserErrorCode.USER_NOT_FOUND, ex.getErrorCode());

        verify(userMapper, never()).updateUserProfile(any(UserProfileVO.class));
    }

    @Test
    @DisplayName("프로필 수정 - 프로필 미등록 사용자 → PROFILE_NOT_FOUND + update 미호출")
    void updateProfile_profileNotFound() {
        // Given — ACTIVE 회원이지만 프로필 미등록 (profileId null)
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(activeUserWithoutProfile());

        ProfileUpdateRequestDTO request = new ProfileUpdateRequestDTO();
        request.setNickname("새로운닉네임");

        // When & Then — 최초 등록 전 사용자는 수정 불가
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.updateProfile(501L, request));
        assertEquals(UserErrorCode.PROFILE_NOT_FOUND, ex.getErrorCode());

        verify(userMapper, never()).updateUserProfile(any(UserProfileVO.class));
    }

    @Test
    @DisplayName("프로필 수정 - nickname 중복(다른 사용자 사용 중) → DUPLICATE_NICKNAME + update 미호출")
    void updateProfile_duplicateNickname() {
        // Given — 프로필 등록 완료 회원 + 변경 닉네임이 다른 사용자에게 사용 중
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(activeUserWithProfile());
        when(userMapper.countByNicknameExcludingUserId("이미쓴닉네임", 501L)).thenReturn(1);

        ProfileUpdateRequestDTO request = new ProfileUpdateRequestDTO();
        request.setNickname("이미쓴닉네임");

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.updateProfile(501L, request));
        assertEquals(UserErrorCode.DUPLICATE_NICKNAME, ex.getErrorCode());

        // Then — 중복 시 UPDATE 미호출
        verify(userMapper, never()).updateUserProfile(any(UserProfileVO.class));
    }

    @Test
    @DisplayName("프로필 수정 - nickname 길이 초과(51자) → INVALID_PROFILE_REQUEST + update 미호출")
    void updateProfile_nicknameTooLong() {
        // Given — 프로필 등록 완료 회원 (사용자/프로필 확인 통과)
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(activeUserWithProfile());

        ProfileUpdateRequestDTO request = new ProfileUpdateRequestDTO();
        request.setNickname(buildString(51, 'a'));

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.updateProfile(501L, request));
        assertEquals(UserErrorCode.INVALID_PROFILE_REQUEST, ex.getErrorCode());

        verify(userMapper, never()).updateUserProfile(any(UserProfileVO.class));
    }

    @Test
    @DisplayName("프로필 수정 - companyName 길이 초과(101자) → INVALID_PROFILE_REQUEST + update 미호출")
    void updateProfile_companyNameTooLong() {
        // Given — 프로필 등록 완료 회원 (사용자/프로필 확인 통과)
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(activeUserWithProfile());

        ProfileUpdateRequestDTO request = new ProfileUpdateRequestDTO();
        request.setCompanyName(buildString(101, 'b'));

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.updateProfile(501L, request));
        assertEquals(UserErrorCode.INVALID_PROFILE_REQUEST, ex.getErrorCode());

        verify(userMapper, never()).updateUserProfile(any(UserProfileVO.class));
    }

    @Test
    @DisplayName("프로필 수정 - 수정 대상 필드 없음(둘 다 null/빈 값) → INVALID_PROFILE_REQUEST + update 미호출")
    void updateProfile_noFields() {
        // Given — 프로필 등록 완료 회원 (사용자/프로필 확인 통과)
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(activeUserWithProfile());

        ProfileUpdateRequestDTO nullRequest = new ProfileUpdateRequestDTO();
        // 공백 nickname 만 전달 (companyName 미전달) — 수정 대상 필드 없음
        ProfileUpdateRequestDTO blankNicknameRequest = new ProfileUpdateRequestDTO();
        blankNicknameRequest.setNickname("   ");

        // When & Then — 모두 INVALID_PROFILE_REQUEST (PATCH: 최소 1개 필드 필요)
        //   ※ companyName 은 null/빈 값 전달도 삭제 요청으로 수정 대상이므로 이 케이스에 포함되지 않는다
        assertThrows(BusinessException.class, () -> userService.updateProfile(501L, null));
        assertThrows(BusinessException.class, () -> userService.updateProfile(501L, nullRequest));
        assertThrows(BusinessException.class, () -> userService.updateProfile(501L, blankNicknameRequest));

        // Then — 저장 Mapper 미호출
        verify(userMapper, never()).updateUserProfile(any(UserProfileVO.class));
    }

    // ---------- 회원 탈퇴 ----------

    /** 탈퇴 요청 DTO 생성 헬퍼 — password 원문은 Service 가 AuthService 로 위임한다 */
    private UserWithdrawalRequestDTO withdrawalRequest(String password) {
        UserWithdrawalRequestDTO request = new UserWithdrawalRequestDTO();
        request.setPassword(password);
        return request;
    }

    /** WITHDRAWN 상태 회원 헬퍼 */
    private MyProfileVO withdrawnUser() {
        MyProfileVO withdrawn = activeUserWithoutProfile();
        withdrawn.setStatus("WITHDRAWN");
        return withdrawn;
    }

    @Test
    @DisplayName("회원 탈퇴 성공 - 비밀번호 검증/잔액 확인 후 status WITHDRAWN UPDATE + Refresh 세션 전체 revoke")
    void withdraw_success() {
        // Given — ACTIVE 회원 + 잔액 0 (탈퇴 가능)
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(activeUserWithoutProfile());
        when(walletService.getBalance(501L)).thenReturn(BigDecimal.ZERO);
        when(userMapper.updateUserStatusToWithdrawn(501L)).thenReturn(1);

        // When — 올바른 비밀번호 재확인
        userService.withdraw(501L, withdrawalRequest("password123!"));

        // Then
        // 1) 현재 비밀번호 검증을 AuthService 로 위임 (BCrypt 검증은 Auth 도메인 책임)
        verify(authService).verifyCurrentPassword(eq(501L), eq("password123!"));
        // 2) 지갑 잔액 조회를 WalletService 로 위임
        verify(walletService).getBalance(501L);
        // 3) users.status = WITHDRAWN + deleted_at 기록 (Soft Delete) — deleted_at 은 SQL(NOW()) 에서 설정
        verify(userMapper).updateUserStatusToWithdrawn(eq(501L));
        // 4) 모든 Refresh Token 세션 revoke (Redis — DB 커밋 확정 후, AuthService 위임)
        verify(authService).revokeAllRefreshSessions(eq(501L));
    }

    @Test
    @DisplayName("회원 탈퇴 성공 - 지갑 잔액 0 이면 탈퇴 허용")
    void withdraw_balanceZero_success() {
        // Given — ACTIVE 회원 + 잔액 0.00
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(activeUserWithoutProfile());
        when(walletService.getBalance(501L)).thenReturn(new BigDecimal("0.00"));
        when(userMapper.updateUserStatusToWithdrawn(501L)).thenReturn(1);

        // When
        userService.withdraw(501L, withdrawalRequest("password123!"));

        // Then — WALLET_BALANCE_REMAINING 없이 정상 탈퇴
        verify(userMapper).updateUserStatusToWithdrawn(501L);
        verify(authService).revokeAllRefreshSessions(501L);
    }

    @Test
    @DisplayName("회원 탈퇴 - 비밀번호 불일치 → AUTH_INVALID_PASSWORD + 상태 변경/세션 revoke 없음")
    void withdraw_wrongPassword() {
        // Given — ACTIVE 회원 (잔액 무관 — 비밀번호 검증이 먼저 수행)
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(activeUserWithoutProfile());
        doThrow(new BusinessException(AuthErrorCode.AUTH_INVALID_PASSWORD))
                .when(authService).verifyCurrentPassword(eq(501L), anyString());

        // When & Then — AuthService 가 비밀번호 불일치를 거부
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.withdraw(501L, withdrawalRequest("wrong-password!")));
        assertEquals(AuthErrorCode.AUTH_INVALID_PASSWORD, ex.getErrorCode());

        // 상태 변경/세션 revoke/잔액 조회가 발생하지 않아야 한다
        verify(userMapper, never()).updateUserStatusToWithdrawn(any());
        verify(authService, never()).revokeAllRefreshSessions(any());
        verify(walletService, never()).getBalance(any());
    }

    @Test
    @DisplayName("회원 탈퇴 - password 누락/빈 값 → COMMON_INVALID_REQUEST (400) + 어떤 처리도 없음")
    void withdraw_missingPassword() {
        // When & Then — null 요청 / null 비밀번호 / 빈 값 모두 검증 실패
        BusinessException nullEx = assertThrows(BusinessException.class,
                () -> userService.withdraw(501L, null));
        assertEquals(CommonErrorCode.COMMON_INVALID_REQUEST, nullEx.getErrorCode());

        BusinessException nullPasswordEx = assertThrows(BusinessException.class,
                () -> userService.withdraw(501L, withdrawalRequest(null)));
        assertEquals(CommonErrorCode.COMMON_INVALID_REQUEST, nullPasswordEx.getErrorCode());

        BusinessException blankEx = assertThrows(BusinessException.class,
                () -> userService.withdraw(501L, withdrawalRequest("   ")));
        assertEquals(CommonErrorCode.COMMON_INVALID_REQUEST, blankEx.getErrorCode());

        // 검증 실패 시 어떤 Mapper/Auth/Wallet 호출도 없어야 한다
        verify(userMapper, never()).selectMyProfileByUserId(any());
        verify(authService, never()).verifyCurrentPassword(any(), anyString());
        verify(walletService, never()).getBalance(any());
        verify(userMapper, never()).updateUserStatusToWithdrawn(any());
        verify(authService, never()).revokeAllRefreshSessions(any());
    }

    @Test
    @DisplayName("회원 탈퇴 - 이미 WITHDRAWN 상태 → USER_ALREADY_WITHDRAWN (409) + 비밀번호 검증/UPDATE 없음")
    void withdraw_alreadyWithdrawn() {
        // Given — WITHDRAWN 상태 회원
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(withdrawnUser());

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.withdraw(501L, withdrawalRequest("password123!")));
        assertEquals(UserErrorCode.USER_ALREADY_WITHDRAWN, ex.getErrorCode());

        // 비밀번호 검증/잔액 조회/상태 변경/세션 revoke 가 발생하지 않아야 한다
        verify(authService, never()).verifyCurrentPassword(any(), anyString());
        verify(walletService, never()).getBalance(any());
        verify(userMapper, never()).updateUserStatusToWithdrawn(any());
        verify(authService, never()).revokeAllRefreshSessions(any());
    }

    @Test
    @DisplayName("회원 탈퇴 - 회원 없음 → USER_NOT_FOUND (404)")
    void withdraw_userNotFound() {
        // Given — Mapper 가 null 반환 (회원 없음)
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(null);

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.withdraw(501L, withdrawalRequest("password123!")));
        assertEquals(UserErrorCode.USER_NOT_FOUND, ex.getErrorCode());

        verify(userMapper, never()).updateUserStatusToWithdrawn(any());
    }

    @Test
    @DisplayName("회원 탈퇴 - 기타 비활성(차단 등) 회원 → USER_NOT_FOUND (계정 존재 여부 비노출)")
    void withdraw_blockedUser() {
        // Given — BLOCKED 상태 회원
        MyProfileVO blocked = activeUserWithoutProfile();
        blocked.setStatus("BLOCKED");
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(blocked);

        // When & Then — 탈퇴 회원과 달리 존재 여부 비노출 정책으로 USER_NOT_FOUND 통일
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.withdraw(501L, withdrawalRequest("password123!")));
        assertEquals(UserErrorCode.USER_NOT_FOUND, ex.getErrorCode());

        verify(userMapper, never()).updateUserStatusToWithdrawn(any());
    }

    @Test
    @DisplayName("회원 탈퇴 - 지갑 잔액 > 0 → WALLET_BALANCE_REMAINING (409) + 상태 변경/세션 revoke 없음")
    void withdraw_balanceRemaining() {
        // Given — ACTIVE 회원 + 잔액 10,000 원 (탈퇴 차단)
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(activeUserWithoutProfile());
        when(walletService.getBalance(501L)).thenReturn(new BigDecimal("10000.00"));

        // When & Then — BigDecimal.compareTo(ZERO) > 0 → 탈퇴 불가
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.withdraw(501L, withdrawalRequest("password123!")));
        assertEquals(WalletErrorCode.WALLET_BALANCE_REMAINING, ex.getErrorCode());

        // 상태 변경/세션 revoke 가 발생하지 않아야 한다 (잔액은 환불/0 원 처리하지 않음)
        verify(userMapper, never()).updateUserStatusToWithdrawn(any());
        verify(authService, never()).revokeAllRefreshSessions(any());
    }

    @Test
    @DisplayName("회원 탈퇴 - 지갑 미존재(잔액 null) → 잔액 0 으로 간주해 탈퇴 허용")
    void withdraw_walletMissing_balanceTreatedAsZero() {
        // Given — ACTIVE 회원 + 지갑 없음 (getBalance null 반환 — WalletService 순수 조회)
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(activeUserWithoutProfile());
        when(walletService.getBalance(501L)).thenReturn(null);
        when(userMapper.updateUserStatusToWithdrawn(501L)).thenReturn(1);

        // When
        userService.withdraw(501L, withdrawalRequest("password123!"));

        // Then — null 잔액은 0 으로 간주해 탈퇴 성공
        verify(userMapper).updateUserStatusToWithdrawn(501L);
        verify(authService).revokeAllRefreshSessions(501L);
    }

    @Test
    @DisplayName("회원 탈퇴 - UPDATE 영향 row 0 (조회-갱신 사이 중복 탈퇴) → USER_ALREADY_WITHDRAWN + 세션 revoke 없음")
    void withdraw_updateNoRows() {
        // Given — ACTIVE 회원 + 잔액 0 + UPDATE 가 0 row 반환 (동시 요청으로 이미 탈퇴 처리된 경우)
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(activeUserWithoutProfile());
        when(walletService.getBalance(501L)).thenReturn(BigDecimal.ZERO);
        when(userMapper.updateUserStatusToWithdrawn(501L)).thenReturn(0);

        // When & Then — Race Condition 최종 방어선
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.withdraw(501L, withdrawalRequest("password123!")));
        assertEquals(UserErrorCode.USER_ALREADY_WITHDRAWN, ex.getErrorCode());

        // 세션이 유지되어야 한다 (탈퇴 실패 — revoke 없음)
        verify(authService, never()).revokeAllRefreshSessions(any());
    }

    @Test
    @DisplayName("회원 탈퇴 - DB 오류 시 예외 전파 + Refresh 세션 revoke 미발생 (Transaction Rollback 경계)")
    void withdraw_dbError_noSessionRevoke() {
        // Given — ACTIVE 회원 + 잔액 0 + 상태 변경 UPDATE 중 DB 오류 발생
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(activeUserWithoutProfile());
        when(walletService.getBalance(501L)).thenReturn(BigDecimal.ZERO);
        when(userMapper.updateUserStatusToWithdrawn(501L))
                .thenThrow(new RuntimeException("DB connection error"));

        // When & Then — 예외가 전파된다 (@Transactional 이 트랜잭션을 롤백 처리)
        assertThrows(RuntimeException.class,
                () -> userService.withdraw(501L, withdrawalRequest("password123!")));

        // DB 변경이 실패했으므로 Redis 세션 revoke 는 수행되지 않아야 한다
        // (Redis 는 DB 커밋 확정 후(afterCommit)에만 폐기 — knowledge.md Redis Transaction 주의)
        verify(authService, never()).revokeAllRefreshSessions(any());
    }

    @Test
    @DisplayName("보안 - UserWithdrawalRequestDTO toString 에 비밀번호 원문 미노출")
    void withdraw_requestToStringHidesSecret() {
        // Given
        UserWithdrawalRequestDTO request = withdrawalRequest("secret-password123!");

        // When
        String text = request.toString();

        // Then
        assertFalse(text.contains("secret-password123!"));
    }

    // ---------- 계정 설정 진입용 비밀번호 재인증 ----------

    /** 재인증 요청 DTO 생성 헬퍼 — password 원문은 Service 가 AuthService 로 위임한다 */
    private AccountPasswordVerifyRequestDTO verifyRequest(String password) {
        AccountPasswordVerifyRequestDTO request = new AccountPasswordVerifyRequestDTO();
        request.setPassword(password);
        return request;
    }

    @Test
    @DisplayName("비밀번호 재인증 성공 - AuthService.verifyCurrentPassword 위임 + 별도 인증 세션/토큰 미생성")
    void verifyAccountPassword_success() {
        // Given — ACTIVE 회원
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(activeUserWithoutProfile());

        // When — 올바른 비밀번호 재확인
        userService.verifyAccountPassword(501L, verifyRequest("password123!"));

        // Then
        // 1) 현재 비밀번호 검증을 AuthService 로 위임 (BCrypt 검증은 Auth 도메인 책임 — 중복 구현 금지)
        verify(authService).verifyCurrentPassword(eq(501L), eq("password123!"));
        // 2) 재인증은 SELECT 만 수행 — DB 쓰기(상태 변경/프로필 저장)가 없어야 한다
        verify(userMapper, never()).updateUserStatusToWithdrawn(any());
        verify(userMapper, never()).insertUserProfile(any(UserProfileVO.class));
        verify(userMapper, never()).updateUserProfile(any(UserProfileVO.class));
        // 3) Redis/세션/토큰 부수 효과 없음 — Refresh 세션 revoke 등 다른 Auth 호출이 없어야 한다
        verify(authService, never()).revokeAllRefreshSessions(any());
    }

    @Test
    @DisplayName("비밀번호 재인증 - 비밀번호 불일치 → AUTH_INVALID_PASSWORD 전파")
    void verifyAccountPassword_wrongPassword() {
        // Given — ACTIVE 회원 + AuthService 가 비밀번호 불일치를 거부
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(activeUserWithoutProfile());
        doThrow(new BusinessException(AuthErrorCode.AUTH_INVALID_PASSWORD))
                .when(authService).verifyCurrentPassword(eq(501L), anyString());

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.verifyAccountPassword(501L, verifyRequest("wrong-password!")));
        assertEquals(AuthErrorCode.AUTH_INVALID_PASSWORD, ex.getErrorCode());
    }

    @Test
    @DisplayName("비밀번호 재인증 - password 누락/빈 값/공백 → COMMON_INVALID_REQUEST + 조회 없음")
    void verifyAccountPassword_missingPassword() {
        // When & Then — null 요청 / null 비밀번호 / 빈 값 / 공백 모두 검증 실패
        BusinessException nullEx = assertThrows(BusinessException.class,
                () -> userService.verifyAccountPassword(501L, null));
        assertEquals(CommonErrorCode.COMMON_INVALID_REQUEST, nullEx.getErrorCode());

        BusinessException nullPasswordEx = assertThrows(BusinessException.class,
                () -> userService.verifyAccountPassword(501L, verifyRequest(null)));
        assertEquals(CommonErrorCode.COMMON_INVALID_REQUEST, nullPasswordEx.getErrorCode());

        BusinessException emptyEx = assertThrows(BusinessException.class,
                () -> userService.verifyAccountPassword(501L, verifyRequest("")));
        assertEquals(CommonErrorCode.COMMON_INVALID_REQUEST, emptyEx.getErrorCode());

        BusinessException blankEx = assertThrows(BusinessException.class,
                () -> userService.verifyAccountPassword(501L, verifyRequest("   ")));
        assertEquals(CommonErrorCode.COMMON_INVALID_REQUEST, blankEx.getErrorCode());

        // 검증 실패 시 어떤 Mapper/Auth 호출도 없어야 한다 (사용자 조회조차 하지 않음)
        verify(userMapper, never()).selectMyProfileByUserId(any());
        verify(authService, never()).verifyCurrentPassword(any(), anyString());
    }

    @Test
    @DisplayName("비밀번호 재인증 - 이미 WITHDRAWN 상태 → USER_ALREADY_WITHDRAWN + 비밀번호 검증 없음")
    void verifyAccountPassword_alreadyWithdrawn() {
        // Given — WITHDRAWN 상태 회원
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(withdrawnUser());

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.verifyAccountPassword(501L, verifyRequest("password123!")));
        assertEquals(UserErrorCode.USER_ALREADY_WITHDRAWN, ex.getErrorCode());

        // 비밀번호 검증이 수행되지 않아야 한다 (탈퇴 회원 재인증 차단)
        verify(authService, never()).verifyCurrentPassword(any(), anyString());
    }

    @Test
    @DisplayName("비밀번호 재인증 - 회원 없음 → USER_NOT_FOUND")
    void verifyAccountPassword_userNotFound() {
        // Given — Mapper 가 null 반환 (회원 없음)
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(null);

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.verifyAccountPassword(501L, verifyRequest("password123!")));
        assertEquals(UserErrorCode.USER_NOT_FOUND, ex.getErrorCode());

        verify(authService, never()).verifyCurrentPassword(any(), anyString());
    }

    @Test
    @DisplayName("비밀번호 재인증 - 기타 비활성(차단 등) 회원 → USER_NOT_FOUND (계정 존재 여부 비노출)")
    void verifyAccountPassword_blockedUser() {
        // Given — BLOCKED 상태 회원
        MyProfileVO blocked = activeUserWithoutProfile();
        blocked.setStatus("BLOCKED");
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(blocked);

        // When & Then — 탈퇴 회원과 달리 존재 여부 비노출 정책으로 USER_NOT_FOUND 통일
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.verifyAccountPassword(501L, verifyRequest("password123!")));
        assertEquals(UserErrorCode.USER_NOT_FOUND, ex.getErrorCode());

        verify(authService, never()).verifyCurrentPassword(any(), anyString());
    }

    @Test
    @DisplayName("보안 - AccountPasswordVerifyRequestDTO toString 에 비밀번호 원문 미노출")
    void verifyAccountPassword_requestToStringHidesSecret() {
        // Given
        AccountPasswordVerifyRequestDTO request = verifyRequest("secret-password123!");

        // When
        String text = request.toString();

        // Then
        assertFalse(text.contains("secret-password123!"));
    }

    // ---------- 휴대폰 번호 변경 ----------

    /** 휴대폰 번호 변경 요청 DTO 생성 헬퍼 — identityVerificationId 만 전달 (phoneNumber 는 Request 에 없음) */
    private PhoneChangeRequestDTO phoneChangeRequest(String identityVerificationId) {
        PhoneChangeRequestDTO request = new PhoneChangeRequestDTO();
        request.setIdentityVerificationId(identityVerificationId);
        return request;
    }

    /** PASS 인증 결과 헬퍼 — Mock PASS 세션에서 복원된 인증 정보 (name 은 현재 사용자와 동일해야 본인 확인 통과) */
    private IdentityVerificationResult verifiedResult(String name, String phoneNumber) {
        return IdentityVerificationResult.builder()
                .ci("MOCK-CI-" + sha256Hex(phoneNumber))
                .name(name)
                .phoneNumber(phoneNumber)
                .build();
    }

    @Test
    @DisplayName("휴대폰 번호 변경 성공 - PASS 인증 결과 번호로 users UPDATE + 변경된 번호 반환")
    void changePhone_success() {
        // Given — ACTIVE 회원 (현재 번호: 01012345678) + PASS 인증 결과가 새 번호(01098765432) 반환
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(activeUserWithoutProfile());
        when(identityVerificationProvider.verify("identity-verification-id"))
                .thenReturn(verifiedResult(NAME, NEW_PHONE_NUMBER));
        when(userMapper.countByPhoneHashExcludingUserId(sha256Hex(NEW_PHONE_NUMBER), 501L)).thenReturn(0);
        when(userMapper.updateUserPhoneNumber(eq(501L), anyString(), anyString())).thenReturn(1);

        // When
        PhoneChangeResponseDTO result = userService.changePhone(501L, phoneChangeRequest("identity-verification-id"));

        // Then — 변경된 번호 응답 (PASS 인증 결과에서 조회한 값 — 프론트 전달 번호 아님)
        assertEquals(NEW_PHONE_NUMBER, result.getUpdatedPhone());

        // PASS 인증 결과 검증을 Provider 로 위임
        verify(identityVerificationProvider).verify("identity-verification-id");
        // 다른 사용자 중복 조회는 SHA-256 hash 로만 수행 (원문 조회 금지)
        verify(userMapper).countByPhoneHashExcludingUserId(sha256Hex(NEW_PHONE_NUMBER), 501L);

        // users UPDATE — hash(SHA-256) + encrypt(AES-256) 모두 Service Layer 에서 생성해 전달
        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> encryptCaptor = ArgumentCaptor.forClass(String.class);
        verify(userMapper).updateUserPhoneNumber(eq(501L), hashCaptor.capture(), encryptCaptor.capture());
        assertEquals(sha256Hex(NEW_PHONE_NUMBER), hashCaptor.getValue());
        // AES-GCM 은 매 호출 새 IV — 복호화로 원문 확인 (암호문 동일성 비교 금지)
        assertEquals(NEW_PHONE_NUMBER, PersonalDataCipher.decrypt(encryptCaptor.getValue()));
    }

    @Test
    @DisplayName("휴대폰 번호 변경 - identityVerificationId 누락/빈 값/공백 → INVALID_VERIFICATION_ID + 어떤 처리도 없음")
    void changePhone_missingIdentityVerificationId() {
        // When & Then — null 요청 / null ID / 빈 값 / 공백 모두 검증 실패
        BusinessException nullEx = assertThrows(BusinessException.class,
                () -> userService.changePhone(501L, null));
        assertEquals(AuthErrorCode.INVALID_VERIFICATION_ID, nullEx.getErrorCode());

        BusinessException nullIdEx = assertThrows(BusinessException.class,
                () -> userService.changePhone(501L, phoneChangeRequest(null)));
        assertEquals(AuthErrorCode.INVALID_VERIFICATION_ID, nullIdEx.getErrorCode());

        BusinessException emptyEx = assertThrows(BusinessException.class,
                () -> userService.changePhone(501L, phoneChangeRequest("")));
        assertEquals(AuthErrorCode.INVALID_VERIFICATION_ID, emptyEx.getErrorCode());

        BusinessException blankEx = assertThrows(BusinessException.class,
                () -> userService.changePhone(501L, phoneChangeRequest("   ")));
        assertEquals(AuthErrorCode.INVALID_VERIFICATION_ID, blankEx.getErrorCode());

        // 검증 실패 시 어떤 Mapper/Provider 호출도 없어야 한다 (사용자 조회조차 하지 않음)
        verify(userMapper, never()).selectMyProfileByUserId(any());
        verify(identityVerificationProvider, never()).verify(anyString());
        verify(userMapper, never()).updateUserPhoneNumber(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("휴대폰 번호 변경 - 존재하지 않는/만료된/VERIFIED 아닌 identityVerificationId → INVALID_VERIFICATION_ID 전파")
    void changePhone_invalidSession() {
        // Given — ACTIVE 회원 + Provider 가 세션 없음(만료/미인증)을 거부한다
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(activeUserWithoutProfile());
        when(identityVerificationProvider.verify("invalid-id"))
                .thenThrow(new BusinessException(AuthErrorCode.INVALID_VERIFICATION_ID));

        // When & Then — 세션 없음/TTL 만료/status != VERIFIED/used == true 모두 동일 에러 (Provider 책임)
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.changePhone(501L, phoneChangeRequest("invalid-id")));
        assertEquals(AuthErrorCode.INVALID_VERIFICATION_ID, ex.getErrorCode());

        // 인증 실패 시 번호 변경 UPDATE 가 발생하지 않아야 한다
        verify(userMapper, never()).updateUserPhoneNumber(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("휴대폰 번호 변경 - 다른 사용자에게 발급된 identityVerificationId(이름 불일치) → VERIFICATION_FAILED")
    void changePhone_nameMismatch() {
        // Given — ACTIVE 회원(이름: 홍길동) + PASS 인증 결과가 다른 사람 이름을 반환
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(activeUserWithoutProfile());
        when(identityVerificationProvider.verify("other-user-id"))
                .thenReturn(verifiedResult("김철수", NEW_PHONE_NUMBER));

        // When & Then — 본인 인증 실패 (다른 사용자에게 발급된 인증 ID 차단)
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.changePhone(501L, phoneChangeRequest("other-user-id")));
        assertEquals(AuthErrorCode.VERIFICATION_FAILED, ex.getErrorCode());

        verify(userMapper, never()).updateUserPhoneNumber(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("휴대폰 번호 변경 - 인증된 번호가 현재 번호와 동일 → PHONE_SAME_AS_CURRENT")
    void changePhone_samePhone() {
        // Given — ACTIVE 회원(현재 번호: 01012345678) + PASS 인증 결과가 동일 번호를 반환
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(activeUserWithoutProfile());
        when(identityVerificationProvider.verify("identity-verification-id"))
                .thenReturn(verifiedResult(NAME, PHONE_NUMBER));

        // When & Then — 동일 번호로 변경 불가 (비밀번호/PIN 변경 동일 패턴)
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.changePhone(501L, phoneChangeRequest("identity-verification-id")));
        assertEquals(UserErrorCode.PHONE_SAME_AS_CURRENT, ex.getErrorCode());

        // 중복 조회/UPDATE 가 발생하지 않아야 한다
        verify(userMapper, never()).countByPhoneHashExcludingUserId(anyString(), any());
        verify(userMapper, never()).updateUserPhoneNumber(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("휴대폰 번호 변경 - 다른 사용자가 사용 중인 번호 → PHONE_ALREADY_IN_USE + update 미호출")
    void changePhone_phoneAlreadyInUse() {
        // Given — ACTIVE 회원 + PASS 인증 성공 + 새 번호가 다른 사용자에게 등록됨 (hash 기준)
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(activeUserWithoutProfile());
        when(identityVerificationProvider.verify("identity-verification-id"))
                .thenReturn(verifiedResult(NAME, NEW_PHONE_NUMBER));
        when(userMapper.countByPhoneHashExcludingUserId(sha256Hex(NEW_PHONE_NUMBER), 501L)).thenReturn(1);

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.changePhone(501L, phoneChangeRequest("identity-verification-id")));
        assertEquals(UserErrorCode.PHONE_ALREADY_IN_USE, ex.getErrorCode());

        // 중복 시 UPDATE 미호출
        verify(userMapper, never()).updateUserPhoneNumber(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("휴대폰 번호 변경 - 이미 탈퇴한 사용자 → USER_ALREADY_WITHDRAWN + PASS 검증 없음")
    void changePhone_alreadyWithdrawn() {
        // Given — WITHDRAWN 상태 회원
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(withdrawnUser());

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.changePhone(501L, phoneChangeRequest("identity-verification-id")));
        assertEquals(UserErrorCode.USER_ALREADY_WITHDRAWN, ex.getErrorCode());

        // 사용자 상태 확인이 PASS 검증보다 먼저 수행되므로 Provider 호출이 없어야 한다
        verify(identityVerificationProvider, never()).verify(anyString());
        verify(userMapper, never()).updateUserPhoneNumber(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("휴대폰 번호 변경 - 회원 없음 → USER_NOT_FOUND")
    void changePhone_userNotFound() {
        // Given — Mapper 가 null 반환 (회원 없음)
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(null);

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.changePhone(501L, phoneChangeRequest("identity-verification-id")));
        assertEquals(UserErrorCode.USER_NOT_FOUND, ex.getErrorCode());

        verify(userMapper, never()).updateUserPhoneNumber(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("휴대폰 번호 변경 - 기타 비활성(차단 등) 회원 → USER_NOT_FOUND (계정 존재 여부 비노출)")
    void changePhone_blockedUser() {
        // Given — BLOCKED 상태 회원
        MyProfileVO blocked = activeUserWithoutProfile();
        blocked.setStatus("BLOCKED");
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(blocked);

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.changePhone(501L, phoneChangeRequest("identity-verification-id")));
        assertEquals(UserErrorCode.USER_NOT_FOUND, ex.getErrorCode());

        verify(userMapper, never()).updateUserPhoneNumber(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("휴대폰 번호 변경 - UPDATE 영향 row 0 (조회-갱신 사이 동시 탈퇴) → USER_ALREADY_WITHDRAWN")
    void changePhone_updateNoRows() {
        // Given — ACTIVE 회원 + PASS 인증 성공 + 중복 없음 + UPDATE 가 0 row 반환 (동시 탈퇴)
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(activeUserWithoutProfile());
        when(identityVerificationProvider.verify("identity-verification-id"))
                .thenReturn(verifiedResult(NAME, NEW_PHONE_NUMBER));
        when(userMapper.countByPhoneHashExcludingUserId(sha256Hex(NEW_PHONE_NUMBER), 501L)).thenReturn(0);
        when(userMapper.updateUserPhoneNumber(eq(501L), anyString(), anyString())).thenReturn(0);

        // When & Then — Race Condition 최종 방어선 (WHERE status != 'WITHDRAWN')
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.changePhone(501L, phoneChangeRequest("identity-verification-id")));
        assertEquals(UserErrorCode.USER_ALREADY_WITHDRAWN, ex.getErrorCode());
    }

    @Test
    @DisplayName("휴대폰 번호 변경 - phone_number_hash UNIQUE 충돌(DuplicateKeyException) → PHONE_ALREADY_IN_USE")
    void changePhone_duplicateKey() {
        // Given — ACTIVE 회원 + PASS 인증 성공 + UPDATE 중 동시 요청으로 UNIQUE 충돌
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(activeUserWithoutProfile());
        when(identityVerificationProvider.verify("identity-verification-id"))
                .thenReturn(verifiedResult(NAME, NEW_PHONE_NUMBER));
        when(userMapper.countByPhoneHashExcludingUserId(sha256Hex(NEW_PHONE_NUMBER), 501L)).thenReturn(0);
        when(userMapper.updateUserPhoneNumber(eq(501L), anyString(), anyString()))
                .thenThrow(new DuplicateKeyException("users.phone_number_hash UNIQUE constraint violated"));

        // When & Then — Race Condition 방어선: 500 대신 409
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.changePhone(501L, phoneChangeRequest("identity-verification-id")));
        assertEquals(UserErrorCode.PHONE_ALREADY_IN_USE, ex.getErrorCode());
    }

    @Test
    @DisplayName("보안 - PhoneChangeRequestDTO toString 에 identityVerificationId 미노출")
    void changePhone_requestToStringHidesSecret() {
        // Given
        PhoneChangeRequestDTO request = phoneChangeRequest("secret-identity-verification-id");

        // When
        String text = request.toString();

        // Then
        assertFalse(text.contains("secret-identity-verification-id"));
    }

    // ---------- 이메일 인증번호 발송 ----------

    /** 이메일 인증번호 발송 요청 DTO 생성 헬퍼 — email 만 전달 */
    private EmailVerificationRequestDTO emailVerificationRequest(String email) {
        EmailVerificationRequestDTO request = new EmailVerificationRequestDTO();
        request.setEmail(email);
        return request;
    }

    @Test
    @DisplayName("이메일 인증번호 발송 성공 - 이메일 정규화 후 Mock 발송 서비스 위임 + DB 저장 없음")
    void sendEmailVerification_success() {
        // Given — ACTIVE 회원 (현재 이메일: user@example.com) + 새 이메일 (대소문자/공백 정규화 대상)
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(activeUserWithoutProfile());

        // When — 대소문자/공백이 섞인 이메일은 lowercase + trim 정규화되어 발송된다
        EmailVerificationResponseDTO result =
                userService.sendEmailVerification(501L, emailVerificationRequest("  New@Example.com  "));

        // Then — EmailValidator 공통 정책으로 정규화된 값이 Mock 발송 서비스로 전달된다
        verify(emailVerificationService).issueVerificationCode(501L, "new@example.com");
        // 응답은 인증번호를 발송한(정규화된) 이메일 (docs 응답 data.email)
        assertEquals("new@example.com", result.getEmail());
        // 인증번호는 DB 에 저장하지 않는다 (Mock 임시 저장소 사용 — docs)
        verify(userMapper, never()).updateUserPhoneNumber(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("이메일 인증번호 발송 - email 누락(null/빈 값/공백) → INVALID_EMAIL_REQUEST + 발송 없음")
    void sendEmailVerification_missingEmail() {
        // Given — ACTIVE 회원 (사용자 확인 통과 — docs 처리 로직 순서)
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(activeUserWithoutProfile());

        // When & Then — null 요청 / null 이메일 / 빈 값 / 공백 모두 검증 실패
        BusinessException nullEx = assertThrows(BusinessException.class,
                () -> userService.sendEmailVerification(501L, null));
        assertEquals(UserErrorCode.INVALID_EMAIL_REQUEST, nullEx.getErrorCode());

        BusinessException nullEmailEx = assertThrows(BusinessException.class,
                () -> userService.sendEmailVerification(501L, emailVerificationRequest(null)));
        assertEquals(UserErrorCode.INVALID_EMAIL_REQUEST, nullEmailEx.getErrorCode());

        BusinessException emptyEx = assertThrows(BusinessException.class,
                () -> userService.sendEmailVerification(501L, emailVerificationRequest("")));
        assertEquals(UserErrorCode.INVALID_EMAIL_REQUEST, emptyEx.getErrorCode());

        BusinessException blankEx = assertThrows(BusinessException.class,
                () -> userService.sendEmailVerification(501L, emailVerificationRequest("   ")));
        assertEquals(UserErrorCode.INVALID_EMAIL_REQUEST, blankEx.getErrorCode());

        // 검증 실패 시 Mock 발송 서비스가 호출되지 않아야 한다
        verify(emailVerificationService, never()).issueVerificationCode(any(), anyString());
    }

    @Test
    @DisplayName("이메일 인증번호 발송 - 잘못된 이메일 형식 → INVALID_EMAIL_REQUEST + 발송 없음")
    void sendEmailVerification_invalidFormat() {
        // Given — ACTIVE 회원 (사용자 확인 통과)
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(activeUserWithoutProfile());

        // When & Then — '@' 없음 / 도메인 없음 / 공백 포함 등 형식 오류
        BusinessException noAtEx = assertThrows(BusinessException.class,
                () -> userService.sendEmailVerification(501L, emailVerificationRequest("new.example.com")));
        assertEquals(UserErrorCode.INVALID_EMAIL_REQUEST, noAtEx.getErrorCode());

        BusinessException noDomainEx = assertThrows(BusinessException.class,
                () -> userService.sendEmailVerification(501L, emailVerificationRequest("new@example")));
        assertEquals(UserErrorCode.INVALID_EMAIL_REQUEST, noDomainEx.getErrorCode());

        // 검증 실패 시 Mock 발송 서비스가 호출되지 않아야 한다
        verify(emailVerificationService, never()).issueVerificationCode(any(), anyString());
    }

    @Test
    @DisplayName("이메일 인증번호 발송 - 현재 이메일과 동일한 이메일 → EMAIL_SAME_AS_CURRENT + 발송 없음")
    void sendEmailVerification_sameAsCurrent() {
        // Given — ACTIVE 회원 (현재 이메일: user@example.com)
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(activeUserWithoutProfile());

        // When & Then — 현재 이메일(정규화 결과 동일)로는 발송 불가
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.sendEmailVerification(501L, emailVerificationRequest("USER@Example.com")));
        assertEquals(UserErrorCode.EMAIL_SAME_AS_CURRENT, ex.getErrorCode());

        // 중복 조회/발송이 발생하지 않아야 한다
        verify(userMapper, never()).countByEmailHashExcludingUserId(anyString(), any());
        verify(emailVerificationService, never()).issueVerificationCode(any(), anyString());
    }

    @Test
    @DisplayName("이메일 인증번호 발송 - 다른 사용자가 이미 사용 중인 이메일 → EMAIL_ALREADY_IN_USE + 발송 없음")
    void sendEmailVerification_alreadyInUse() {
        // Given — ACTIVE 회원 + 새 이메일이 다른 사용자에게 등록됨 (hash 기준)
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(activeUserWithoutProfile());
        when(userMapper.countByEmailHashExcludingUserId(sha256Hex("new@example.com"), 501L)).thenReturn(1);

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.sendEmailVerification(501L, emailVerificationRequest("new@example.com")));
        assertEquals(UserErrorCode.EMAIL_ALREADY_IN_USE, ex.getErrorCode());

        // 중복 시 Mock 발송 서비스가 호출되지 않아야 한다
        verify(emailVerificationService, never()).issueVerificationCode(any(), anyString());
    }

    @Test
    @DisplayName("이메일 인증번호 발송 - 이미 탈퇴한 사용자 → USER_ALREADY_WITHDRAWN + 발송 없음")
    void sendEmailVerification_alreadyWithdrawn() {
        // Given — WITHDRAWN 상태 회원
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(withdrawnUser());

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.sendEmailVerification(501L, emailVerificationRequest("new@example.com")));
        assertEquals(UserErrorCode.USER_ALREADY_WITHDRAWN, ex.getErrorCode());

        // 탈퇴 회원은 이메일 검증/발송이 수행되지 않아야 한다
        verify(emailVerificationService, never()).issueVerificationCode(any(), anyString());
    }

    @Test
    @DisplayName("이메일 인증번호 발송 - 회원 없음 → USER_NOT_FOUND")
    void sendEmailVerification_userNotFound() {
        // Given — Mapper 가 null 반환 (회원 없음)
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(null);

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.sendEmailVerification(501L, emailVerificationRequest("new@example.com")));
        assertEquals(UserErrorCode.USER_NOT_FOUND, ex.getErrorCode());

        verify(emailVerificationService, never()).issueVerificationCode(any(), anyString());
    }

    @Test
    @DisplayName("이메일 인증번호 발송 - 기타 비활성(차단 등) 회원 → USER_NOT_FOUND (계정 존재 여부 비노출)")
    void sendEmailVerification_blockedUser() {
        // Given — BLOCKED 상태 회원
        MyProfileVO blocked = activeUserWithoutProfile();
        blocked.setStatus("BLOCKED");
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(blocked);

        // When & Then — 탈퇴 회원과 달리 존재 여부 비노출 정책으로 USER_NOT_FOUND 통일
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.sendEmailVerification(501L, emailVerificationRequest("new@example.com")));
        assertEquals(UserErrorCode.USER_NOT_FOUND, ex.getErrorCode());

        verify(emailVerificationService, never()).issueVerificationCode(any(), anyString());
    }

    @Test
    @DisplayName("보안 - EmailVerificationRequestDTO toString 에 이메일 원문 미노출")
    void sendEmailVerification_requestToStringHidesSecret() {
        // Given
        EmailVerificationRequestDTO request = emailVerificationRequest("secret@example.com");

        // When
        String text = request.toString();

        // Then — 이메일은 개인정보이므로 로그/toString 노출 금지 (knowledge.md)
        assertFalse(text.contains("secret@example.com"));
    }

    @Test
    @DisplayName("이메일 인증번호 발송 - 같은 이메일 재발송 허용 (Mock 발송 서비스 2회 위임 — 기존 인증번호 무효화는 저장소 책임)")
    void sendEmailVerification_resend() {
        // Given — ACTIVE 회원
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(activeUserWithoutProfile());

        // When — 같은 이메일로 두 번 발송 요청
        userService.sendEmailVerification(501L, emailVerificationRequest("new@example.com"));
        userService.sendEmailVerification(501L, emailVerificationRequest("new@example.com"));

        // Then — 두 번 모두 발송 처리된다 (같은 이메일의 기존 인증번호 폐기/재발급은
        //   Mock 발송 서비스의 저장소가 같은 key 에 덮어써 처리한다 — docs)
        verify(emailVerificationService, times(2)).issueVerificationCode(501L, "new@example.com");
    }

    // ---------- 이메일 인증번호 확인 ----------

    /** 이메일 인증번호 확인 요청 DTO 생성 헬퍼 — email/verificationCode 전달 */
    private EmailVerificationConfirmRequestDTO emailVerificationConfirmRequest(String email, String code) {
        EmailVerificationConfirmRequestDTO request = new EmailVerificationConfirmRequestDTO();
        request.setEmail(email);
        request.setVerificationCode(code);
        return request;
    }

    @Test
    @DisplayName("이메일 인증번호 확인 성공 - 이메일 정규화 후 Mock 인증 서비스 위임 + 인증 완료 여부(true) 반환")
    void confirmEmailVerification_success() {
        // Given — ACTIVE 회원 (사용자 확인 통과)
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(activeUserWithoutProfile());

        // When — 대소문자/공백이 섞인 이메일도 정규화되어 확인된다
        EmailVerificationConfirmResponseDTO result = userService.confirmEmailVerification(
                501L, emailVerificationConfirmRequest("  New@Example.com  ", "123456"));

        // Then — 정규화된 이메일과 입력 인증번호가 Mock 인증 서비스로 전달된다
        verify(emailVerificationService).confirmVerificationCode(501L, "new@example.com", "123456");
        // 응답은 인증 완료 여부 (docs 응답 data.verified — 성공 시 true)
        assertTrue(result.isVerified());
    }

    @Test
    @DisplayName("이메일 인증번호 확인 - email 누락(null/빈 값/공백) → INVALID_EMAIL_REQUEST + 확인 없음")
    void confirmEmailVerification_missingEmail() {
        // Given — ACTIVE 회원 (사용자 확인 통과 — docs 처리 로직 순서)
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(activeUserWithoutProfile());

        // When & Then — null 요청 / null 이메일 / 빈 값 / 공백 모두 검증 실패
        BusinessException nullEx = assertThrows(BusinessException.class,
                () -> userService.confirmEmailVerification(501L, null));
        assertEquals(UserErrorCode.INVALID_EMAIL_REQUEST, nullEx.getErrorCode());

        BusinessException nullEmailEx = assertThrows(BusinessException.class,
                () -> userService.confirmEmailVerification(501L,
                        emailVerificationConfirmRequest(null, "123456")));
        assertEquals(UserErrorCode.INVALID_EMAIL_REQUEST, nullEmailEx.getErrorCode());

        BusinessException emptyEx = assertThrows(BusinessException.class,
                () -> userService.confirmEmailVerification(501L,
                        emailVerificationConfirmRequest("", "123456")));
        assertEquals(UserErrorCode.INVALID_EMAIL_REQUEST, emptyEx.getErrorCode());

        BusinessException blankEx = assertThrows(BusinessException.class,
                () -> userService.confirmEmailVerification(501L,
                        emailVerificationConfirmRequest("   ", "123456")));
        assertEquals(UserErrorCode.INVALID_EMAIL_REQUEST, blankEx.getErrorCode());

        // 검증 실패 시 Mock 인증 서비스가 호출되지 않아야 한다
        verify(emailVerificationService, never()).confirmVerificationCode(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("이메일 인증번호 확인 - verificationCode 누락(null/빈 값/공백) → EMAIL_VERIFICATION_CODE_INVALID + 확인 없음")
    void confirmEmailVerification_missingCode() {
        // Given — ACTIVE 회원 (사용자 확인 통과)
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(activeUserWithoutProfile());

        // When & Then — null / 빈 값 / 공백 인증번호 모두 검증 실패
        BusinessException nullCodeEx = assertThrows(BusinessException.class,
                () -> userService.confirmEmailVerification(501L,
                        emailVerificationConfirmRequest("new@example.com", null)));
        assertEquals(UserErrorCode.EMAIL_VERIFICATION_CODE_INVALID, nullCodeEx.getErrorCode());

        BusinessException emptyCodeEx = assertThrows(BusinessException.class,
                () -> userService.confirmEmailVerification(501L,
                        emailVerificationConfirmRequest("new@example.com", "")));
        assertEquals(UserErrorCode.EMAIL_VERIFICATION_CODE_INVALID, emptyCodeEx.getErrorCode());

        BusinessException blankCodeEx = assertThrows(BusinessException.class,
                () -> userService.confirmEmailVerification(501L,
                        emailVerificationConfirmRequest("new@example.com", "   ")));
        assertEquals(UserErrorCode.EMAIL_VERIFICATION_CODE_INVALID, blankCodeEx.getErrorCode());

        // 검증 실패 시 Mock 인증 서비스가 호출되지 않아야 한다
        verify(emailVerificationService, never()).confirmVerificationCode(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("이메일 인증번호 확인 - 잘못된 이메일 형식 → INVALID_EMAIL_REQUEST + 확인 없음")
    void confirmEmailVerification_invalidFormat() {
        // Given — ACTIVE 회원 (사용자 확인 통과)
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(activeUserWithoutProfile());

        // When & Then — '@' 없음 등 형식 오류
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.confirmEmailVerification(501L,
                        emailVerificationConfirmRequest("new.example.com", "123456")));
        assertEquals(UserErrorCode.INVALID_EMAIL_REQUEST, ex.getErrorCode());

        verify(emailVerificationService, never()).confirmVerificationCode(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("이메일 인증번호 확인 - 이미 탈퇴한 사용자 → USER_ALREADY_WITHDRAWN + 확인 없음")
    void confirmEmailVerification_alreadyWithdrawn() {
        // Given — WITHDRAWN 상태 회원
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(withdrawnUser());

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.confirmEmailVerification(501L,
                        emailVerificationConfirmRequest("new@example.com", "123456")));
        assertEquals(UserErrorCode.USER_ALREADY_WITHDRAWN, ex.getErrorCode());

        verify(emailVerificationService, never()).confirmVerificationCode(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("이메일 인증번호 확인 - 회원 없음 → USER_NOT_FOUND")
    void confirmEmailVerification_userNotFound() {
        // Given — Mapper 가 null 반환 (회원 없음)
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(null);

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.confirmEmailVerification(501L,
                        emailVerificationConfirmRequest("new@example.com", "123456")));
        assertEquals(UserErrorCode.USER_NOT_FOUND, ex.getErrorCode());

        verify(emailVerificationService, never()).confirmVerificationCode(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("이메일 인증번호 확인 - 기타 비활성(차단 등) 회원 → USER_NOT_FOUND (계정 존재 여부 비노출)")
    void confirmEmailVerification_blockedUser() {
        // Given — BLOCKED 상태 회원
        MyProfileVO blocked = activeUserWithoutProfile();
        blocked.setStatus("BLOCKED");
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(blocked);

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.confirmEmailVerification(501L,
                        emailVerificationConfirmRequest("new@example.com", "123456")));
        assertEquals(UserErrorCode.USER_NOT_FOUND, ex.getErrorCode());

        verify(emailVerificationService, never()).confirmVerificationCode(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("보안 - EmailVerificationConfirmRequestDTO toString 에 이메일/인증번호 원문 미노출")
    void confirmEmailVerification_requestToStringHidesSecret() {
        // Given
        EmailVerificationConfirmRequestDTO request =
                emailVerificationConfirmRequest("secret@example.com", "123456");

        // When
        String text = request.toString();

        // Then — 이메일(개인정보)/인증번호(1회성 인증값)는 로그/toString 노출 금지 (knowledge.md)
        assertFalse(text.contains("secret@example.com"));
        assertFalse(text.contains("123456"));
    }

    // ---------- 이메일 변경 ----------

    @Test
    @DisplayName("이메일 변경 성공 - 인증 완료된 이메일로 users UPDATE + 인증 세션 소비 + updatedEmail 반환")
    void changeEmail_success() {
        // Given — ACTIVE 회원 (현재 이메일: user@example.com) + 인증 완료된 새 이메일
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(activeUserWithoutProfile());
        when(emailVerificationService.getVerifiedEmail(501L)).thenReturn(NEW_EMAIL);
        when(userMapper.countByEmailHashExcludingUserId(sha256Hex(NEW_EMAIL), 501L)).thenReturn(0);
        when(userMapper.updateUserEmail(eq(501L), anyString(), anyString())).thenReturn(1);

        // When
        EmailChangeResponseDTO result = userService.changeEmail(501L);

        // Then — 변경된 이메일 응답 (인증 세션에서 조회한 값 — 프론트 전달 값 아님)
        assertEquals(NEW_EMAIL, result.getUpdatedEmail());

        // 인증 완료된 이메일 조회를 EmailVerificationService 로 위임 (Request Body 없음 — docs)
        verify(emailVerificationService).getVerifiedEmail(501L);
        // 다른 사용자 중복 조회는 SHA-256 hash 로만 수행 (원문 조회 금지)
        verify(userMapper).countByEmailHashExcludingUserId(sha256Hex(NEW_EMAIL), 501L);

        // users UPDATE — hash(SHA-256) + encrypt(AES-256) 모두 Service Layer 에서 생성해 전달
        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> encryptCaptor = ArgumentCaptor.forClass(String.class);
        verify(userMapper).updateUserEmail(eq(501L), hashCaptor.capture(), encryptCaptor.capture());
        assertEquals(sha256Hex(NEW_EMAIL), hashCaptor.getValue());
        // AES-GCM 은 매 호출 새 IV — 복호화로 원문 확인 (암호문 동일성 비교 금지)
        assertEquals(NEW_EMAIL, PersonalDataCipher.decrypt(encryptCaptor.getValue()));

        // 변경 성공 후 인증 세션 소비 (동일 인증 결과 재사용 방지 — docs)
        verify(emailVerificationService).consumeVerification(501L);
    }

    @Test
    @DisplayName("이메일 변경 - 인증 미완료/인증정보 없음/만료 → EMAIL_VERIFICATION_REQUIRED + UPDATE 미호출")
    void changeEmail_verificationRequired() {
        // Given — ACTIVE 회원 + 인증 세션 검증 실패 (EmailVerificationService 가 거부)
        //   - 인증 미완료(verified=false) / 인증정보 없음 / 인증정보 만료 는 모두 동일 에러로 수렴된다
        //     (세부 원인별 검증은 MockEmailVerificationServiceImplTest 에서 수행)
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(activeUserWithoutProfile());
        when(emailVerificationService.getVerifiedEmail(501L))
                .thenThrow(new BusinessException(UserErrorCode.EMAIL_VERIFICATION_REQUIRED));

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.changeEmail(501L));
        assertEquals(UserErrorCode.EMAIL_VERIFICATION_REQUIRED, ex.getErrorCode());

        // 인증 실패 시 중복 조회/UPDATE/소비가 발생하지 않아야 한다
        verify(userMapper, never()).countByEmailHashExcludingUserId(anyString(), any());
        verify(userMapper, never()).updateUserEmail(any(), anyString(), anyString());
        verify(emailVerificationService, never()).consumeVerification(any());
    }

    @Test
    @DisplayName("이메일 변경 - 인증된 이메일이 현재 이메일과 동일 → EMAIL_SAME_AS_CURRENT + UPDATE 미호출")
    void changeEmail_sameAsCurrent() {
        // Given — ACTIVE 회원 (현재 이메일: user@example.com) + 인증 완료된 이메일도 동일 값
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(activeUserWithoutProfile());
        when(emailVerificationService.getVerifiedEmail(501L)).thenReturn(EMAIL);

        // When & Then — 변경할 이메일이 없음 (docs: 현재 이메일과 동일 → 오류 처리)
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.changeEmail(501L));
        assertEquals(UserErrorCode.EMAIL_SAME_AS_CURRENT, ex.getErrorCode());

        // 중복 조회/UPDATE/소비가 발생하지 않아야 한다
        verify(userMapper, never()).countByEmailHashExcludingUserId(anyString(), any());
        verify(userMapper, never()).updateUserEmail(any(), anyString(), anyString());
        verify(emailVerificationService, never()).consumeVerification(any());
    }

    @Test
    @DisplayName("이메일 변경 - 다른 사용자가 사용 중인 이메일 → EMAIL_ALREADY_IN_USE + UPDATE 미호출")
    void changeEmail_alreadyInUse() {
        // Given — ACTIVE 회원 + 인증 성공 + 새 이메일이 다른 사용자에게 등록됨 (hash 기준)
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(activeUserWithoutProfile());
        when(emailVerificationService.getVerifiedEmail(501L)).thenReturn(NEW_EMAIL);
        when(userMapper.countByEmailHashExcludingUserId(sha256Hex(NEW_EMAIL), 501L)).thenReturn(1);

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.changeEmail(501L));
        assertEquals(UserErrorCode.EMAIL_ALREADY_IN_USE, ex.getErrorCode());

        // 중복 시 UPDATE/소비가 발생하지 않아야 한다
        verify(userMapper, never()).updateUserEmail(any(), anyString(), anyString());
        verify(emailVerificationService, never()).consumeVerification(any());
    }

    @Test
    @DisplayName("이메일 변경 - 이미 탈퇴한 사용자 → USER_ALREADY_WITHDRAWN + 인증 조회 없음")
    void changeEmail_alreadyWithdrawn() {
        // Given — WITHDRAWN 상태 회원
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(withdrawnUser());

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.changeEmail(501L));
        assertEquals(UserErrorCode.USER_ALREADY_WITHDRAWN, ex.getErrorCode());

        // 사용자 상태 확인이 인증 조회보다 먼저 수행되므로 인증 서비스 호출이 없어야 한다
        verify(emailVerificationService, never()).getVerifiedEmail(any());
        verify(userMapper, never()).updateUserEmail(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("이메일 변경 - 회원 없음 → USER_NOT_FOUND")
    void changeEmail_userNotFound() {
        // Given — Mapper 가 null 반환 (회원 없음)
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(null);

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.changeEmail(501L));
        assertEquals(UserErrorCode.USER_NOT_FOUND, ex.getErrorCode());

        verify(userMapper, never()).updateUserEmail(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("이메일 변경 - 기타 비활성(차단 등) 회원 → USER_NOT_FOUND (계정 존재 여부 비노출)")
    void changeEmail_blockedUser() {
        // Given — BLOCKED 상태 회원
        MyProfileVO blocked = activeUserWithoutProfile();
        blocked.setStatus("BLOCKED");
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(blocked);

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.changeEmail(501L));
        assertEquals(UserErrorCode.USER_NOT_FOUND, ex.getErrorCode());

        verify(userMapper, never()).updateUserEmail(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("이메일 변경 - UPDATE 영향 row 0 (조회-갱신 사이 동시 탈퇴) → USER_ALREADY_WITHDRAWN + 소비 없음")
    void changeEmail_updateNoRows() {
        // Given — ACTIVE 회원 + 인증 성공 + 중복 없음 + UPDATE 가 0 row 반환 (동시 탈퇴)
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(activeUserWithoutProfile());
        when(emailVerificationService.getVerifiedEmail(501L)).thenReturn(NEW_EMAIL);
        when(userMapper.countByEmailHashExcludingUserId(sha256Hex(NEW_EMAIL), 501L)).thenReturn(0);
        when(userMapper.updateUserEmail(eq(501L), anyString(), anyString())).thenReturn(0);

        // When & Then — Race Condition 최종 방어선 (WHERE status != 'WITHDRAWN')
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.changeEmail(501L));
        assertEquals(UserErrorCode.USER_ALREADY_WITHDRAWN, ex.getErrorCode());

        // DB 갱신이 실패했으므로 인증 세션을 소비하지 않아야 한다 (재시도 가능)
        verify(emailVerificationService, never()).consumeVerification(any());
    }

    @Test
    @DisplayName("이메일 변경 - email_hash UNIQUE 충돌(DuplicateKeyException) → EMAIL_ALREADY_IN_USE")
    void changeEmail_duplicateKey() {
        // Given — ACTIVE 회원 + 인증 성공 + UPDATE 중 동시 요청으로 UNIQUE 충돌
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(activeUserWithoutProfile());
        when(emailVerificationService.getVerifiedEmail(501L)).thenReturn(NEW_EMAIL);
        when(userMapper.countByEmailHashExcludingUserId(sha256Hex(NEW_EMAIL), 501L)).thenReturn(0);
        when(userMapper.updateUserEmail(eq(501L), anyString(), anyString()))
                .thenThrow(new DuplicateKeyException("users.email_hash UNIQUE constraint violated"));

        // When & Then — Race Condition 방어선: 500 대신 409
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.changeEmail(501L));
        assertEquals(UserErrorCode.EMAIL_ALREADY_IN_USE, ex.getErrorCode());

        // DB 갱신이 실패했으므로 인증 세션을 소비하지 않아야 한다 (재시도 가능)
        verify(emailVerificationService, never()).consumeVerification(any());
    }

    @Test
    @DisplayName("이메일 변경 재사용 방지 - 변경 성공 후 인증 세션 소비, 이후 동일 인증정보로 재요청 시 실패")
    void changeEmail_reusePrevention() {
        // Given — 첫 번째 변경 성공 (getVerifiedEmail → new@example.com),
        //        두 번째 요청은 인증 세션이 소비된 상태 (getVerifiedEmail → EMAIL_VERIFICATION_REQUIRED)
        when(userMapper.selectMyProfileByUserId(501L)).thenReturn(activeUserWithoutProfile());
        when(emailVerificationService.getVerifiedEmail(501L))
                .thenReturn(NEW_EMAIL)
                .thenThrow(new BusinessException(UserErrorCode.EMAIL_VERIFICATION_REQUIRED));
        when(userMapper.countByEmailHashExcludingUserId(sha256Hex(NEW_EMAIL), 501L)).thenReturn(0);
        when(userMapper.updateUserEmail(eq(501L), anyString(), anyString())).thenReturn(1);

        // When — 첫 번째 변경 성공 + 인증 세션 소비 (동일 인증 결과 재사용 방지 — docs)
        EmailChangeResponseDTO first = userService.changeEmail(501L);
        assertEquals(NEW_EMAIL, first.getUpdatedEmail());
        verify(emailVerificationService).consumeVerification(501L);

        // When & Then — 두 번째 변경 요청은 인증 세션이 소비되어 EMAIL_VERIFICATION_REQUIRED 로 실패
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.changeEmail(501L));
        assertEquals(UserErrorCode.EMAIL_VERIFICATION_REQUIRED, ex.getErrorCode());
    }

    /** SHA-256 hex 변환 — 저장될 phone_number_hash 기대값 계산 (Service 와 동일 hex 인코딩) */
    private String sha256Hex(String value) {
        byte[] digest;
        try {
            digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
        StringBuilder sb = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }

    /** 지정한 길이의 문자열 생성 — Java 8 호환 (String.repeat 미사용) */
    private String buildString(int length, char c) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(c);
        }
        return sb.toString();
    }
}
