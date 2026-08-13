package com.workit.domain.schedule.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

// 스케줄러 통합 조회 응답
@Getter
@Builder
public class ScheduleListResponseDTO {

    private final Long workationId;

    // 워케이션 기간. 화면이 앞뒤로 더 넘길 수 있는지 판단하는 데 쓴다
    private final LocalDate workationStartDate;
    private final LocalDate workationEndDate;

    private final LocalDate startDate;
    private final Integer days;

    private final List<ScheduleDayResponseDTO> schedules;
}
