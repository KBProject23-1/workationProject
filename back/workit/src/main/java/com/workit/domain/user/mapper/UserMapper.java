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

    /**
     * 회원 탈퇴 - users 상태 WITHDRAWN 전환 + deleted_at 기록 (Soft Delete)
     * - status = 'WITHDRAWN', deleted_at = 탈퇴 시각 (NOW()), updated_at 갱신
     * - WHERE status != 'WITHDRAWN' — 이미 탈퇴된 회원은 갱신 대상에서 제외
     *   (조회(SELECT)와 갱신(UPDATE) 사이 동시 요청으로 인한 중복 탈퇴 Race Condition 방어선)
     * - 금융 거래/결제/지갑 데이터는 삭제하지 않는다 (UserMapper 는 users 테이블만 담당)
     *
     * @param userId 탈퇴 처리할 회원 번호
     * @return 갱신된 row 수 (0 이면 이미 WITHDRAWN 상태)
     */
    int updateUserStatusToWithdrawn(@Param("userId") Long userId);

    /**
     * 휴대폰 번호 변경 - 휴대폰 번호(SHA-256 hash) 중복 조회 (자기 자신 제외)
     * - users.phone_number_hash (UNIQUE) 대상
     * - PASS 인증된 휴대폰 번호가 다른 사용자에게 이미 등록되어 있는지 확인
     *   (개인정보 원문(phone_number_encrypt)은 조회하지 않는다 — knowledge.md: 검색용 hash)
     * - 반환값이 0 초과면 다른 사용자가 사용 중인 번호 (PHONE_ALREADY_IN_USE 판단은 Service)
     *
     * @param phoneHash 변경할 휴대폰 번호의 SHA-256 hash (Service Layer 에서 생성)
     * @param userId    변경을 요청한 사용자 — 본인은 중복 대상에서 제외한다
     */
    int countByPhoneHashExcludingUserId(@Param("phoneHash") String phoneHash, @Param("userId") Long userId);

    /**
     * 휴대폰 번호 변경 - users 휴대폰 번호 갱신 (AES 암호화본 + 검색용 SHA-256 hash)
     * - phone_number_encrypt: AES-256 암호화본 (Service Layer 에서 암호화 후 전달 — Mapper 에서 암호화 금지)
     * - phone_number_hash: SHA-256 해시 (knowledge.md: 검색용 개인정보는 hash)
     * - WHERE status != 'WITHDRAWN' — 조회-갱신 사이 동시 탈퇴(Race Condition) 시 0 row 반환
     *   (USER_ALREADY_WITHDRAWN 최종 방어선 — updateUserStatusToWithdrawn 과 동일 패턴)
     *
     * @param userId             변경할 회원 번호
     * @param phoneNumberHash    변경할 휴대폰 번호의 SHA-256 hash
     * @param phoneNumberEncrypt 변경할 휴대폰 번호의 AES-256 암호화본
     * @return 갱신된 row 수 (0 이면 이미 WITHDRAWN 상태)
     */
    int updateUserPhoneNumber(@Param("userId") Long userId,
                              @Param("phoneNumberHash") String phoneNumberHash,
                              @Param("phoneNumberEncrypt") String phoneNumberEncrypt);

    /**
     * 이메일 인증번호 발송 - 이메일(SHA-256 hash) 중복 조회 (자기 자신 제외)
     * - users.email_hash (UNIQUE) 대상
     * - 인증번호를 받을 이메일이 다른 사용자에게 이미 등록되어 있는지 확인
     *   (개인정보 원문(email_encrypt)은 조회하지 않는다 — knowledge.md: 검색용 hash,
     *    AuthMapper.countByEmailHash 와 동일 원칙)
     * - 반환값이 0 초과면 다른 사용자가 사용 중인 이메일 (EMAIL_ALREADY_IN_USE 판단은 Service)
     *
     * @param emailHash 인증번호를 받을 이메일의 SHA-256 hash (Service Layer 에서 생성)
     * @param userId    인증번호 발송을 요청한 사용자 — 본인은 중복 대상에서 제외한다
     */
    int countByEmailHashExcludingUserId(@Param("emailHash") String emailHash, @Param("userId") Long userId);
}
