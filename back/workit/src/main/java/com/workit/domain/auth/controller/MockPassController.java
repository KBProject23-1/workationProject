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
// - 프론트가 생성한 identityVerificationId 를 VERIFIED 세션으로 등록한다.
//   (Mock 인증 완료는 팝업 UX 로 프론트가 흉내 내지만, 이 등록 없이는
//    verify-identity(/auth/signup/verify-identity) 가 거부하므로 인증 성공 여부는 백엔드가 관리)
// - 비로그인 공개 API: /api/v1/auth/** → SecurityPath.PUBLIC_AUTH_PATTERN(permitAll) 대상
// - Controller 에는 비즈니스 로직 없음 — Service 에서 검증/암호화/세션 저장을 수행
@RestController
@RequestMapping("/api/v1/auth/pass")
public class MockPassController {

    private final MockPassService mockPassService;

    public MockPassController(MockPassService mockPassService) {
        this.mockPassService = mockPassService;
    }

    // Mock 인증 완료 등록 — 프론트가 생성한 identityVerificationId 를 VERIFIED 세션으로 저장
    // - data: { identityVerificationId, status: VERIFIED, name }
    @PostMapping
    public ResponseEntity<CommonResponse<MockPassStatusResponseDTO>> passComplete(
            @RequestBody MockPassCompleteRequestDTO request) {

        return GlobalResponseFactory.success(
                mockPassService.complete(request), "본인인증이 완료되었습니다.");
    }
}
