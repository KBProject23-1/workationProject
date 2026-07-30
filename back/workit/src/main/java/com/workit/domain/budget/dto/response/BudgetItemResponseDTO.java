package com.workit.domain.budget.dto.response;

import com.workit.domain.budget.vo.BudgetItemVO;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;

// 카테고리 1건의 예산 사용현황
@Getter
@Builder
public class BudgetItemResponseDTO {

    private Long budgetId;
    private Long expenseCategoryId;
    private String categoryName;
    private String description;
    private BigDecimal targetAmount;
    private BigDecimal spentAmount;
    private BigDecimal usageRate;
    private Boolean isDeletable;
    private Integer expenseCount;

    public static BudgetItemResponseDTO from(BudgetItemVO vo) {

        BigDecimal target = nvl(vo.getTargetAmount());
        BigDecimal spent = nvl(vo.getSpentAmount());

        return BudgetItemResponseDTO.builder()
                .budgetId(vo.getBudgetId())
                .expenseCategoryId(vo.getExpenseCategoryId())
                // 별칭이 있으면 별칭을 표시명으로 사용한다 (카테고리 조회와 동일한 규칙)
                .categoryName(vo.getCustomName() != null ? vo.getCustomName() : vo.getCategoryName())
                .description(vo.getDescription())
                .targetAmount(target)
                .spentAmount(spent)
                .usageRate(calculateRate(spent, target))
                .isDeletable(vo.getIsDeletable())
                .expenseCount(vo.getExpenseCount() != null ? vo.getExpenseCount() : 0)
                .build();
    }

    // 사용률 계산
    // 예산이 0원이면 나눗셈이 불가능하므로 0% 로 처리한다
    // BigDecimal.divide 는 자릿수를 지정하지 않으면 나누어떨어지지 않을 때 예외가 발생한다
    public static BigDecimal calculateRate(BigDecimal spent, BigDecimal target) {

        if (target == null || target.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return spent.multiply(BigDecimal.valueOf(100))
                .divide(target, 1, RoundingMode.HALF_UP);
    }

    // 지출이 없는 카테고리는 집계 결과가 null 이므로 0 으로 바꾼다
    // Service 계층에서도 금액 계산에 사용하므로 public 으로 공개한다
    public static BigDecimal nvl(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
