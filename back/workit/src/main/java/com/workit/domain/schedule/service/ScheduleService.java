package com.workit.domain.schedule.service;

import com.workit.domain.schedule.dto.request.ScheduleCreateRequestDTO;
import com.workit.domain.schedule.dto.response.ScheduleDetailResponseDTO;
import com.workit.domain.schedule.dto.response.ScheduleListResponseDTO;

import java.time.LocalDate;

public interface ScheduleService {

    // 음식점·여가 활동 방문 계획 등록
    ScheduleDetailResponseDTO addSchedule(Long userId, Long workationId,
                                          ScheduleCreateRequestDTO dto);

    // 스케줄러 통합 조회. 예약과 일정을 날짜별로 묶어 반환한다
    ScheduleListResponseDTO getScheduleList(Long userId, Long workationId,
                                            LocalDate startDate, Integer days);

    // 일정 상세
    ScheduleDetailResponseDTO getSchedule(Long userId, Long scheduleId);

    // 일정 삭제
    void removeSchedule(Long userId, Long workationId, Long scheduleId);
}
