package com.workit.domain.expense.dto.request;

import com.workit.domain.workation.vo.BudgetType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// 5.7 경비/개인소비 구분 변경 요청
// 예산 유형이 바뀌면 카테고리 마스터도 달라지므로 카테고리를 함께 받는다
@Getter
@Setter
@ToString
public class ExpenseBudgetTypeChangeRequestDTO {

    private BudgetType budgetType;
    private Long expenseCategoryId;
}
