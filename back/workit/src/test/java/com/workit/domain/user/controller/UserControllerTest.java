package com.workit.domain.user.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workit.domain.user.dto.request.ProfileOnboardingRequestDTO;
import com.workit.domain.user.dto.response.MyProfileResponseDTO;
import com.workit.domain.user.dto.response.ProfileOnboardingResponseDTO;
import com.workit.domain.user.exception.UserErrorCode;
import com.workit.domain.user.service.UserService;
import com.workit.exception.BusinessException;
import com.workit.exception.CommonExceptionAdvice;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userService))
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
}
