package com.workit.domain.workation.service;

import com.workit.domain.workation.dto.request.WorkationCreateRequestDTO;
import com.workit.domain.workation.dto.request.WorkationUpdateRequestDTO;
import com.workit.domain.workation.dto.response.*;
import com.workit.global.dto.PageResponseDTO;

public interface WorkationService {

    // 1.1 워케이션 등록
    WorkationResponseDTO addWorkation(Long userId, WorkationCreateRequestDTO dto);

    // 1.2 진행 중 워케이션 조회
    WorkationCurrentResponseDTO getCurrentWorkation(Long userId);

    // 1.3 워케이션 기록 목록 (정산 완료, 최신순)
    PageResponseDTO<WorkationHistoryResponseDTO> getWorkationHistory(Long userId, int page, int size);

    // 1.4 워케이션 수정
    WorkationResponseDTO modifyWorkation(Long userId, Long workationId, WorkationUpdateRequestDTO dto);

    // 1.5 워케이션 삭제
    void removeWorkation(Long userId, Long workationId);

    // 1.6 워케이션 종료
    WorkationSettleResponseDTO settleWorkation(Long userId, Long workationId);
}
