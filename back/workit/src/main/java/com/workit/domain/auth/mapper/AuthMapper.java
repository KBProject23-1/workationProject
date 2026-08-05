package com.workit.domain.auth.mapper;

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
}
