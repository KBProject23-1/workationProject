package com.workit.domain.schedule.mapper;

import com.workit.domain.schedule.vo.ScheduleAlertTargetVO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

// 일정 알림 대상 조회 Mapper
// - schedules 테이블에서 시작 1시간 전 대상을 조회한다
// - 중복 알림 방지는 NotificationMapper.existsNotificationsByReferencesBatch로 처리한다
// - ScheduleAlertScheduler에서 사용한다
public interface ScheduleAlertMapper {

    /**
     * 시작 1시간 전 대상 일정 조회 (catch-up 지원)
     * - scheduledAt이 now-5분 ~ now+1시간 범위에 있는 일정을 조회한다
     * - now-5분보다 이전 일정은 너무 오래된 과거 일정으로 간주하여 제외한다
     * - now+1시간보다 미래 일정은 아직 알림 시점이 아닌 것으로 간주하여 제외한다
     * - workations.status = 'ACTIVE'인 워케이션에 속한 일정만 조회한다
     * - workation의 user_id를 통해 알림 대상 사용자를 확인한다
     * - merchants.name을 일정 제목으로 사용한다
     *
     * @param now 기준 시각
     * @return 알림 대상 목록
     */
    List<ScheduleAlertTargetVO> selectSchedulesStartingInOneHour(@Param("now") LocalDateTime now);
}
