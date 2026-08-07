package com.workit.domain.user.mapper;

import com.workit.domain.user.vo.MyProfileVO;
import com.workit.domain.user.vo.UserProfileVO;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;

// User 도메인 Mapper (users / user_profile 테이블 담당)
// - SQL 은 XML 매퍼(com/workit/domain/user/mapper/UserMapper.xml)에 작성한다 (MyBatis 규칙)
// - Mapper 에 비즈니스 로직 금지 — 조회/저장만 담당 (상태 판단/복호화/중복 검증은 Service)
public interface UserMapper {

    /**
     * 내 프로필 조회 - 로그인 사용자 기본 정보 + 프로필 정보 함께 조회
     * - users + user_profile LEFT JOIN (프로필 미등록 회원은 nickname/companyName 이 null)
     * - email_encrypt/name_encrypt/phone_number_encrypt 는 AES 암호화본이므로 SELECT 가능 —
     *   복호화는 Service Layer 에서만 수행 (knowledge.md)
     * - status 도 함께 조회 — 탈퇴/차단 등 비활성 회원 여부는 Service 에서 판단 (Mapper 비즈니스 로직 금지)
     *
     * @return 매칭되는 회원이 없으면 null
     */
    MyProfileVO selectMyProfileByUserId(@Param("userId") Long userId);

    /**
     * 프로필 최초 등록 - 닉네임 중복 조회
     * - user_profile.nickname (UNIQUE) 대상
     * - 반환값이 0 초과면 이미 사용 중인 닉네임 (DUPLICATE_NICKNAME 판단은 Service)
     */
    int countByNickname(@Param("nickname") String nickname);

    /**
     * 프로필 최초 등록 - user_profile insert
     * - 자동 생성 PK 는 UserProfileVO.id 로 채워진다 (useGeneratedKeys)
     * - nickname/companyName 은 Service Layer 에서 검증 후 전달 (Mapper 에서 검증 금지)
     */
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertUserProfile(UserProfileVO userProfile);

    /**
     * 프로필 수정 - 닉네임 중복 조회 (자기 자신 제외)
     * - user_profile.nickname (UNIQUE) 대상
     * - 변경 요청 닉네임이 다른 사용자의 프로필에서 이미 사용 중인지 확인
     * - 반환값이 0 초과면 다른 사용자가 사용 중인 닉네임 (DUPLICATE_NICKNAME 판단은 Service)
     *
     * @param userId 변경을 요청한 사용자 — 본인 프로필은 중복 대상에서 제외한다
     */
    int countByNicknameExcludingUserId(@Param("nickname") String nickname, @Param("userId") Long userId);

    /**
     * 프로필 수정 - user_profile 부분 수정 (PATCH: 전달된 값만 UPDATE)
     * - nickname/companyName 중 null 이 아닌 필드만 수정한다 (동적 UPDATE — Mapper XML)
     * - nickname/companyName 은 Service Layer 에서 검증/trim 후 전달 (Mapper 에서 검증 금지)
     * - updated_at 은 DB 기본값(ON UPDATE CURRENT_TIMESTAMP)으로 자동 갱신
     *
     * @return UPDATE 된 row 수 (0 이면 대상 프로필 없음)
     */
    int updateUserProfile(UserProfileVO userProfile);
}
