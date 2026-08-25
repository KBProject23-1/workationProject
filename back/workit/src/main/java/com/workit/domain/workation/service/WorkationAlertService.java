package com.workit.domain.workation.service;

import com.workit.domain.workation.vo.WorkationVO;

import java.util.List;

// 워케이션 알림 판단 서비스 인터페이스
// - 내일 시작/종료하는 워케이션에 대해 D-1 알림을 생성한다
// - NotificationCreateService를 통해 알림을 생성하며, 워케이션 도메인에서 직접 INSERT하지 않는다
// - 중복 알림 방지: notification_histories의 userId + referenceType + referenceId + notificationType을 활용한다
public interface WorkationAlertService {

    /**
     * 내일 시작하는 워케이션에 대해 시작 D-1 알림을 생성한다.
     *
     * @param workations 내일 시작하는 워케이션 목록
     */
    void notifyWorkationStartD1(List<WorkationVO> workations);

    /**
     * 내일 종료하는 워케이션에 대해 종료 D-1 알림을 생성한다.
     *
     * @param workations 내일 종료하는 워케이션 목록
     */
    void notifyWorkationEndD1(List<WorkationVO> workations);
}
