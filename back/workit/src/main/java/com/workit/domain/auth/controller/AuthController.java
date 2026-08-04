package com.workit.domain.auth.controller;

import com.workit.domain.auth.dto.request.VerifyIdentityRequestDTO;
import com.workit.domain.auth.dto.response.EmailAvailabilityResponseDTO;
import com.workit.domain.auth.dto.response.IdentityVerificationResponseDTO;
import com.workit.domain.auth.dto.response.TermsListResponseDTO;
import com.workit.domain.auth.service.AuthService;
import com.workit.global.dto.CommonResponse;
import com.workit.global.response.GlobalResponseFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 인증 도메인 컨트롤러
// 약관 조회는 비로그인 접근 가능한 공개 API (회원가입 화면에서 호출)
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    // 1.1 필수/선택 약관 목록 조회
    @GetMapping("/terms")
    public ResponseEntity<CommonResponse<TermsListResponseDTO>> termsListGet() {

        return GlobalResponseFactory.success(
                authService.getTermsList(), "약관 목록 조회가 완료되었습니다.");
    }

    // 1.2 회원가입 이메일 중복 확인
    // - docs: 이메일 중복 확인 (GET /api/v1/auth/signup/check-email?email=...)
    // - 비로그인 공개 API: 회원가입 화면에서 입력한 이메일의 사용 가능 여부를 실시간으로 조회
    // - 중복 여부는 200 SUCCESS + data.available 로 반환 (available=false 시 프론트에서 가입 진행 차단)
    // - required=false: email 누락 시 Service Layer 에서 AuthErrorCode.INVALID_EMAIL_FORMAT 로 처리
    @GetMapping("/signup/check-email")
    public ResponseEntity<CommonResponse<EmailAvailabilityResponseDTO>> checkEmailGet(
            @RequestParam(value = "email", required = false) String email) {

        EmailAvailabilityResponseDTO result = authService.checkEmailAvailability(email);

        return GlobalResponseFactory.success(result,
                result.isAvailable() ? "사용 가능한 이메일입니다." : "이미 사용 중인 이메일입니다.");
    }

    // 1.3 PASS 본인인증 검증 및 회원 중복 체크 (회원가입 1단계)
    // - docs: 본인인증 검증 및 회원 중복 체크 (POST /api/v1/auth/signup/verify-identity)
    // - 비로그인 공개 API: 회원가입 화면에서 PASS 인증 완료 후 호출
    @PostMapping("/signup/verify-identity")
    public ResponseEntity<CommonResponse<IdentityVerificationResponseDTO>> verifyIdentityPost(
            @RequestBody VerifyIdentityRequestDTO request) {

        return GlobalResponseFactory.success(
                authService.verifyIdentity(request.getIdentityVerificationId()),
                "본인인증 성공. 가입을 진행합니다.");
    }
}
