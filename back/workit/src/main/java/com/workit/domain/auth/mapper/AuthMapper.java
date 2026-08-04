package com.workit.domain.auth.mapper;

import com.workit.domain.auth.vo.TermsVO;

import java.util.List;

public interface AuthMapper {

    /**
     * 약관 목록 전체 조회 (필수 → 선택 순)
     * - 회원가입 화면 및 서비스 내 약관 노출에 사용
     */
    List<TermsVO> selectTermsList();
}
