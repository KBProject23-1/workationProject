package com.workit.domain.schedule.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

// 스케줄러 한 줄.
// 예약과 일정을 UNION ALL 로 합쳐 받는다.
// 두 원본의 컬럼이 달라 쓰지 않는 값은 null 로 온다
@Getter
@Setter
public class ScheduleItemVO {

    private ScheduleItemType itemType;

    // 예약이면 reservationId, 일정이면 scheduleId 만 채워진다
    private Long reservationId;
    private Long scheduleId;

    private Long merchantId;
    private String merchantName;
    private String merchantCategory;   // ACCOMMODATION, OFFICE, RESTAURANT, ACTIVITY
    private String address;
    private String thumbnailUrl;

    // 숙소·공유오피스는 기간, 음식점·여가는 시각이다
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime scheduledAt;

    // 어느 날짜 칸에 들어갈지. 조회한 날짜 범위를 서버가 펼쳐 붙인다
    private LocalDate itemDate;
}
