package com.workit.domain.schedule.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

// 스케줄러의 하루.
// 일정이 없는 날도 빈 배열로 내려준다. 화면이 "아직 정해진 일정이 없어요" 를 그려야 한다
@Getter
@Builder
public class ScheduleDayResponseDTO {

    private final LocalDate date;
    private final Boolean isToday;
    private final List<ScheduleItemResponseDTO> items;
}
