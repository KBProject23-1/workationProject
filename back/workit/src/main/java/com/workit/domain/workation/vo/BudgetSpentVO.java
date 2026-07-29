package com.workit.domain.workation.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Setter
@ToString
public class BudgetSpentVO {

    private BudgetType budgetType;
    private BigDecimal spentTotal;
}
