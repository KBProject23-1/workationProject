package com.workit.domain.user.dto.response;

import lombok.Builder;
import lombok.Getter;

// 로그인 사용자 휴대폰 번호 변경 응답 DTO
// API 스펙(docs): PATCH /api/v1/users/me/phone → data { updatedPhone }
//
// 개인정보 규칙 (knowledge.md):
// - updatedPhone 은 PASS 본인인증 결과에서 조회해 Service 가 복호화한 인증된 휴대폰 번호
//   (프론트가 전달한 번호가 아닌 백엔드 검증 결과 값)
@Getter
@Builder
public class PhoneChangeResponseDTO {

    /** PASS 인증을 통해 변경된 휴대폰 번호 */
    private String updatedPhone;

    public static PhoneChangeResponseDTO of(String updatedPhone) {
        return PhoneChangeResponseDTO.builder()
                .updatedPhone(updatedPhone)
                .build();
    }
}
