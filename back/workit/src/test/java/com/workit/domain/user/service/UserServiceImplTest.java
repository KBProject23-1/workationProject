package com.workit.domain.user.service;

import com.workit.domain.auth.exception.AuthErrorCode;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

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

    @Mock
    private UserMapper userMapper;

    // 회원 탈퇴 시 비밀번호 검증/Refresh 세션 revoke 를 AuthService 로 위임한다 (Mock 주입)
    @Mock
    private AuthService authService;

    // 회원 탈퇴 시 지갑 잔액 조회를 WalletService 로 위임한다 (Mock 주입)
    @Mock
    private WalletService walletService;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        // AES 키 주입 후 PersonalDataCipher 재로드 (AuthServiceImplTest 와 동일 패턴)
        System.setProperty("personal.data.aes.key", TEST_AES_KEY);
        PersonalDataCipher.reloadKey();

        userService = new UserServiceImpl(userMapper, authService, walletService);
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
        vo.setEmailEncrypt(PersonalDataCipher.encrypt(EMAIL));
        vo.setNameEncrypt(PersonalDataCipher.encrypt(NAME));
        vo.setPhoneNumberEncrypt(PersonalDataCipher.encrypt(PHONE_NUMBER));
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

    /** 지정한 길이의 문자열 생성 — Java 8 호환 (String.repeat 미사용) */
    private String buildString(int length, char c) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(c);
        }
        return sb.toString();
    }
}
