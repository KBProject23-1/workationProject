package com.workit.domain.auth.controller;

import com.workit.domain.auth.dto.response.TermsListResponseDTO;
import com.workit.domain.auth.service.AuthService;
import com.workit.global.dto.CommonResponse;
import com.workit.global.response.GlobalResponseFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
