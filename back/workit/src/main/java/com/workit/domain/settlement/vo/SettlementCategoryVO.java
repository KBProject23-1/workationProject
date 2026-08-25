package com.workit.domain.settlement.vo;

import com.workit.domain.workation.vo.BudgetType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

// 정산 집계 한 줄. 카테고리별 배정 예산과 실제 집행액
@Getter
@Setter
@ToString
public class SettlementCategoryVO {

    private BudgetType budgetType;
    private Long expenseCategoryId;
    private String categoryName;   // 마스터 기본 이름
    private String customName;     // 사용자 별칭. 없으면 null
    private BigDecimal targetAmount;
    private BigDecimal spentAmount;
    private Integer expenseCount;

    public String getDisplayCategoryName() {
        return (customName != null && !customName.trim().isEmpty()) ? customName : categoryName;
    }
}
