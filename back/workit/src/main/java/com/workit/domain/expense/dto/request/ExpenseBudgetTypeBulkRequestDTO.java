package com.workit.domain.expense.dto.request;

import com.workit.domain.workation.vo.BudgetType;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

// 5.9 지출 예산유형 일괄 변경 요청
@Getter
@Setter
public class ExpenseBudgetTypeBulkRequestDTO {

    // 옮길 예산 유형
    private BudgetType budgetType;

    // 옮길 지출 id 목록
    private List<Long> expenseIds;
}
