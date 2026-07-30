package com.workit.domain.budget.dto.request;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

// 카테고리 1건의 배정 금액
@Getter
@Setter
@ToString
public class BudgetItemRequestDTO {

    private Long expenseCategoryId;
    private BigDecimal targetAmount;
}
