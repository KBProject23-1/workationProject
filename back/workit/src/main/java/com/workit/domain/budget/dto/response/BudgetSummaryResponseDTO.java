package com.workit.domain.budget.dto.response;

import com.workit.domain.workation.vo.BudgetType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

// 예산 유형(법인/개인) 단위 집계 + 카테고리 목록
// budgetTotal 은 워케이션 등록 시 정한 총예산, targetSum 은 카테고리에 배분한 금액의 합.
// 둘을 함께 내려주면 프론트가 "아직 배분되지 않은 금액" 을 계산해 보여줌
@Getter
@Builder
public class BudgetSummaryResponseDTO {

    private BudgetType budgetType;
    private BigDecimal budgetTotal;
    private BigDecimal targetSum;
    private BigDecimal spentTotal;
    private BigDecimal remainAmount;
    private BigDecimal usageRate;
    private List<BudgetItemResponseDTO> items;

    public static BudgetSummaryResponseDTO of(BudgetType budgetType,
                                             BigDecimal budgetTotal,
                                             List<BudgetItemResponseDTO> items) {

        BigDecimal total = BudgetItemResponseDTO.nvl(budgetTotal);

        // BigDecimal 은 stream().sum() 을 제공하지 않으므로 직접 누적한다
        BigDecimal targetSum = BigDecimal.ZERO;
        BigDecimal spentTotal = BigDecimal.ZERO;

        for (BudgetItemResponseDTO item : items) {
            targetSum = targetSum.add(item.getTargetAmount());
            spentTotal = spentTotal.add(item.getSpentAmount());
        }

        return BudgetSummaryResponseDTO.builder()
                .budgetType(budgetType)
                .budgetTotal(total)
                .targetSum(targetSum)
                .spentTotal(spentTotal)
                // 사용자가 체감하는 잔액은 총예산 기준이므로 배분 합계가 아닌 budgetTotal 에서 뺀다
                .remainAmount(total.subtract(spentTotal))
                .usageRate(BudgetItemResponseDTO.calculateRate(spentTotal, total))
                .items(items)
                .build();
    }
}
