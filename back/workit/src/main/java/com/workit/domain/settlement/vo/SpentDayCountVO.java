package com.workit.domain.settlement.vo;

import com.workit.domain.workation.vo.BudgetType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// 예산 유형별 지출 발생 일수
@Getter
@Setter
@ToString
public class SpentDayCountVO {

    private BudgetType budgetType;
    private Integer spentDayCount;
}
