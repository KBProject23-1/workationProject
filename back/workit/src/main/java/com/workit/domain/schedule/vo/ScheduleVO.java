package com.workit.domain.schedule.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// 음식점·여가 활동 방문 계획.
// 장소 정보는 merchants 에서 조인해 채운다
@Getter
@Setter
public class ScheduleVO {

    private Long id;
    private Long workationId;
    private Long merchantId;
    private LocalDateTime scheduledAt;
    private LocalDateTime createdAt;

    // merchants 조인
    private String merchantName;
    private String merchantCategory;   // RESTAURANT, ACTIVITY
    private String address;
    private String thumbnailUrl;
}
