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
    // 지출이 발생한 날짜 수. 하루에 여러 건을 써도 1일로 센다
    private final Integer spentDayCount;
    // 워케이션 전체 일수에서 지출한 날을 뺀 값
    private final Integer noSpendDayCount;
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

    // 문서 출력용. 일수 정보가 필요 없는 곳에서 쓴다
    public static SettlementSummaryDTO of(BudgetType budgetType, List<SettlementCategoryVO> rows) {
        return of(budgetType, rows, null, null);
    }

    public static SettlementSummaryDTO of(BudgetType budgetType, List<SettlementCategoryVO> rows,
                                          Integer spentDayCount, Integer totalDays) {

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

        int spentDays = spentDayCount != null ? spentDayCount : 0;

        return SettlementSummaryDTO.builder()
                .budgetType(budgetType)
                .totalAmount(totalAmount)
                .totalCount(totalCount)
                .spentDayCount(spentDays)
                .noSpendDayCount(totalDays != null ? Math.max(totalDays - spentDays, 0) : null)
                .categories(items)
                .build();
    }

    private static BigDecimal nvl(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
