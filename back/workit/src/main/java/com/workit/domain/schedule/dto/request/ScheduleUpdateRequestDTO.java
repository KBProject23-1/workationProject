package com.workit.domain.schedule.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// 음식점·여가 활동 일정 시각 수정 요청.
// 가맹점은 바꾸지 않는다. 다른 장소로 가는 것은 새 일정이라 삭제 후 재등록한다
@Getter
@Setter
public class ScheduleUpdateRequestDTO {

    // 2026-08-14T12:30 형태로 받는다
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm[:ss]")
    private LocalDateTime scheduledAt;
}
