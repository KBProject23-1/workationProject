package com.workit.domain.schedule.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// 일정 알림 대상 VO
// - ScheduleAlertMapper의 selectSchedulesStartingInOneHour 결과를 담는다
// - 일정 ID, 사용자 ID, 일정 제목, 시작 일시를 포함한다
@Getter
@Setter
public class ScheduleAlertTargetVO {

    /** 일정 고유 번호 (schedules.id) */
    private Long scheduleId;

    /** 알림 대상 사용자 ID (workations.user_id) */
    private Long userId;

    /** 일정 제목 (merchants.name) */
    private String scheduleTitle;

    /** 일정 시작 일시 (schedules.scheduled_at) */
    private LocalDateTime scheduledAt;
}
