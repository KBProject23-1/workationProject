package com.workit.domain.budget.service;

import com.workit.domain.budget.dto.request.BudgetItemAddRequestDTO;
import com.workit.domain.budget.dto.request.BudgetSetupRequestDTO;
import com.workit.domain.budget.dto.response.BudgetItemResponseDTO;
import com.workit.domain.budget.dto.response.BudgetStatusResponseDTO;
import com.workit.domain.budget.dto.response.BudgetSummaryResponseDTO;
import com.workit.domain.workation.vo.BudgetType;

public interface BudgetService {

    // 3.1 예산 사용현황 조회
    // budgetType 이 null 이면 법인·개인 모두 반환
    BudgetStatusResponseDTO getBudgetStatus(Long userId, Long workationId, BudgetType budgetType);

    // 3.2 예산 카테고리별 세부 금액 설정
    BudgetSummaryResponseDTO setupBudget(Long userId, Long workationId, BudgetSetupRequestDTO dto);

    // 3.3 예산 세부 금액 수정
    BudgetSummaryResponseDTO modifyBudget(Long userId, Long workationId, BudgetSetupRequestDTO dto);

    // 3.4 예산 카테고리 추가 (＋ 버튼)
    BudgetItemResponseDTO addBudgetItem(Long userId, Long workationId, BudgetItemAddRequestDTO dto);

    // 3.5 예산 카테고리 삭제 (− 버튼)
    // force 가 true 면 지출이 있어도 예산 배정만 삭제한다
    void removeBudgetItem(Long userId, Long workationId, Long budgetId, boolean force);
}
