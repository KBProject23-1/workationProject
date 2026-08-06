package com.workit.domain.auth.mapper;

import com.workit.domain.auth.vo.LoginUserVO;
import com.workit.domain.auth.vo.TermsVO;
import com.workit.domain.auth.vo.UserAuthVO;
import com.workit.domain.auth.vo.UserProfileVO;
import com.workit.domain.auth.vo.UserVO;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AuthMapper {

    /**
     * 약관 목록 전체 조회 (필수 → 선택 순)
     * - 회원가입 화면 및 서비스 내 약관 노출에 사용
     */
    List<TermsVO> selectTermsList();

    /**
     * 필수 약관 ID 목록 조회 (terms.required = 1)
     * - 회원가입 완료 시 agreedTermsIds 에 전부 포함되어 있는지 검증하는 기준
     * - DB 의 terms 마스터를 기준으로 판단하므로 약관이 추가/변경되어도 자동 반영된다
     */
    List<Long> selectRequiredTermsIds();

    /**
     * 주어진 약관 ID 중 terms 테이블에 실제로 존재하는 ID 목록 조회
     * - 회원가입 완료 시 agreedTermsIds 에 존재하지 않는 약관 ID 가 섞여 있으면
     *   user_terms_agreements FK 위반(500) 대신 Service 에서 INVALID_TERM_ID(400) 로 차단한다
     */
    List<Long> selectExistingTermIds(@Param("termIds") List<Long> termIds);

    /**
     * CI(SHA-256 해시) 기준 중복 가입 조회
     * - user_auth.identity_ci_hash (UNIQUE) 대상
     * - 반환값이 0 초과면 이미 가입된 회원
     */
    int countByCiHash(String ciHash);

    /**
     * 아이디 찾기 - CI(SHA-256 해시) 기준 가입 회원 조회
     * - user_auth.identity_ci_hash (UNIQUE) + users JOIN
     * - email_encrypt 는 AES 암호화본이므로 SELECT 가능 — 복호화는 Service Layer 에서만 수행
     *   (원문 컬럼은 존재하지 않으며, CI 원문으로 조회하지 않는다 — knowledge.md)
     * - status 도 함께 조회 — 탈퇴/차단 등 비활성 회원 여부는 Service 에서 판단 (Mapper 비즈니스 로직 금지)
     *
     * @return 매칭되는 회원이 없으면 null
     */
    UserVO selectUserByCiHash(String ciHash);

    /**
     * 이메일(SHA-256 해시) 기준 중복 가입 조회
     * - users.email_hash (UNIQUE) 대상
     * - 반환값이 0 초과면 이미 가입된 회원 → 사용 불가 이메일
     * - email_encrypt(원문 복호화)는 조회하지 않고 hash 만 사용 (보안 정책)
     */
    int countByEmailHash(String emailHash);

    /**
     * 닉네임 중복 가입 조회
     * - user_profile.nickname (UNIQUE) 대상
     * - 반환값이 0 초과면 이미 사용 중인 닉네임
     */
    int countByNickname(String nickname);

    /**
     * users 테이블 insert (회원가입 완료)
     * - 자동 생성 PK 는 UserVO.id 로 채워진다 (useGeneratedKeys)
     * - Mapper 는 저장만 담당 (암호화/해시는 Service)
     */
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertUser(UserVO user);

    /**
     * user_auth 테이블 insert (회원가입 완료)
     * - user_id 는 users insert 후 생성된 PK 를 사용한다
     */
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertUserAuth(UserAuthVO userAuth);

    /**
     * user_profile 테이블 insert (회원가입 완료 — nickname 저장)
     */
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertUserProfile(UserProfileVO userProfile);

    /**
     * user_terms_agreements 테이블 일괄 insert (회원가입 완료 — 약관 동의 저장)
     * - user_id 는 users insert 후 생성된 PK, termIds 는 유저가 동의한 약관 ID 목록
     * - agreed_at 은 DB 기본값(CURRENT_TIMESTAMP) 사용
     * - Mapper 는 저장만 담당 (필수 약관 검증은 Service)
     */
    int insertUserTerms(@Param("userId") Long userId, @Param("termIds") List<Long> termIds);

    /**
     * PASSWORD 로그인 - 이메일(SHA-256 hash) 기준 회원 + 비밀번호 hash 조회
     * - users.email_hash(UNIQUE) + user_auth JOIN (password_hash/identity_ci_hash 포함)
     * - email_encrypt(원문)은 조회하지 않는다 (knowledge.md: 검색용 hash 저장, 원문 조회 금지)
     * - identity_ci_hash 는 비밀번호 재설정(verify)의 CI 대조용으로 함께 조회한다
     *
     * @return 매칭되는 회원이 없으면 null
     */
    LoginUserVO findUserByEmailHash(String emailHash);

    /**
     * PASSWORD 로그인 - 휴대폰 번호(SHA-256 hash) 기준 회원 + 비밀번호 hash 조회
     * - users.phone_number_hash(UNIQUE) + user_auth JOIN (password_hash/identity_ci_hash 포함)
     * - phone_number_encrypt(원문)은 조회하지 않는다 (knowledge.md: 검색용 hash 저장, 원문 조회 금지)
     * - identity_ci_hash 는 비밀번호 재설정(verify)의 CI 대조용으로 함께 조회한다
     *
     * @return 매칭되는 회원이 없으면 null
     */
    LoginUserVO findUserByPhoneHash(String phoneHash);

    /**
     * PIN 로그인 - 등록 기기(device_id) 기준 회원 + PIN hash 조회
     * - user_device(device_id) + users JOIN (pin_hash 포함)
     * - 같은 device_id 가 여러 회원에 존재할 수 없도록 UNIQUE(user_id, device_id) — LIMIT 1 로 안전 처리
     *
     * @return 등록된 기기가 없으면 null
     */
    LoginUserVO findUserByDeviceId(String deviceId);

    /**
     * Refresh Token 재발급 - userId(PK) 기준 회원 상태 조회
     * - 재발급 시 회원 존재 여부와 ACTIVE 상태를 검증하기 위한 조회
     * - name_encrypt 등 개인정보 원문은 조회하지 않는다 (knowledge.md: 원문 조회 금지)
     *
     * @return 해당 회원이 없으면 null
     */
    LoginUserVO findUserById(Long userId);

    /**
     * 비밀번호 재설정 - user_auth.password_hash 갱신
     * - BCrypt 해시는 Service Layer 에서 생성 후 전달한다 (Mapper 에서 암호화 금지)
     * - updated_at 은 DB 기본값 정책과 동일하게 CURRENT_TIMESTAMP 로 갱신
     * - 갱신 행 수가 0 이면 해당 회원의 인증 정보가 없는 것 (Service 에서 판단)
     *
     * @return 갱신된 행 수
     */
    int updatePasswordHash(@Param("userId") Long userId, @Param("passwordHash") String passwordHash);
}
