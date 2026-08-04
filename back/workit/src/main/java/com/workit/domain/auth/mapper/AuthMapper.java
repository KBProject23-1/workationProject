package com.workit.domain.auth.mapper;

import com.workit.domain.auth.vo.TermsVO;

import java.util.List;

public interface AuthMapper {

    /**
     * 약관 목록 전체 조회 (필수 → 선택 순)
     * - 회원가입 화면 및 서비스 내 약관 노출에 사용
     */
    List<TermsVO> selectTermsList();

    /**
     * CI(SHA-256 해시) 기준 중복 가입 조회
     * - user_auth.identity_ci_hash (UNIQUE) 대상
     * - 반환값이 0 초과면 이미 가입된 회원
     */
    int countByCiHash(String ciHash);
}
