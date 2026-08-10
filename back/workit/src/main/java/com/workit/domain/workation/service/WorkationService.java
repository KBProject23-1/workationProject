package com.workit.domain.workation.service;

import com.workit.domain.workation.dto.request.WorkationCreateRequestDTO;
import com.workit.domain.workation.dto.request.WorkationUpdateRequestDTO;
import com.workit.domain.workation.dto.response.*;
import com.workit.global.dto.PageResponseDTO;

import java.time.LocalDate;

public interface WorkationService {

    // 1.1 워케이션 등록
    WorkationResponseDTO addWorkation(Long userId, WorkationCreateRequestDTO dto);

    // 1.2 진행 중 워케이션 조회
    WorkationCurrentResponseDTO getCurrentWorkation(Long userId);

    // 1.3 워케이션 기록 목록 (정산 완료, 최신순)
    PageResponseDTO<WorkationHistoryResponseDTO> getWorkationHistory(Long userId, int page, int size);

    // 1.4 워케이션 수정
    // force = true 면 기간을 벗어나게 되는 지출을 워케이션에서 분리하고 진행한다
    WorkationResponseDTO modifyWorkation(Long userId, Long workationId,
                                         WorkationUpdateRequestDTO dto, boolean force);

    // 1.5 워케이션 삭제
    void removeWorkation(Long userId, Long workationId);

    // 1.8 기간 변경 영향 조회
    // 기간을 바꾸기 전에 어긋나는 예약과 숙소가 빈 날짜를 알려준다
    // 날짜를 주지 않으면 현재 기간 기준으로 본다
    ReservationCheckResponseDTO checkReservations(Long userId, Long workationId,
                                                  LocalDate startDate, LocalDate endDate);

    // 1.6 워케이션 종료
    WorkationSettleResponseDTO settleWorkation(Long userId, Long workationId);
}
