package com.workit.domain.budget.service;

import com.workit.domain.workation.vo.BudgetType;

// 예산 알림 판단 서비스 인터페이스
// - 지출이 추가·수정된 후 카테고리별 예산 사용률을 확인하여 80% / 초과 알림을 생성한다
// - 알림 생성은 공통 알림 생성 서비스(NotificationCreateService)를 통해 처리한다
// -_budgetServiceImpl 또는 WorkationExpenseServiceImpl 에서 호출한다
public interface BudgetAlertService {

    /**
     * 지출 변경 후 예산 알림을 확인하고 필요시 알림을 생성한다.
     *
     * 흐름:
     * 1. 해당 카테고리의 예산 배정 + 실제 지출 집계 조회
     * 2. 사용률 계산 (spentAmount / targetAmount * 100)
     * 3. 80% 도달 여부 판단 (80% 이상이면 알림 생성, 중복 방지)
     * 4. 예산 초과 여부 판단 (100% 초과면 알림 생성, 중복 방지)
     * 5. NotificationCreateService 호출
     *
     * @param userId            사용자 ID
     * @param workationId       워케이션 ID
     * @param budgetType        예산 유형 (WORK / PERSONAL)
     * @param expenseCategoryId 지출 카테고리 ID
     */
    void checkAndNotifyBudgetAlert(Long userId, Long workationId,
                                    BudgetType budgetType, Long expenseCategoryId);
}
