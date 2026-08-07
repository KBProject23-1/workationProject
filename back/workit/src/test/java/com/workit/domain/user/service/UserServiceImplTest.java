package com.workit.domain.user.service;

import com.workit.domain.user.dto.request.ProfileOnboardingRequestDTO;
import com.workit.domain.user.dto.request.ProfileUpdateRequestDTO;
import com.workit.domain.user.dto.response.MyProfileResponseDTO;
import com.workit.domain.user.dto.response.ProfileOnboardingResponseDTO;
import com.workit.domain.user.exception.UserErrorCode;
import com.workit.domain.user.mapper.UserMapper;
import com.workit.domain.user.vo.MyProfileVO;
import com.workit.domain.user.vo.UserProfileVO;
import com.workit.exception.BusinessException;
import com.workit.global.util.PersonalDataCipher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        // AES 키 주입 후 PersonalDataCipher 재로드 (AuthServiceImplTest 와 동일 패턴)
        System.setProperty("personal.data.aes.key", TEST_AES_KEY);
        PersonalDataCipher.reloadKey();

        userService = new UserServiceImpl(userMapper);
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

        // Then — nickname 만 전달되어 동적 UPDATE (companyName 은 null → XML <if> 로 제외)
        ArgumentCaptor<UserProfileVO> captor = ArgumentCaptor.forClass(UserProfileVO.class);
        verify(userMapper).updateUserProfile(captor.capture());
        UserProfileVO saved = captor.getValue();
        assertEquals(501L, saved.getUserId());
        assertEquals("새로운닉네임", saved.getNickname());
        assertNull(saved.getCompanyName());
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

        // Then — companyName 만 전달되어 동적 UPDATE (nickname 은 null → XML <if> 로 제외)
        ArgumentCaptor<UserProfileVO> captor = ArgumentCaptor.forClass(UserProfileVO.class);
        verify(userMapper).updateUserProfile(captor.capture());
        UserProfileVO saved = captor.getValue();
        assertEquals(501L, saved.getUserId());
        assertNull(saved.getNickname());
        assertEquals("구글코리아", saved.getCompanyName());
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

        // Then — trim 후 두 필드 모두 전달
        ArgumentCaptor<UserProfileVO> captor = ArgumentCaptor.forClass(UserProfileVO.class);
        verify(userMapper).updateUserProfile(captor.capture());
        UserProfileVO saved = captor.getValue();
        assertEquals("새닉네임", saved.getNickname());
        assertEquals("구글코리아", saved.getCompanyName());
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
        ProfileUpdateRequestDTO blankRequest = new ProfileUpdateRequestDTO();
        blankRequest.setNickname("   ");
        blankRequest.setCompanyName("");

        // When & Then — 모두 INVALID_PROFILE_REQUEST (PATCH: 최소 1개 필드 필요)
        assertThrows(BusinessException.class, () -> userService.updateProfile(501L, null));
        assertThrows(BusinessException.class, () -> userService.updateProfile(501L, nullRequest));
        assertThrows(BusinessException.class, () -> userService.updateProfile(501L, blankRequest));

        // Then — 저장 Mapper 미호출
        verify(userMapper, never()).updateUserProfile(any(UserProfileVO.class));
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
