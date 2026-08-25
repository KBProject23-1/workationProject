package com.workit.domain.budget.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

// 3.1 예산 사용현황 조회 응답
// budgetType 을 지정하지 않으면 budgets 에 WORK / PERSONAL 두 건이 담긴다
@Getter
@Builder
public class BudgetStatusResponseDTO {

    private Long workationId;
    private List<BudgetSummaryResponseDTO> budgets;

    public static BudgetStatusResponseDTO of(Long workationId, List<BudgetSummaryResponseDTO> budgets) {
        return BudgetStatusResponseDTO.builder()
                .workationId(workationId)
                .budgets(budgets)
                .build();
    }
}
