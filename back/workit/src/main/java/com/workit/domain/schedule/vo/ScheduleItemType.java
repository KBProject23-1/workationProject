package com.workit.domain.schedule.vo;

// 스케줄러 한 줄이 무엇에서 왔는지.
// 눌렀을 때 예약 상세로 갈지 일정 상세로 갈지가 갈린다
public enum ScheduleItemType {

    // 숙소·공유오피스. reservations 에서 온다
    RESERVATION,

    // 음식점·여가 활동. schedules 에서 온다
    SCHEDULE
}
