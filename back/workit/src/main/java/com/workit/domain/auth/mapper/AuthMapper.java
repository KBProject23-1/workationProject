package com.workit.domain.auth.mapper;

import com.workit.domain.auth.vo.LoginUserVO;
import com.workit.domain.auth.vo.TermsVO;
import com.workit.domain.auth.vo.UserAuthVO;
import com.workit.domain.auth.vo.UserDeviceVO;
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
     * user_profile 테이블 insert (회원가입 완료 — 서버가 생성한 기본 닉네임 저장)
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
     * 비밀번호 변경 - 로그인 사용자의 현재 비밀번호 hash 조회
     * - 현재 비밀번호 BCrypt 대조용 — password_hash(해시) 만 조회한다
     * - BCrypt 는 단방향 해시이므로 조회 결과를 그대로 matches() 에 사용하며 원문을 복호화하지 않는다
     *   (selectPinHashesByUserId 와 동일 원칙 — knowledge.md: 비밀번호 원문 조회 금지)
     * - user_auth 행이 없는 회원(회원 탈퇴 등)이면 null 반환 (USER_NOT_FOUND 판단은 Service)
     *
     * @return 비밀번호 BCrypt 해시, user_auth 가 없으면 null
     */
    String selectPasswordHashByUserId(@Param("userId") Long userId);

    /**
     * 비밀번호 재설정 - user_auth.password_hash 갱신
     * - BCrypt 해시는 Service Layer 에서 생성 후 전달한다 (Mapper 에서 암호화 금지)
     * - updated_at 은 DB 기본값 정책과 동일하게 CURRENT_TIMESTAMP 로 갱신
     * - 갱신 행 수가 0 이면 해당 회원의 인증 정보가 없는 것 (Service 에서 판단)
     *
     * @return 갱신된 행 수
     */
    int updatePasswordHash(@Param("userId") Long userId, @Param("passwordHash") String passwordHash);

    /**
     * PIN 최초 설정 - 기존 PIN 등록 여부 확인 (user_device)
     * - user_id + device_id (UNIQUE(user_id, device_id)) 기준 조회
     * - 반환값이 0 초과면 해당 기기에 이미 PIN 이 등록된 것 (PIN_ALREADY_EXISTS 판단은 Service)
     */
    int countByUserIdAndDeviceId(@Param("userId") Long userId, @Param("deviceId") String deviceId);

    /**
     * PIN 최초 설정 - user_device insert (PIN 정보 저장)
     * - pin_hash(BCrypt) 는 Service Layer 에서 암호화 후 전달 (Mapper 에서 암호화 금지)
     * - last_login_at 은 최초 설정 시점 null, created_at 은 DB 기본값(CURRENT_TIMESTAMP) 사용
     */
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertUserDevice(UserDeviceVO userDevice);

    /**
     * 보안 PIN 재설정 - JWT 로그인 사용자 조회 (users + user_auth JOIN)
     * - userId(PK) 기준으로 회원 상태(status)와 identity_ci_hash 를 함께 조회한다
     *   (PASS 재인증 결과 CI 와 대조하기 위함 — findUserById 는 status 만 조회하므로 본 플로우 전용 정의)
     * - 개인정보 원문은 조회하지 않는다 (knowledge.md: 원문 조회 금지)
     *
     * @return 해당 회원이 없으면 null
     */
    LoginUserVO selectUserAuthById(Long userId);

    /**
     * 보안 PIN 재설정 - 로그인 사용자의 모든 등록 기기(device) pin_hash 갱신
     * - BCrypt 해시는 Service Layer 에서 생성 후 전달한다 (Mapper 에서 암호화 금지)
     * - PASS 재인증 기반 개인 단위 재설정이므로 기기(device_id) 구분 없이 user_id 기준 전체 갱신
     *   (PIN 은 기기별(user_device) 저장 — 재설정 시 등록된 모든 기기에 동일 적용)
     * - 갱신 행 수가 0 이면 등록된 PIN(기기)이 없는 것 (PIN_NOT_REGISTERED 판단은 Service)
     *
     * @return 갱신된 행 수
     */
    int updateUserDevicePinHash(@Param("userId") Long userId, @Param("pinHash") String pinHash);

    /**
     * 보안 PIN 재설정 - 로그인 사용자의 등록 기기별 기존 pin_hash 목록 조회
     * - SAME_AS_CURRENT_PIN 대조용 — 신규 PIN 이 기존 PIN 과 동일한지 BCrypt matches() 로 확인한다
     *   (BCrypt 는 단방향 해시이므로 조회 결과를 그대로 matches() 에 사용하며 원문을 복호화하지 않는다)
     * - 등록된 기기가 없으면 빈 목록 반환 (SAME_AS_CURRENT_PIN 판단은 Service)
     *
     * @return 등록된 기기의 pin_hash(BCrypt) 목록, 없으면 빈 목록
     */
    List<String> selectPinHashesByUserId(Long userId);

    /**
     * 로그인 성공 시 user_device.last_login_at 갱신
     * - user_id + device_id (UNIQUE) 기준으로 해당 기기의 최종 로그인 일시를 현재 시각으로 갱신
     * - PASSWORD 로그인 시 deviceId 가 있는 경우와 PIN 로그인 시 호출한다
     * - deviceId 가 등록되지 않은 신규 기기(初次 PASSWORD 로그인)인 경우 호출하지 않는다
     *   (PIN 설정(setupPin) 시 INSERT 시점의 DB 기본값(CURRENT_TIMESTAMP)이 사용된다)
     *
     * @return 갱신된 행 수 (0 이면 등록되지 않은 기기)
     */
    int updateLastLoginAt(@Param("userId") Long userId, @Param("deviceId") String deviceId);
}
