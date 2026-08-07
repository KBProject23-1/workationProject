package com.workit.domain.user.service;

import com.workit.domain.user.dto.request.ProfileOnboardingRequestDTO;
import com.workit.domain.user.dto.request.ProfileUpdateRequestDTO;
import com.workit.domain.user.dto.response.MyProfileResponseDTO;
import com.workit.domain.user.dto.response.ProfileOnboardingResponseDTO;

// User 도메인 Service (회원 기본 정보 / 프로필 / 회원 상태 담당 — knowledge.md User Domain 책임)
public interface UserService {

    /**
     * 내 프로필 조회 — 로그인 사용자의 기본 정보 + 프로필 정보 조회
     *
     * 흐름:
     *   1. 로그인 사용자(users + user_profile LEFT JOIN) 조회 — 없음/비활성 → USER_NOT_FOUND(404)
     *   2. email/name/phoneNumber AES 복호화 (Service Layer 에서만 — Controller/Mapper 금지)
     *   3. nickname/companyName 반환 (프로필 미등록 시 null)
     *
     * @param userId JWT 인증된 로그인 사용자 id (@CurrentUser — Controller 에서 주입)
     * @return email, name, phoneNumber, nickname, companyName
     */
    MyProfileResponseDTO getMyProfile(Long userId);

    /**
     * 프로필 최초 등록 — 회원가입 및 보안 PIN 설정 완료 후 nickname/companyName 최초 저장
     *
     * 흐름:
     *   1. 요청 값 검증 — nickname 필수/길이(50자), companyName 길이(100자) → INVALID_PROFILE_REQUEST(400)
     *   2. 로그인 사용자 존재 + ACTIVE 상태 확인 → USER_NOT_FOUND(404)
     *   3. 이미 프로필 등록 확인 (user_profile 1:1) → PROFILE_ALREADY_EXISTS(409)
     *   4. nickname 중복 확인 (user_profile.nickname UNIQUE) → DUPLICATE_NICKNAME(409)
     *   5. user_profile insert → ProfileOnboardingResponseDTO 반환 (profileId 포함)
     *
     * name/phoneNumber 는 요청에서 받지 않는다 — DB 에 저장된 본인인증 정보를 그대로 사용
     *
     * @param userId  JWT 인증된 로그인 사용자 id (@CurrentUser — Controller 에서 주입)
     * @param request 프로필 최초 등록 요청 (nickname 필수, companyName 선택)
     * @return profileId, userId, nickname, companyName
     */
    ProfileOnboardingResponseDTO onboardProfile(Long userId, ProfileOnboardingRequestDTO request);

    /**
     * 프로필 수정 — 로그인 사용자의 nickname/companyName 부분 수정 (PATCH: 전달된 값만)
     *
     * 흐름:
     *   1. 로그인 사용자 존재 + ACTIVE 상태 확인 → USER_NOT_FOUND(404)
     *   2. 프로필 존재 확인 (최초 등록 전 사용자는 수정 불가) → PROFILE_NOT_FOUND(404)
     *   3. 요청 값 검증 — 수정 대상 필드 최소 1개, nickname 길이(50자), companyName 길이(100자)
     *      → INVALID_PROFILE_REQUEST(400)
     *   4. nickname 중복 확인 — 기존 nickname 과 동일하면 허용, 변경 시 다른 사용자 중복 조회
     *      → DUPLICATE_NICKNAME(409)
     *   5. 전달된 필드만 동적 UPDATE (name/phoneNumber/email 은 수정 불가 — 요청에서 받지 않음)
     *
     * name/phoneNumber/email 은 PASS 본인인증 기반 기존 값 유지 (재인증 API 경유 — knowledge.md)
     *
     * @param userId  JWT 인증된 로그인 사용자 id (@CurrentUser — Controller 에서 주입)
     * @param request 프로필 수정 요청 (nickname/companyName 중 하나 이상)
     */
    void updateProfile(Long userId, ProfileUpdateRequestDTO request);
}
