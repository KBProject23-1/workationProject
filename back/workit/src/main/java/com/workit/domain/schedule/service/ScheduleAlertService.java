package com.workit.domain.schedule.service;

import com.workit.domain.schedule.vo.ScheduleAlertTargetVO;

import java.util.List;

// 일정 알림 판단 서비스 인터페이스
// - 시작 1시간 전 일정에 대해 알림을 생성한다
// - NotificationCreateService를 통해 알림을 생성하며, 일정 도메인에서 직접 INSERT하지 않는다
// - 중복 알림 방지: notification_histories의 userId + referenceType + referenceId + notificationType을 활용한다
public interface ScheduleAlertService {

    /**
     * 시작 1시간 전 일정에 대해 알림을 생성한다.
     * - 각 대상에 대해 중복 여부를 확인하고, 필요시 NotificationCreateService를 통해 알림을 생성한다
     *
     * @param targets 알림 대상 목록
     */
    void notifyScheduleD1Hour(List<ScheduleAlertTargetVO> targets);
}
