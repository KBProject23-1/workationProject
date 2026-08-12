package com.workit.domain.auth.controller;

import com.workit.domain.auth.dto.request.MockPassCompleteRequestDTO;
import com.workit.domain.auth.dto.response.MockPassStatusResponseDTO;
import com.workit.domain.auth.service.MockPassService;
import com.workit.global.dto.CommonResponse;
import com.workit.global.response.GlobalResponseFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Mock PASS 본인인증 API (개발/테스트용)
//
// - 실제 PASS/PortOne SDK 연동 전까지 Mock 본인인증 UX 를 제공하는 임시 API
// - identityVerificationId 는 프론트가 생성하지 않는다 — 백엔드 MockPassService 가 생성하고
//   인증 성공 후 프론트에 발급한다 (프론트는 발급받은 ID 만 보관해 후속 API 에 전달)
// - 비로그인 공개 API: /api/v1/auth/** → SecurityPath.PUBLIC_AUTH_PATTERN(permitAll) 대상
// - Controller 에는 비즈니스 로직 없음 — Service 에서 검증/암호화/세션 저장을 수행
@RestController
@RequestMapping("/api/v1/auth/pass")
public class MockPassController {

    private final MockPassService mockPassService;

    public MockPassController(MockPassService mockPassService) {
        this.mockPassService = mockPassService;
    }

    // Mock 본인인증 처리 — 백엔드가 인증 세션 생성 + identityVerificationId 발급
    // - data: { identityVerificationId, status: VERIFIED } (개인정보 응답 미포함)
    @PostMapping
    public ResponseEntity<CommonResponse<MockPassStatusResponseDTO>> passComplete(
            @RequestBody MockPassCompleteRequestDTO request) {

        return GlobalResponseFactory.success(
                mockPassService.complete(request), "본인인증이 완료되었습니다.");
    }
}
