package com.workit.domain.user.dto.response;

import lombok.Builder;
import lombok.Getter;

// 프로필 최초 등록 응답 DTO
// API 스펙(docs): POST /api/v1/users/me/onboarding → data { profileId, userId, nickname, companyName }
//
// - profileId: user_profile 테이블 PK (insert 후 생성된 값)
// - userId: 로그인 사용자 id
// - nickname/companyName: 최초 등록으로 저장된 값
@Getter
@Builder
public class ProfileOnboardingResponseDTO {

    /** 프로필 테이블 유저 id (user_profile.id) */
    private Long profileId;

    /** 유저 id (users.id) */
    private Long userId;

    /** 유저 닉네임 */
    private String nickname;

    /** 유저 회사 이름 */
    private String companyName;

    public static ProfileOnboardingResponseDTO of(Long profileId, Long userId,
                                                  String nickname, String companyName) {
        return ProfileOnboardingResponseDTO.builder()
                .profileId(profileId)
                .userId(userId)
                .nickname(nickname)
                .companyName(companyName)
                .build();
    }
}
