package com.workit.domain.budget.vo;

import com.workit.domain.workation.vo.BudgetType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

// 카테고리별 예산 + 실제 지출 집계 결과
// spentAmount 는 budgets 테이블에 저장하지 않고 조회 시점에 집계한다.
// 컬럼으로 두면 지출 등록·수정·삭제마다 갱신해야 하고, 한 번이라도 누락되면 화면 숫자가 틀어진다.
@Getter
@Setter
@ToString
public class BudgetItemVO {

    private Long budgetId;
    private Long workationId;        // 예산 단건 조회 시 소유 워케이션 확인용
    private BudgetType budgetType;
    private Long expenseCategoryId;

    private String categoryName;     // 마스터 기본 이름
    private String categoryCode;     // expense_categories.code (예: FOOD, ACCOMMODATION)
    private String customName;       // 사용자 별칭. 없으면 null
    private String description;
    private Boolean isDeletable;
    private Integer sortOrder;

    private BigDecimal targetAmount; // 카테고리에 배정한 예산
    private BigDecimal spentAmount;  // 실제 지출 합계
    private Integer expenseCount;    // 지출 건수
}
