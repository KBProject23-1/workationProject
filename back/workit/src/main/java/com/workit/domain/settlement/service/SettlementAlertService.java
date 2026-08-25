package com.workit.domain.settlement.service;

import com.workit.domain.workation.vo.WorkationUncheckedCountVO;
import com.workit.domain.workation.vo.WorkationVO;

import java.util.List;
import java.util.Map;

// 정산 알림 판단 서비스 인터페이스
// - 종료 후 3일 경과 but 정산 미완료 워케이션에 대해 SETTLEMENT_OVERDUE 알림을 생성한다
// - 미확인 지출이 3건 이상인 워케이션에 대해 UNCONFIRMED_EXPENSE_OVER_3 알림을 생성한다
// - NotificationCreateService를 통해 알림을 생성하며, 정산 도메인에서 직접 INSERT하지 않는다
// - 중복 알림 방지: notification_histories의 userId + referenceType + referenceId + notificationType을 활용한다
public interface SettlementAlertService {

    /**
     * 종료 후 3일 경과 but 정산 미완료 워케이션에 대해 SETTLEMENT_OVERDUE 알림을 생성한다.
     *
     * @param workations         정산 지연 대상 워케이션 목록
     * @param unconfirmedCounts  workationId → 미확인 지출 항목 수 매핑
     */
    void notifySettlementOverdue(List<WorkationVO> workations, Map<Long, Integer> unconfirmedCounts);

    /**
     * 미확인 지출이 3건 이상인 워케이션에 대해 UNCONFIRMED_EXPENSE_OVER_3 알림을 생성한다.
     *
     * @param uncheckedCounts 미확인 지출 건수 목록 (userId, workationId, uncheckedCount)
     */
    void notifyUnconfirmedExpenses(List<WorkationUncheckedCountVO> uncheckedCounts);
}
