package com.workit.domain.workation.service;

import com.workit.domain.workation.dto.WorkationCreateRequestDTO;
import com.workit.domain.workation.dto.WorkationCurrentResponseDTO;
import com.workit.domain.workation.dto.WorkationResponseDTO;

public interface WorkationService {

    /** 1.1 워케이션 등록 */
    WorkationResponseDTO addWorkation(Long userId, WorkationCreateRequestDTO dto);

    /** 1.2 진행 중 워케이션 조회 */
    WorkationCurrentResponseDTO getCurrentWorkation(Long userId);
}
