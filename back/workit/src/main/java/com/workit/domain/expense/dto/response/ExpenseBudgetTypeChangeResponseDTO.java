package com.workit.domain.expense.dto.response;

import com.workit.domain.workation.vo.BudgetType;
import lombok.Builder;
import lombok.Getter;

// 5.7 경비/개인소비 구분 변경 응답
@Getter
@Builder
public class ExpenseBudgetTypeChangeResponseDTO {

    private final Long expenseId;
    private final BudgetType budgetType;
    private final Long expenseCategoryId;
    private final String categoryName;
}
