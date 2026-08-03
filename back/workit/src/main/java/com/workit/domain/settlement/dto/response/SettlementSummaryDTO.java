package com.workit.domain.settlement.dto.response;

import com.workit.domain.settlement.vo.SettlementCategoryVO;
import com.workit.domain.workation.vo.BudgetType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

// 예산 유형 1건의 정산 집계
@Getter
@Builder
public class SettlementSummaryDTO {

    private final BudgetType budgetType;
    private final BigDecimal totalAmount;
    private final Integer totalCount;
    private final List<CategoryItem> categories;

    @Getter
    @Builder
    public static class CategoryItem {
        private final Long expenseCategoryId;
        private final String categoryName;
        private final BigDecimal targetAmount;
        private final BigDecimal spentAmount;
        private final Integer expenseCount;
    }

    public static SettlementSummaryDTO of(BudgetType budgetType, List<SettlementCategoryVO> rows) {

        List<CategoryItem> items = rows.stream()
                .map(vo -> CategoryItem.builder()
                        .expenseCategoryId(vo.getExpenseCategoryId())
                        .categoryName(vo.getDisplayCategoryName())
                        .targetAmount(nvl(vo.getTargetAmount()))
                        .spentAmount(nvl(vo.getSpentAmount()))
                        .expenseCount(vo.getExpenseCount() != null ? vo.getExpenseCount() : 0)
                        .build())
                .collect(Collectors.toList());

        // 합계는 DB 에서 따로 구하지 않고 카테고리 목록에서 더한다
        // 집계 쿼리를 두 번 실행하면 두 값이 어긋날 수 있다
        BigDecimal totalAmount = items.stream()
                .map(CategoryItem::getSpentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalCount = items.stream()
                .mapToInt(CategoryItem::getExpenseCount)
                .sum();

        return SettlementSummaryDTO.builder()
                .budgetType(budgetType)
                .totalAmount(totalAmount)
                .totalCount(totalCount)
                .categories(items)
                .build();
    }

    private static BigDecimal nvl(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
