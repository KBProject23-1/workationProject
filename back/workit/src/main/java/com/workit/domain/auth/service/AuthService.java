package com.workit.domain.auth.service;

import com.workit.domain.auth.dto.response.TermsListResponseDTO;

public interface AuthService {

    /** 필수/선택 약관 목록 조회 */
    TermsListResponseDTO getTermsList();
}
