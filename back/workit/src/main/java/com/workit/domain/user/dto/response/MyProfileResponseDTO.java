package com.workit.domain.user.dto.response;

import lombok.Builder;
import lombok.Getter;

// 내 프로필 조회 응답 DTO
// API 스펙(docs): GET /api/v1/users/me → data { email, name, phoneNumber, nickname, companyName }
//
// 개인정보 규칙 (knowledge.md):
// - name/phoneNumber 는 회원가입 시 PASS 본인인증으로 저장된 users 테이블 값을 Service 에서 복호화해 반환
// - email 은 회원가입 시 입력한 이메일을 복호화해 반환
// - 최초 등록 전: nickname/companyName 은 null 가능
@Getter
@Builder
public class MyProfileResponseDTO {

    /** 로그인 ID(이메일) — 복호화 값 */
    private String email;

    /** 실명 — PASS 본인인증 결과 복호화 값 */
    private String name;

    /** 휴대폰 번호 — PASS 본인인증 결과 복호화 값 */
    private String phoneNumber;

    /** 닉네임 (최초 등록 전 null) */
    private String nickname;

    /** 소속 회사명 (최초 등록 전 null) */
    private String companyName;

    public static MyProfileResponseDTO of(String email, String name, String phoneNumber,
                                          String nickname, String companyName) {
        return MyProfileResponseDTO.builder()
                .email(email)
                .name(name)
                .phoneNumber(phoneNumber)
                .nickname(nickname)
                .companyName(companyName)
                .build();
    }
}
