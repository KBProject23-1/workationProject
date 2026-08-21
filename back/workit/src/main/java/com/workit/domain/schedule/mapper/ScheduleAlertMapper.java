package com.workit.domain.schedule.mapper;

import com.workit.domain.schedule.vo.ScheduleAlertTargetVO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

// 일정 알림 중복 방지용 + 대상 조회 Mapper
// - notification_histories 에서 userId + referenceType + referenceId + notificationType 으로 기존 알림 존재 여부를 확인한다
// - schedules 테이블에서 시작 1시간 전 대상을 조회한다
// - ScheduleAlertService에서 사용한다
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

    /**
     * 특정 일정에 대한 알림이 이미 존재하는지 확인한다.
     * - userId + referenceType + referenceId + notificationType 조건으로 EXISTS 쿼리
     * - 동일한 일정에 대해 중복 알림을 방지한다
     * - user_id 조건을 포함하여 다른 사용자의 알림과 구분한다
     *
     * @param userId           사용자 ID
     * @param referenceType    참조 타입 (예: "SCHEDULE")
     * @param referenceId      참조 ID (일정 ID)
     * @param notificationType 알림 타입 (예: "SCHEDULE_D_MINUS_1_HOUR")
     * @return 알림 존재 여부 (true: 이미 알림 있음, false: 알림 없음)
     */
    boolean existsNotificationByReference(@Param("userId") Long userId,
                                           @Param("referenceType") String referenceType,
                                           @Param("referenceId") Long referenceId,
                                           @Param("notificationType") String notificationType);
}
