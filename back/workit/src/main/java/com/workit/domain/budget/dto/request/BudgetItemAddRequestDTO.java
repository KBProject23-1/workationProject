package com.workit.domain.budget.dto.request;

import com.workit.domain.workation.vo.BudgetType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

// 3.4 예산 카테고리 추가 요청 (＋ 버튼)
// 마스터에 있지만 아직 예산에 없는 카테고리를 한 건 추가한다
@Getter
@Setter
@ToString
public class BudgetItemAddRequestDTO {

    private BudgetType budgetType;
    private Long expenseCategoryId;
    private BigDecimal targetAmount;
}
