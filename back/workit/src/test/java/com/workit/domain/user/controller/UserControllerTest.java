package com.workit.domain.user.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workit.domain.auth.dto.request.ChangePasswordRequestDTO;
import com.workit.domain.auth.exception.AuthErrorCode;
import com.workit.domain.auth.service.AuthService;
import com.workit.domain.user.dto.request.AccountPasswordVerifyRequestDTO;
import com.workit.domain.user.dto.request.PhoneChangeRequestDTO;
import com.workit.domain.user.dto.request.ProfileOnboardingRequestDTO;
import com.workit.domain.user.dto.request.ProfileUpdateRequestDTO;
import com.workit.domain.user.dto.request.UserWithdrawalRequestDTO;
import com.workit.domain.user.dto.response.MyProfileResponseDTO;
import com.workit.domain.user.dto.response.PhoneChangeResponseDTO;
import com.workit.domain.user.dto.response.ProfileOnboardingResponseDTO;
import com.workit.domain.user.exception.UserErrorCode;
import com.workit.domain.user.service.UserService;
import com.workit.domain.wallet.exception.WalletErrorCode;
import com.workit.exception.BusinessException;
import com.workit.exception.CommonErrorCode;
import com.workit.exception.CommonExceptionAdvice;
import com.workit.exception.ErrorCode;
import com.workit.security.CurrentUserArgumentResolver;
import com.workit.security.WorkitPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// UserController (내 프로필 조회 / 프로필 최초 등록) 테스트
// - UserService 를 Mockito @Mock 으로 주입한다 (Controller 계층 검증에 집중)
// - @CurrentUser Long userId 는 CurrentUserArgumentResolver 가 SecurityContext 에서 해석한다
//   (AuthControllerTest 의 PIN 설정 테스트와 동일 패턴)
@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    // 비밀번호 변경은 Auth 도메인 책임 — UserController 가 AuthService 로 위임하므로 Mock 으로 주입한다
    @Mock
    private AuthService authService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // Cookie Secure/SameSite 는 @Value 파라미터 — Cookie Secure 속성 검증을 위해
        // 운영 기준(true, Lax) 사용 (AuthControllerTest 의 standaloneSetup 과 동일 패턴)
        mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userService, authService, true, "Lax"))
                .setControllerAdvice(new CommonExceptionAdvice())
                // @CurrentUser Long userId 파라미터 해석용 — 운영에서는 ServletConfig 가 등록한다
                .setCustomArgumentResolvers(new CurrentUserArgumentResolver())
                .build();
    }

    @AfterEach
    void clearSecurityContext() {
        // @CurrentUser 테스트에서 설정한 인증 객체가 다른 테스트에 영향 주지 않도록 초기화
        SecurityContextHolder.clearContext();
    }

    private JsonNode parse(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        return new ObjectMapper().readTree(body);
    }

    // ---------- Stub 헬퍼 (Mockito when / doThrow) ----------

    /** 내 프로필 조회 성공 Stub — Service 반환값과 무관하게 Controller 전달값만 검증 */
    private void stubGetMyProfileSuccess() {
        when(userService.getMyProfile(any()))
                .thenReturn(MyProfileResponseDTO.of(
                        "user@example.com", "홍길동", "01012345678", "지갑대장홍길동", "6인조테크"));
    }

    /** 프로필 최초 등록 성공 Stub — Service 가 profileId 포함 응답을 반환한다 */
    private void stubOnboardProfileSuccess() {
        when(userService.onboardProfile(any(), any(ProfileOnboardingRequestDTO.class)))
                .thenReturn(ProfileOnboardingResponseDTO.of(12L, 501L, "지갑대장홍길동", "6인조테크"));
    }

    /** 내 프로필 조회 실패 Stub — Service 가 지정 에러를 던진다 */
    private void stubGetMyProfileError(UserErrorCode errorCode) {
        doThrow(new BusinessException(errorCode)).when(userService).getMyProfile(any());
    }

    /** 프로필 최초 등록 실패 Stub — Service 가 지정 에러를 던진다 */
    private void stubOnboardProfileError(UserErrorCode errorCode) {
        doThrow(new BusinessException(errorCode))
                .when(userService).onboardProfile(anyLong(), any(ProfileOnboardingRequestDTO.class));
    }

    /** 프로필 수정 실패 Stub — Service 가 지정 에러를 던진다 */
    private void stubUpdateProfileError(UserErrorCode errorCode) {
        doThrow(new BusinessException(errorCode))
                .when(userService).updateProfile(anyLong(), any(ProfileUpdateRequestDTO.class));
    }

    /** 비밀번호 변경 실패 Stub — AuthService 가 지정 에러를 던진다 */
    private void stubChangePasswordError(AuthErrorCode errorCode) {
        doThrow(new BusinessException(errorCode))
                .when(authService).changePassword(anyLong(), any(ChangePasswordRequestDTO.class));
    }

    /** 회원 탈퇴 실패 Stub — UserService 가 지정 에러를 던진다 */
    private void stubWithdrawError(ErrorCode errorCode) {
        doThrow(new BusinessException(errorCode))
                .when(userService).withdraw(anyLong(), any(UserWithdrawalRequestDTO.class));
    }

    /** 비밀번호 재인증 실패 Stub — UserService 가 지정 에러를 던진다 */
    private void stubVerifyAccountPasswordError(ErrorCode errorCode) {
        doThrow(new BusinessException(errorCode))
                .when(userService).verifyAccountPassword(anyLong(), any(AccountPasswordVerifyRequestDTO.class));
    }

    /** 휴대폰 번호 변경 성공 Stub — Service 가 변경된 번호를 반환한다 */
    private void stubChangePhoneSuccess() {
        when(userService.changePhone(any(), any(PhoneChangeRequestDTO.class)))
                .thenReturn(PhoneChangeResponseDTO.of("01098765432"));
    }

    /** 휴대폰 번호 변경 실패 Stub — UserService 가 지정 에러를 던진다 */
    private void stubChangePhoneError(ErrorCode errorCode) {
        doThrow(new BusinessException(errorCode))
                .when(userService).changePhone(anyLong(), any(PhoneChangeRequestDTO.class));
    }

    // ---------- 내 프로필 조회 ----------

    @Test
    @DisplayName("내 프로필 조회 성공 - 200 + SUCCESS + email/name/phoneNumber/nickname/companyName 반환")
    void getMyProfile_success() throws Exception {
        // Given — JWT 인증된 로그인 사용자 + Service 가 프로필을 반환한다
        SecurityContextHolder.getContext().setAuthentication(new WorkitPrincipal(501L, "ROLE_USER"));
        stubGetMyProfileSuccess();

        // When
        MvcResult result = mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("SUCCESS", json.get("status").asText());
        assertEquals("프로필 정보를 성공적으로 조회했습니다.", json.get("message").asText());
        assertTrue(json.get("errorCode") == null || json.get("errorCode").isNull());

        JsonNode data = json.get("data");
        assertNotNull(data);
        assertEquals("user@example.com", data.get("email").asText());
        assertEquals("홍길동", data.get("name").asText());
        assertEquals("01012345678", data.get("phoneNumber").asText());
        assertEquals("지갑대장홍길동", data.get("nickname").asText());
        assertEquals("6인조테크", data.get("companyName").asText());

        // Service 가 인증 userId 를 전달받았는지 확인
        verify(userService).getMyProfile(eq(501L));
    }

    @Test
    @DisplayName("내 프로필 조회 - 회원 없음 → 404 + USER_NOT_FOUND")
    void getMyProfile_userNotFound() throws Exception {
        // Given — JWT 인증된 로그인 사용자 + Service 가 회원 없음을 거부한다
        SecurityContextHolder.getContext().setAuthentication(new WorkitPrincipal(501L, "ROLE_USER"));
        stubGetMyProfileError(UserErrorCode.USER_NOT_FOUND);

        // When
        MvcResult result = mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isNotFound())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("USER_NOT_FOUND", json.get("errorCode").asText());
        assertEquals("회원 정보를 찾을 수 없습니다.", json.get("message").asText());
    }

    @Test
    @DisplayName("내 프로필 조회 - 인증 사용자 없음 → 401 + AUTH_TOKEN_NOT_FOUND")
    void getMyProfile_unauthenticated() throws Exception {
        // Given — SecurityContext 에 인증 객체가 없음 (CurrentUserArgumentResolver 가 401 처리)

        // When
        MvcResult result = mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        // Then — Service 호출 없이 401 응답
        verify(userService, never()).getMyProfile(any());
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("AUTH_TOKEN_NOT_FOUND", json.get("errorCode").asText());
    }

    // ---------- 프로필 최초 등록 ----------

    @Test
    @DisplayName("프로필 최초 등록 성공 - 201 + SUCCESS + profileId/userId/nickname/companyName 반환")
    void onboardProfile_success() throws Exception {
        // Given — JWT 인증된 로그인 사용자 + Service 가 등록 완료 결과를 반환한다
        SecurityContextHolder.getContext().setAuthentication(new WorkitPrincipal(501L, "ROLE_USER"));
        stubOnboardProfileSuccess();

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/users/me/onboarding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"지갑대장홍길동\",\"companyName\":\"6인조테크\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("SUCCESS", json.get("status").asText());
        assertEquals("유저 프로필 정보가 성공적으로 등록되었습니다.", json.get("message").asText());
        assertTrue(json.get("errorCode") == null || json.get("errorCode").isNull());

        JsonNode data = json.get("data");
        assertNotNull(data);
        assertEquals(12, data.get("profileId").asInt());
        assertEquals(501, data.get("userId").asInt());
        assertEquals("지갑대장홍길동", data.get("nickname").asText());
        assertEquals("6인조테크", data.get("companyName").asText());

        // Controller 는 userId 와 요청을 Service 로 위임만 한다 (DB 접근/검증 금지)
        verify(userService).onboardProfile(eq(501L), any(ProfileOnboardingRequestDTO.class));
    }

    @Test
    @DisplayName("프로필 최초 등록 - 잘못된 요청(nickname 누락) → 400 + INVALID_PROFILE_REQUEST")
    void onboardProfile_invalidRequest() throws Exception {
        // Given — JWT 인증된 로그인 사용자 + Service 가 요청 검증 실패를 거부한다
        SecurityContextHolder.getContext().setAuthentication(new WorkitPrincipal(501L, "ROLE_USER"));
        stubOnboardProfileError(UserErrorCode.INVALID_PROFILE_REQUEST);

        // When — nickname 누락 본문
        MvcResult result = mockMvc.perform(post("/api/v1/users/me/onboarding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"6인조테크\"}"))
                .andExpect(status().isBadRequest())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("INVALID_PROFILE_REQUEST", json.get("errorCode").asText());
        assertEquals("프로필 정보를 확인해주세요.", json.get("message").asText());
    }

    @Test
    @DisplayName("프로필 최초 등록 - 닉네임 중복 → 409 + DUPLICATE_NICKNAME")
    void onboardProfile_duplicateNickname() throws Exception {
        // Given — JWT 인증된 로그인 사용자 + Service 가 닉네임 중복을 거부한다
        SecurityContextHolder.getContext().setAuthentication(new WorkitPrincipal(501L, "ROLE_USER"));
        stubOnboardProfileError(UserErrorCode.DUPLICATE_NICKNAME);

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/users/me/onboarding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"이미쓴닉네임\"}"))
                .andExpect(status().isConflict())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("DUPLICATE_NICKNAME", json.get("errorCode").asText());
    }

    @Test
    @DisplayName("프로필 최초 등록 - 이미 프로필 등록된 사용자 → 409 + PROFILE_ALREADY_EXISTS")
    void onboardProfile_alreadyRegistered() throws Exception {
        // Given — JWT 인증된 로그인 사용자 + Service 가 재등록을 거부한다
        SecurityContextHolder.getContext().setAuthentication(new WorkitPrincipal(501L, "ROLE_USER"));
        stubOnboardProfileError(UserErrorCode.PROFILE_ALREADY_EXISTS);

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/users/me/onboarding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"새닉네임\"}"))
                .andExpect(status().isConflict())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("PROFILE_ALREADY_EXISTS", json.get("errorCode").asText());
    }

    // ---------- 프로필 수정 ----------

    @Test
    @DisplayName("프로필 수정 성공 - 200 + SUCCESS + 수정 완료 메시지")
    void updateProfile_success() throws Exception {
        // Given — JWT 인증된 로그인 사용자 + nickname/companyName 동시 수정 요청
        SecurityContextHolder.getContext().setAuthentication(new WorkitPrincipal(501L, "ROLE_USER"));

        // When
        MvcResult result = mockMvc.perform(patch("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"새로운닉네임\",\"companyName\":\"구글코리아\"}"))
                .andExpect(status().isOk())
                .andReturn();

        // Then — docs 응답: data null + 수정 완료 메시지
        //   (CommonResponse 는 NON_NULL 직렬화 — data 가 null 이면 JSON 에서 제외됨)
        JsonNode json = parse(result);
        assertEquals("SUCCESS", json.get("status").asText());
        assertEquals("프로필 정보가 성공적으로 수정되었습니다.", json.get("message").asText());
        assertTrue(json.get("errorCode") == null || json.get("errorCode").isNull());
        assertTrue(json.get("data") == null || json.get("data").isNull());

        // Controller 는 userId 와 요청을 Service 로 위임만 한다 (DB 접근/검증 금지)
        verify(userService).updateProfile(eq(501L), any(ProfileUpdateRequestDTO.class));
    }

    @Test
    @DisplayName("프로필 수정 성공 - nickname 만 전달 (PATCH 부분 수정)")
    void updateProfile_nicknameOnly() throws Exception {
        // Given — JWT 인증된 로그인 사용자 + nickname 만 수정 요청
        SecurityContextHolder.getContext().setAuthentication(new WorkitPrincipal(501L, "ROLE_USER"));

        // When
        MvcResult result = mockMvc.perform(patch("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"새로운닉네임\"}"))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("SUCCESS", json.get("status").asText());
        verify(userService).updateProfile(eq(501L), any(ProfileUpdateRequestDTO.class));
    }

    @Test
    @DisplayName("프로필 수정 성공 - companyName 만 전달 (PATCH 부분 수정)")
    void updateProfile_companyNameOnly() throws Exception {
        // Given — JWT 인증된 로그인 사용자 + companyName 만 수정 요청
        SecurityContextHolder.getContext().setAuthentication(new WorkitPrincipal(501L, "ROLE_USER"));

        // When
        MvcResult result = mockMvc.perform(patch("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"구글코리아\"}"))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("SUCCESS", json.get("status").asText());
        verify(userService).updateProfile(eq(501L), any(ProfileUpdateRequestDTO.class));
    }

    @Test
    @DisplayName("프로필 수정 성공 - companyName 명시적 null 전달 (소속 회사 삭제 → NULL 저장)")
    void updateProfile_clearCompanyName() throws Exception {
        // Given — JWT 인증된 로그인 사용자 + companyName 을 null 로 전달 (삭제 요청)
        SecurityContextHolder.getContext().setAuthentication(new WorkitPrincipal(501L, "ROLE_USER"));

        // When
        MvcResult result = mockMvc.perform(patch("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":null}"))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("SUCCESS", json.get("status").asText());
        // Controller 는 userId 와 요청을 Service 로 위임만 한다 (DB 접근/검증 금지)
        verify(userService).updateProfile(eq(501L), any(ProfileUpdateRequestDTO.class));
    }

    @Test
    @DisplayName("프로필 수정 - 잘못된 요청(수정 필드 없음) → 400 + INVALID_PROFILE_REQUEST")
    void updateProfile_invalidRequest() throws Exception {
        // Given — JWT 인증된 로그인 사용자 + Service 가 요청 검증 실패를 거부한다
        SecurityContextHolder.getContext().setAuthentication(new WorkitPrincipal(501L, "ROLE_USER"));
        stubUpdateProfileError(UserErrorCode.INVALID_PROFILE_REQUEST);

        // When — 수정 대상 필드 없는 빈 본문
        MvcResult result = mockMvc.perform(patch("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("INVALID_PROFILE_REQUEST", json.get("errorCode").asText());
        assertEquals("프로필 정보를 확인해주세요.", json.get("message").asText());
    }

    @Test
    @DisplayName("프로필 수정 - 프로필 미등록 → 404 + PROFILE_NOT_FOUND")
    void updateProfile_profileNotFound() throws Exception {
        // Given — JWT 인증된 로그인 사용자 + Service 가 프로필 미등록을 거부한다
        SecurityContextHolder.getContext().setAuthentication(new WorkitPrincipal(501L, "ROLE_USER"));
        stubUpdateProfileError(UserErrorCode.PROFILE_NOT_FOUND);

        // When
        MvcResult result = mockMvc.perform(patch("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"새로운닉네임\"}"))
                .andExpect(status().isNotFound())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("PROFILE_NOT_FOUND", json.get("errorCode").asText());
    }

    @Test
    @DisplayName("프로필 수정 - 닉네임 중복 → 409 + DUPLICATE_NICKNAME")
    void updateProfile_duplicateNickname() throws Exception {
        // Given — JWT 인증된 로그인 사용자 + Service 가 닉네임 중복을 거부한다
        SecurityContextHolder.getContext().setAuthentication(new WorkitPrincipal(501L, "ROLE_USER"));
        stubUpdateProfileError(UserErrorCode.DUPLICATE_NICKNAME);

        // When
        MvcResult result = mockMvc.perform(patch("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"이미쓴닉네임\"}"))
                .andExpect(status().isConflict())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("DUPLICATE_NICKNAME", json.get("errorCode").asText());
    }

    @Test
    @DisplayName("프로필 수정 - 인증 사용자 없음 → 401 + AUTH_TOKEN_NOT_FOUND")
    void updateProfile_unauthenticated() throws Exception {
        // Given — SecurityContext 에 인증 객체가 없음 (CurrentUserArgumentResolver 가 401 처리)

        // When
        MvcResult result = mockMvc.perform(patch("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"새로운닉네임\"}"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        // Then — Service 호출 없이 401 응답
        verify(userService, never()).updateProfile(anyLong(), any(ProfileUpdateRequestDTO.class));
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("AUTH_TOKEN_NOT_FOUND", json.get("errorCode").asText());
    }

    @Test
    @DisplayName("프로필 최초 등록 - 인증 사용자 없음 → 401 + AUTH_TOKEN_NOT_FOUND")
    void onboardProfile_unauthenticated() throws Exception {
        // Given — SecurityContext 에 인증 객체가 없음 (CurrentUserArgumentResolver 가 401 처리)

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/users/me/onboarding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"지갑대장홍길동\"}"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        // Then — Service 호출 없이 401 응답
        verify(userService, never()).onboardProfile(anyLong(), any(ProfileOnboardingRequestDTO.class));
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("AUTH_TOKEN_NOT_FOUND", json.get("errorCode").asText());
    }

    // ---------- 내 비밀번호 변경 ----------

    @Test
    @DisplayName("비밀번호 변경 성공 - 200 + SUCCESS + 변경 완료 메시지 + AuthService 위임")
    void changePassword_success() throws Exception {
        // Given — JWT 인증된 로그인 사용자
        SecurityContextHolder.getContext().setAuthentication(new WorkitPrincipal(501L, "ROLE_USER"));

        // When
        MvcResult result = mockMvc.perform(patch("/api/v1/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"CurrentPassword123!\",\"newPassword\":\"NewPassword123!\"}"))
                .andExpect(status().isOk())
                .andReturn();

        // Then — docs 응답: data null + 변경 완료 메시지
        //   (CommonResponse 는 NON_NULL 직렬화 — data 가 null 이면 JSON 에서 제외됨)
        JsonNode json = parse(result);
        assertEquals("SUCCESS", json.get("status").asText());
        assertEquals("비밀번호가 성공적으로 변경되었습니다.", json.get("message").asText());
        assertTrue(json.get("errorCode") == null || json.get("errorCode").isNull());
        assertTrue(json.get("data") == null || json.get("data").isNull());

        // Controller 는 userId 와 요청을 AuthService 로 위임만 한다 (DB 접근/암호화 금지)
        verify(authService).changePassword(eq(501L), any(ChangePasswordRequestDTO.class));
    }

    @Test
    @DisplayName("비밀번호 변경 - 현재 비밀번호 불일치 → 400 + AUTH_INVALID_PASSWORD")
    void changePassword_invalidCurrentPassword() throws Exception {
        // Given — JWT 인증된 로그인 사용자 + Service 가 현재 비밀번호 불일치를 거부한다
        SecurityContextHolder.getContext().setAuthentication(new WorkitPrincipal(501L, "ROLE_USER"));
        stubChangePasswordError(AuthErrorCode.AUTH_INVALID_PASSWORD);

        // When
        MvcResult result = mockMvc.perform(patch("/api/v1/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"WrongPassword123!\",\"newPassword\":\"NewPassword123!\"}"))
                .andExpect(status().isBadRequest())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("AUTH_INVALID_PASSWORD", json.get("errorCode").asText());
        assertEquals("현재 비밀번호가 올바르지 않습니다.", json.get("message").asText());
    }

    @Test
    @DisplayName("비밀번호 변경 - 신규 비밀번호가 현재와 동일 → 400 + AUTH_SAME_PASSWORD")
    void changePassword_samePassword() throws Exception {
        // Given — JWT 인증된 로그인 사용자 + Service 가 동일 비밀번호를 거부한다
        SecurityContextHolder.getContext().setAuthentication(new WorkitPrincipal(501L, "ROLE_USER"));
        stubChangePasswordError(AuthErrorCode.AUTH_SAME_PASSWORD);

        // When
        MvcResult result = mockMvc.perform(patch("/api/v1/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"CurrentPassword123!\",\"newPassword\":\"CurrentPassword123!\"}"))
                .andExpect(status().isBadRequest())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("AUTH_SAME_PASSWORD", json.get("errorCode").asText());
        assertEquals("현재 비밀번호와 다른 비밀번호를 입력해 주세요.", json.get("message").asText());
    }

    @Test
    @DisplayName("비밀번호 변경 - 약한 비밀번호 → 422 + WEAK_PASSWORD")
    void changePassword_weakPassword() throws Exception {
        // Given — JWT 인증된 로그인 사용자 + Service 가 비밀번호 정책 미달을 거부한다
        SecurityContextHolder.getContext().setAuthentication(new WorkitPrincipal(501L, "ROLE_USER"));
        stubChangePasswordError(AuthErrorCode.WEAK_PASSWORD);

        // When
        MvcResult result = mockMvc.perform(patch("/api/v1/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"CurrentPassword123!\",\"newPassword\":\"password\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("WEAK_PASSWORD", json.get("errorCode").asText());
    }

    @Test
    @DisplayName("비밀번호 변경 - 요청 값 누락 → 400 + INVALID_PASSWORD_CHANGE_REQUEST")
    void changePassword_missingFields() throws Exception {
        // Given — JWT 인증된 로그인 사용자 + Service 가 필수 값 누락을 거부한다
        SecurityContextHolder.getContext().setAuthentication(new WorkitPrincipal(501L, "ROLE_USER"));
        stubChangePasswordError(AuthErrorCode.INVALID_PASSWORD_CHANGE_REQUEST);

        // When — newPassword 누락 본문
        MvcResult result = mockMvc.perform(patch("/api/v1/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"CurrentPassword123!\"}"))
                .andExpect(status().isBadRequest())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("INVALID_PASSWORD_CHANGE_REQUEST", json.get("errorCode").asText());
    }

    @Test
    @DisplayName("비밀번호 변경 - 회원 없음 → 404 + USER_NOT_FOUND")
    void changePassword_userNotFound() throws Exception {
        // Given — JWT 인증된 로그인 사용자 + Service 가 회원 없음을 거부한다
        SecurityContextHolder.getContext().setAuthentication(new WorkitPrincipal(501L, "ROLE_USER"));
        stubChangePasswordError(AuthErrorCode.USER_NOT_FOUND);

        // When
        MvcResult result = mockMvc.perform(patch("/api/v1/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"CurrentPassword123!\",\"newPassword\":\"NewPassword123!\"}"))
                .andExpect(status().isNotFound())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("USER_NOT_FOUND", json.get("errorCode").asText());
    }

    @Test
    @DisplayName("비밀번호 변경 - 인증 사용자 없음 → 401 + AUTH_TOKEN_NOT_FOUND")
    void changePassword_unauthenticated() throws Exception {
        // Given — SecurityContext 에 인증 객체가 없음 (CurrentUserArgumentResolver 가 401 처리)

        // When
        MvcResult result = mockMvc.perform(patch("/api/v1/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"CurrentPassword123!\",\"newPassword\":\"NewPassword123!\"}"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        // Then — Service 호출 없이 401 응답
        verify(authService, never()).changePassword(anyLong(), any(ChangePasswordRequestDTO.class));
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("AUTH_TOKEN_NOT_FOUND", json.get("errorCode").asText());
    }

    // ---------- 회원 탈퇴 ----------

    @Test
    @DisplayName("회원 탈퇴 성공 - 200 + SUCCESS + 탈퇴 완료 메시지 + Access/Refresh Token Cookie 즉시 만료(Max-Age=0)")
    void withdraw_success() throws Exception {
        // Given — JWT 인증된 로그인 사용자 + Service 는 정상 탈퇴를 허용한다 (void — 별도 Stub 불필요)
        SecurityContextHolder.getContext().setAuthentication(new WorkitPrincipal(501L, "ROLE_USER"));

        // When — 현재 비밀번호 재확인 요청
        MvcResult result = mockMvc.perform(delete("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"user_password123!\"}"))
                .andExpect(status().isOk())
                .andReturn();

        // Then — docs 응답: data null + 탈퇴 완료 메시지
        //   (CommonResponse 는 NON_NULL 직렬화 — data 가 null 이면 JSON 에서 제외됨)
        JsonNode json = parse(result);
        assertEquals("SUCCESS", json.get("status").asText());
        assertEquals("회원탈퇴가 정상적으로 처리되었습니다.", json.get("message").asText());
        assertTrue(json.get("errorCode") == null || json.get("errorCode").isNull());
        assertTrue(json.get("data") == null || json.get("data").isNull());

        // Set-Cookie 2개 — accessToken + refreshToken 모두 즉시 만료 (logout 과 동일: Max-Age=0; HttpOnly; Path=/; SameSite=Lax; Secure)
        java.util.List<String> setCookies = result.getResponse().getHeaders("Set-Cookie");
        assertEquals(2, setCookies.size(), "탈퇴 응답에는 accessToken/refreshToken Cookie 2개가 있어야 한다");
        for (String setCookie : setCookies) {
            assertTrue(setCookie.contains("="), "쿠키명/값: " + setCookie);
            assertTrue(setCookie.contains("Max-Age=0"), "Max-Age 속성: " + setCookie);
            assertTrue(setCookie.contains("HttpOnly"), "HttpOnly 속성: " + setCookie);
            assertTrue(setCookie.contains("Path=/"), "Path 속성: " + setCookie);
            assertTrue(setCookie.contains("Secure"), "Secure 속성: " + setCookie);
            assertTrue(setCookie.contains("SameSite=Lax"), "SameSite 속성: " + setCookie);
        }
        assertTrue(setCookies.stream().anyMatch(c -> c.startsWith("accessToken=")),
                "accessToken Cookie 가 포함되어야 한다: " + setCookies);
        assertTrue(setCookies.stream().anyMatch(c -> c.startsWith("refreshToken=")),
                "refreshToken Cookie 가 포함되어야 한다: " + setCookies);

        // Controller 는 userId 와 요청을 Service 로 위임만 한다 (비밀번호 비교/DB/Redis 금지)
        verify(userService).withdraw(eq(501L), any(UserWithdrawalRequestDTO.class));
    }

    @Test
    @DisplayName("회원 탈퇴 - password 누락 → 400 + COMMON_INVALID_REQUEST")
    void withdraw_missingPassword() throws Exception {
        // Given — JWT 인증된 로그인 사용자 + Service 가 필수 값 누락을 거부한다
        SecurityContextHolder.getContext().setAuthentication(new WorkitPrincipal(501L, "ROLE_USER"));
        stubWithdrawError(CommonErrorCode.COMMON_INVALID_REQUEST);

        // When — password 없는 본문
        MvcResult result = mockMvc.perform(delete("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("COMMON_INVALID_REQUEST", json.get("errorCode").asText());
        assertEquals("요청 값이 올바르지 않습니다.", json.get("message").asText());
    }

    @Test
    @DisplayName("회원 탈퇴 - 비밀번호 불일치 → 400 + AUTH_INVALID_PASSWORD")
    void withdraw_wrongPassword() throws Exception {
        // Given — JWT 인증된 로그인 사용자 + Service 가 현재 비밀번호 불일치를 거부한다
        SecurityContextHolder.getContext().setAuthentication(new WorkitPrincipal(501L, "ROLE_USER"));
        stubWithdrawError(AuthErrorCode.AUTH_INVALID_PASSWORD);

        // When
        MvcResult result = mockMvc.perform(delete("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"wrong-password!\"}"))
                .andExpect(status().isBadRequest())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("AUTH_INVALID_PASSWORD", json.get("errorCode").asText());
        assertEquals("현재 비밀번호가 올바르지 않습니다.", json.get("message").asText());
    }

    @Test
    @DisplayName("회원 탈퇴 - 지갑 잔액 잔존 → 409 + WALLET_BALANCE_REMAINING")
    void withdraw_balanceRemaining() throws Exception {
        // Given — JWT 인증된 로그인 사용자 + Service 가 잔액 잔존으로 탈퇴를 거부한다
        SecurityContextHolder.getContext().setAuthentication(new WorkitPrincipal(501L, "ROLE_USER"));
        stubWithdrawError(WalletErrorCode.WALLET_BALANCE_REMAINING);

        // When
        MvcResult result = mockMvc.perform(delete("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"user_password123!\"}"))
                .andExpect(status().isConflict())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("WALLET_BALANCE_REMAINING", json.get("errorCode").asText());
    }

    @Test
    @DisplayName("회원 탈퇴 - 이미 탈퇴 상태 → 409 + USER_ALREADY_WITHDRAWN")
    void withdraw_alreadyWithdrawn() throws Exception {
        // Given — JWT 인증된 로그인 사용자 + Service 가 이미 탈퇴 상태를 거부한다
        SecurityContextHolder.getContext().setAuthentication(new WorkitPrincipal(501L, "ROLE_USER"));
        stubWithdrawError(UserErrorCode.USER_ALREADY_WITHDRAWN);

        // When
        MvcResult result = mockMvc.perform(delete("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"user_password123!\"}"))
                .andExpect(status().isConflict())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("USER_ALREADY_WITHDRAWN", json.get("errorCode").asText());
    }

    @Test
    @DisplayName("회원 탈퇴 - 회원 없음 → 404 + USER_NOT_FOUND")
    void withdraw_userNotFound() throws Exception {
        // Given — JWT 인증된 로그인 사용자 + Service 가 회원 없음을 거부한다
        SecurityContextHolder.getContext().setAuthentication(new WorkitPrincipal(501L, "ROLE_USER"));
        stubWithdrawError(UserErrorCode.USER_NOT_FOUND);

        // When
        MvcResult result = mockMvc.perform(delete("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"user_password123!\"}"))
                .andExpect(status().isNotFound())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("USER_NOT_FOUND", json.get("errorCode").asText());
    }

    @Test
    @DisplayName("회원 탈퇴 - 인증 사용자 없음 → 401 + AUTH_TOKEN_NOT_FOUND")
    void withdraw_unauthenticated() throws Exception {
        // Given — SecurityContext 에 인증 객체가 없음 (CurrentUserArgumentResolver 가 401 처리)

        // When
        MvcResult result = mockMvc.perform(delete("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"user_password123!\"}"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        // Then — Service 호출 없이 401 응답
        verify(userService, never()).withdraw(anyLong(), any(UserWithdrawalRequestDTO.class));
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("AUTH_TOKEN_NOT_FOUND", json.get("errorCode").asText());
    }

    // ---------- 계정 설정 진입용 비밀번호 재인증 ----------

    @Test
    @DisplayName("비밀번호 재인증 성공 - 200 + SUCCESS + 확인 완료 메시지 + 토큰 Cookie 미변경")
    void verifyAccountPassword_success() throws Exception {
        // Given — JWT 인증된 로그인 사용자
        SecurityContextHolder.getContext().setAuthentication(new WorkitPrincipal(501L, "ROLE_USER"));

        // When — 현재 비밀번호 재확인 요청
        MvcResult result = mockMvc.perform(post("/api/v1/users/me/account/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"user_password123!\"}"))
                .andExpect(status().isOk())
                .andReturn();

        // Then — docs 응답: data null + 확인 완료 메시지
        //   (CommonResponse 는 NON_NULL 직렬화 — data 가 null 이면 JSON 에서 제외됨)
        JsonNode json = parse(result);
        assertEquals("SUCCESS", json.get("status").asText());
        assertEquals("비밀번호가 확인되었습니다.", json.get("message").asText());
        assertTrue(json.get("errorCode") == null || json.get("errorCode").isNull());
        assertTrue(json.get("data") == null || json.get("data").isNull());

        // Access/Refresh Token 은 새로 발급/변경되지 않는다 — Set-Cookie 응답 헤더가 없어야 한다
        assertTrue(result.getResponse().getHeaders("Set-Cookie").isEmpty(),
                "재인증 응답에는 Set-Cookie 가 없어야 한다 (토큰 재발급/변경 없음)");

        // Controller 는 userId 와 요청을 Service 로 위임만 한다 (비밀번호 비교/DB/Redis 금지)
        verify(userService).verifyAccountPassword(eq(501L), any(AccountPasswordVerifyRequestDTO.class));
    }

    @Test
    @DisplayName("비밀번호 재인증 - 비밀번호 불일치 → 400 + AUTH_INVALID_PASSWORD")
    void verifyAccountPassword_wrongPassword() throws Exception {
        // Given — JWT 인증된 로그인 사용자 + Service 가 현재 비밀번호 불일치를 거부한다
        SecurityContextHolder.getContext().setAuthentication(new WorkitPrincipal(501L, "ROLE_USER"));
        stubVerifyAccountPasswordError(AuthErrorCode.AUTH_INVALID_PASSWORD);

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/users/me/account/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"wrong-password!\"}"))
                .andExpect(status().isBadRequest())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("AUTH_INVALID_PASSWORD", json.get("errorCode").asText());
        assertEquals("현재 비밀번호가 올바르지 않습니다.", json.get("message").asText());
    }

    @Test
    @DisplayName("비밀번호 재인증 - password 누락 → 400 + COMMON_INVALID_REQUEST")
    void verifyAccountPassword_missingPassword() throws Exception {
        // Given — JWT 인증된 로그인 사용자 + Service 가 필수 값 누락을 거부한다
        SecurityContextHolder.getContext().setAuthentication(new WorkitPrincipal(501L, "ROLE_USER"));
        stubVerifyAccountPasswordError(CommonErrorCode.COMMON_INVALID_REQUEST);

        // When — password 없는 본문
        MvcResult result = mockMvc.perform(post("/api/v1/users/me/account/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("COMMON_INVALID_REQUEST", json.get("errorCode").asText());
        assertEquals("요청 값이 올바르지 않습니다.", json.get("message").asText());
    }

    @Test
    @DisplayName("비밀번호 재인증 - password 빈 문자열/공백 → 400 + COMMON_INVALID_REQUEST")
    void verifyAccountPassword_blankPassword() throws Exception {
        // Given — JWT 인증된 로그인 사용자 + Service 가 공백 비밀번호를 거부한다
        SecurityContextHolder.getContext().setAuthentication(new WorkitPrincipal(501L, "ROLE_USER"));
        stubVerifyAccountPasswordError(CommonErrorCode.COMMON_INVALID_REQUEST);

        // When — 빈 문자열 본문
        MvcResult result = mockMvc.perform(post("/api/v1/users/me/account/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("COMMON_INVALID_REQUEST", json.get("errorCode").asText());
    }

    @Test
    @DisplayName("비밀번호 재인증 - 이미 탈퇴한 사용자 → 409 + USER_ALREADY_WITHDRAWN")
    void verifyAccountPassword_alreadyWithdrawn() throws Exception {
        // Given — JWT 인증된 로그인 사용자 + Service 가 이미 탈퇴 상태를 거부한다
        SecurityContextHolder.getContext().setAuthentication(new WorkitPrincipal(501L, "ROLE_USER"));
        stubVerifyAccountPasswordError(UserErrorCode.USER_ALREADY_WITHDRAWN);

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/users/me/account/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"user_password123!\"}"))
                .andExpect(status().isConflict())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("USER_ALREADY_WITHDRAWN", json.get("errorCode").asText());
    }

    @Test
    @DisplayName("비밀번호 재인증 - 회원 없음 → 404 + USER_NOT_FOUND")
    void verifyAccountPassword_userNotFound() throws Exception {
        // Given — JWT 인증된 로그인 사용자 + Service 가 회원 없음을 거부한다
        SecurityContextHolder.getContext().setAuthentication(new WorkitPrincipal(501L, "ROLE_USER"));
        stubVerifyAccountPasswordError(UserErrorCode.USER_NOT_FOUND);

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/users/me/account/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"user_password123!\"}"))
                .andExpect(status().isNotFound())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("USER_NOT_FOUND", json.get("errorCode").asText());
    }

    @Test
    @DisplayName("비밀번호 재인증 - 인증 사용자 없음 → 401 + AUTH_TOKEN_NOT_FOUND")
    void verifyAccountPassword_unauthenticated() throws Exception {
        // Given — SecurityContext 에 인증 객체가 없음 (CurrentUserArgumentResolver 가 401 처리)

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/users/me/account/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"user_password123!\"}"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        // Then — Service 호출 없이 401 응답
        verify(userService, never()).verifyAccountPassword(anyLong(), any(AccountPasswordVerifyRequestDTO.class));
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("AUTH_TOKEN_NOT_FOUND", json.get("errorCode").asText());
    }

    // ---------- 휴대폰 번호 변경 ----------

    @Test
    @DisplayName("휴대폰 번호 변경 성공 - 200 + SUCCESS + 변경 완료 메시지 + updatedPhone 반환 + 토큰 Cookie 미변경")
    void changePhone_success() throws Exception {
        // Given — JWT 인증된 로그인 사용자 + Service 가 변경된 번호를 반환한다
        SecurityContextHolder.getContext().setAuthentication(new WorkitPrincipal(501L, "ROLE_USER"));
        stubChangePhoneSuccess();

        // When — Mock PASS 인증 후 발급된 identityVerificationId 만 전달 (phoneNumber 는 Request 에 없음)
        MvcResult result = mockMvc.perform(patch("/api/v1/users/me/phone")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identityVerificationId\":\"identity-verification-id\"}"))
                .andExpect(status().isOk())
                .andReturn();

        // Then — docs 응답: data.updatedPhone + 변경 완료 메시지
        JsonNode json = parse(result);
        assertEquals("SUCCESS", json.get("status").asText());
        assertEquals("휴대폰 번호가 변경되었습니다.", json.get("message").asText());
        assertTrue(json.get("errorCode") == null || json.get("errorCode").isNull());

        JsonNode data = json.get("data");
        assertNotNull(data);
        assertEquals("01098765432", data.get("updatedPhone").asText());

        // Access/Refresh Token 은 새로 발급/변경되지 않는다 — Set-Cookie 응답 헤더가 없어야 한다
        assertTrue(result.getResponse().getHeaders("Set-Cookie").isEmpty(),
                "휴대폰 번호 변경 응답에는 Set-Cookie 가 없어야 한다 (토큰 재발급/변경 없음)");

        // Controller 는 userId 와 요청을 Service 로 위임만 한다 (PASS 검증/DB 접근 금지)
        verify(userService).changePhone(eq(501L), any(PhoneChangeRequestDTO.class));
    }

    @Test
    @DisplayName("휴대폰 번호 변경 - identityVerificationId 유효하지 않음(누락/만료/미인증) → 400 + INVALID_VERIFICATION_ID")
    void changePhone_invalidVerificationId() throws Exception {
        // Given — JWT 인증된 로그인 사용자 + Service 가 PASS 인증 검증 실패를 거부한다
        SecurityContextHolder.getContext().setAuthentication(new WorkitPrincipal(501L, "ROLE_USER"));
        stubChangePhoneError(AuthErrorCode.INVALID_VERIFICATION_ID);

        // When — identityVerificationId 누락 본문
        MvcResult result = mockMvc.perform(patch("/api/v1/users/me/phone")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("INVALID_VERIFICATION_ID", json.get("errorCode").asText());
        assertEquals("PASS 인증이 유효하지 않습니다. 다시 시도해주세요.", json.get("message").asText());
    }

    @Test
    @DisplayName("휴대폰 번호 변경 - 다른 사용자에게 발급된 identityVerificationId(이름 불일치) → 400 + VERIFICATION_FAILED")
    void changePhone_verificationFailed() throws Exception {
        // Given — JWT 인증된 로그인 사용자 + Service 가 본인 인증 불일치를 거부한다
        SecurityContextHolder.getContext().setAuthentication(new WorkitPrincipal(501L, "ROLE_USER"));
        stubChangePhoneError(AuthErrorCode.VERIFICATION_FAILED);

        // When
        MvcResult result = mockMvc.perform(patch("/api/v1/users/me/phone")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identityVerificationId\":\"other-user-verification-id\"}"))
                .andExpect(status().isBadRequest())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("VERIFICATION_FAILED", json.get("errorCode").asText());
    }

    @Test
    @DisplayName("휴대폰 번호 변경 - 인증된 번호가 현재 번호와 동일 → 400 + PHONE_SAME_AS_CURRENT")
    void changePhone_samePhone() throws Exception {
        // Given — JWT 인증된 로그인 사용자 + Service 가 동일 번호 변경을 거부한다
        SecurityContextHolder.getContext().setAuthentication(new WorkitPrincipal(501L, "ROLE_USER"));
        stubChangePhoneError(UserErrorCode.PHONE_SAME_AS_CURRENT);

        // When
        MvcResult result = mockMvc.perform(patch("/api/v1/users/me/phone")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identityVerificationId\":\"identity-verification-id\"}"))
                .andExpect(status().isBadRequest())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("PHONE_SAME_AS_CURRENT", json.get("errorCode").asText());
        assertEquals("현재 휴대폰 번호와 동일한 번호로는 변경할 수 없습니다.", json.get("message").asText());
    }

    @Test
    @DisplayName("휴대폰 번호 변경 - 다른 사용자가 사용 중인 번호 → 409 + PHONE_ALREADY_IN_USE")
    void changePhone_alreadyInUse() throws Exception {
        // Given — JWT 인증된 로그인 사용자 + Service 가 번호 중복을 거부한다
        SecurityContextHolder.getContext().setAuthentication(new WorkitPrincipal(501L, "ROLE_USER"));
        stubChangePhoneError(UserErrorCode.PHONE_ALREADY_IN_USE);

        // When
        MvcResult result = mockMvc.perform(patch("/api/v1/users/me/phone")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identityVerificationId\":\"identity-verification-id\"}"))
                .andExpect(status().isConflict())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("PHONE_ALREADY_IN_USE", json.get("errorCode").asText());
        assertEquals("이미 사용 중인 휴대폰 번호입니다.", json.get("message").asText());
    }

    @Test
    @DisplayName("휴대폰 번호 변경 - 이미 탈퇴한 사용자 → 409 + USER_ALREADY_WITHDRAWN")
    void changePhone_alreadyWithdrawn() throws Exception {
        // Given — JWT 인증된 로그인 사용자 + Service 가 탈퇴 회원 변경을 거부한다
        SecurityContextHolder.getContext().setAuthentication(new WorkitPrincipal(501L, "ROLE_USER"));
        stubChangePhoneError(UserErrorCode.USER_ALREADY_WITHDRAWN);

        // When
        MvcResult result = mockMvc.perform(patch("/api/v1/users/me/phone")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identityVerificationId\":\"identity-verification-id\"}"))
                .andExpect(status().isConflict())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("USER_ALREADY_WITHDRAWN", json.get("errorCode").asText());
    }

    @Test
    @DisplayName("휴대폰 번호 변경 - 회원 없음 → 404 + USER_NOT_FOUND")
    void changePhone_userNotFound() throws Exception {
        // Given — JWT 인증된 로그인 사용자 + Service 가 회원 없음을 거부한다
        SecurityContextHolder.getContext().setAuthentication(new WorkitPrincipal(501L, "ROLE_USER"));
        stubChangePhoneError(UserErrorCode.USER_NOT_FOUND);

        // When
        MvcResult result = mockMvc.perform(patch("/api/v1/users/me/phone")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identityVerificationId\":\"identity-verification-id\"}"))
                .andExpect(status().isNotFound())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("USER_NOT_FOUND", json.get("errorCode").asText());
    }

    @Test
    @DisplayName("휴대폰 번호 변경 - 인증 사용자 없음 → 401 + AUTH_TOKEN_NOT_FOUND")
    void changePhone_unauthenticated() throws Exception {
        // Given — SecurityContext 에 인증 객체가 없음 (CurrentUserArgumentResolver 가 401 처리)

        // When
        MvcResult result = mockMvc.perform(patch("/api/v1/users/me/phone")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identityVerificationId\":\"identity-verification-id\"}"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        // Then — Service 호출 없이 401 응답
        verify(userService, never()).changePhone(anyLong(), any(PhoneChangeRequestDTO.class));
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("AUTH_TOKEN_NOT_FOUND", json.get("errorCode").asText());
    }
}
