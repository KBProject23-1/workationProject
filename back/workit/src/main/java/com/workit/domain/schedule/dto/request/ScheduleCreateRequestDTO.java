package com.workit.domain.schedule.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// 음식점·여가 활동 일정 등록 요청
@Getter
@Setter
public class ScheduleCreateRequestDTO {

    private Long merchantId;

    // 2026-08-14T12:30 형태로 받는다
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm[:ss]")
    private LocalDateTime scheduledAt;
}
