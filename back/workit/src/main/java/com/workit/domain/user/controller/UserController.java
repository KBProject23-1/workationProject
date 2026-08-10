package com.workit.domain.user.controller;

import com.workit.domain.auth.dto.request.ChangePasswordRequestDTO;
import com.workit.domain.auth.service.AuthService;
import com.workit.domain.user.dto.request.ProfileOnboardingRequestDTO;
import com.workit.domain.user.dto.request.ProfileUpdateRequestDTO;
import com.workit.domain.user.dto.response.MyProfileResponseDTO;
import com.workit.domain.user.dto.response.ProfileOnboardingResponseDTO;
import com.workit.domain.user.service.UserService;
import com.workit.global.dto.CommonResponse;
import com.workit.global.response.GlobalResponseFactory;
import com.workit.security.CurrentUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// User 도메인 컨트롤러 (회원 기본 정보 / 프로필 담당)
// - 로그인 사용자 전용 API: JWT 인증 필터 + @CurrentUser 로 userId 를 주입받는다
//   (인증 없이 접근하면 AUTH_TOKEN_NOT_FOUND 401 — CurrentUserArgumentResolver)
// - Controller 는 요청 수신과 CommonResponse 반환만 담당 (DB 조회/복호화/저장 금지 — 전부 Service 책임)
@RestController
@RequestMapping("/api/v1/users")
@Slf4j
public class UserController {

    private final UserService userService;

    // 비밀번호 변경은 인증(Password 검증/변경, Refresh Token 폐기)의 책임이므로
    // Auth Domain 의 AuthService 로 위임한다 (knowledge.md: Auth Domain 책임 — User Domain 에 인증 로직 금지)
    private final AuthService authService;

    public UserController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    // 1.1 내 프로필 조회
    // - docs: 내 프로필 정보 조회 (GET /api/v1/users/me)
    // - 로그인 사용자 전용 API: JWT 인증 + @CurrentUser 로 userId 주입
    // - Service 에서 회원 조회/개인정보 복호화를 수행하고, Controller 는 요청 수신과 응답 반환만 담당한다
    //   (Controller 에서 DB 조회/복호화 금지)
    // - 회원 없음/비활성: USER_NOT_FOUND(404), 최초 등록 전 nickname/companyName 은 null
    @GetMapping("/me")
    public ResponseEntity<CommonResponse<MyProfileResponseDTO>> getMyProfile(
            @CurrentUser Long userId) {

        return GlobalResponseFactory.success(
                userService.getMyProfile(userId),
                "프로필 정보를 성공적으로 조회했습니다.");
    }

    // 1.2 프로필 최초 등록
    // - docs: 유저 프로필 작성 - 프로필 정보 최종 저장 (POST /api/v1/users/me/onboarding)
    // - 로그인 사용자 전용 API: JWT 인증 + @CurrentUser 로 userId 주입
    // - name/phoneNumber 는 Request Body 에서 받지 않는다 — DB 에 저장된 본인인증 정보를 그대로 사용
    // - Service 에서 요청 검증/회원 확인/프로필 기등록 확인/닉네임 중복 확인/insert 를 수행하고,
    //   Controller 는 요청 수신과 CommonResponse 반환만 담당한다 (DB 접근/검증 금지)
    // - 잘못된 요청: INVALID_PROFILE_REQUEST(400), 회원 없음: USER_NOT_FOUND(404),
    //   이미 등록: PROFILE_ALREADY_EXISTS(409), 닉네임 중복: DUPLICATE_NICKNAME(409)
    @PostMapping("/me/onboarding")
    public ResponseEntity<CommonResponse<ProfileOnboardingResponseDTO>> onboardProfile(
            @CurrentUser Long userId,
            @RequestBody ProfileOnboardingRequestDTO request) {

        return GlobalResponseFactory.created(
                userService.onboardProfile(userId, request),
                "유저 프로필 정보가 성공적으로 등록되었습니다.");
    }

    // 1.3 내 프로필 수정
    // - docs: 내 프로필 정보 수정 (PATCH /api/v1/users/me)
    // - 로그인 사용자 전용 API: JWT 인증 + @CurrentUser 로 userId 주입
    // - PATCH 방식 — nickname/companyName 중 전달된 값만 수정 (name/phoneNumber/email 은 수정 불가,
    //   재인증 API 경유 개인정보만 변경 가능 — knowledge.md)
    // - Service 에서 사용자/프로필 확인/요청 검증/닉네임 중복 확인/동적 UPDATE 를 수행하고,
    //   Controller 는 요청 수신과 CommonResponse 반환만 담당한다 (DB 접근/검증 금지)
    // - 회원 없음: USER_NOT_FOUND(404), 프로필 미등록: PROFILE_NOT_FOUND(404),
    //   잘못된 요청: INVALID_PROFILE_REQUEST(400), 닉네임 중복: DUPLICATE_NICKNAME(409)
    @PatchMapping("/me")
    public ResponseEntity<CommonResponse<Void>> updateProfile(
            @CurrentUser Long userId,
            @RequestBody ProfileUpdateRequestDTO request) {

        userService.updateProfile(userId, request);
        return GlobalResponseFactory.success(null, "프로필 정보가 성공적으로 수정되었습니다.");
    }

    // 1.4 내 비밀번호 변경 (로그인 사용자 전용 — Auth 도메인 책임)
    // - docs: 유저 개인정보 재설정 - 비밀번호 변경 (PATCH /api/v1/users/me/password)
    // - 로그인 사용자 전용 API: JWT 인증 + @CurrentUser 로 userId 주입
    //   (인증 없이 접근하면 AUTH_TOKEN_NOT_FOUND 401 — CurrentUserArgumentResolver)
    // - 현재 비밀번호 재입력 본인 인증/BCrypt 검증·암호화/DB 갱신/Refresh Token 전체 폐기/Audit 로그는
    //   AuthService(changePassword) 에서 수행한다 — User Domain 에 인증 로직을 구현하지 않는다
    //   (knowledge.md: Auth Domain 이 Password 검증/변경을 담당)
    // - Controller 는 요청 수신과 CommonResponse 반환만 담당한다 (암호화/DB/Redis 접근 금지)
    // - 현재 비밀번호 불일치: AUTH_INVALID_PASSWORD(400), 동일 비밀번호: AUTH_SAME_PASSWORD(400),
    //   약한 비밀번호: WEAK_PASSWORD(422), 요청 값 누락: INVALID_PASSWORD_CHANGE_REQUEST(400),
    //   회원 없음: USER_NOT_FOUND(404)
    @PatchMapping("/me/password")
    public ResponseEntity<CommonResponse<Void>> changePassword(
            @CurrentUser Long userId,
            @RequestBody ChangePasswordRequestDTO request) {

        authService.changePassword(userId, request);
        return GlobalResponseFactory.success(null, "비밀번호가 성공적으로 변경되었습니다.");
    }
}
