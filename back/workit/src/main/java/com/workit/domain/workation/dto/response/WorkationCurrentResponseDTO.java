package com.workit.domain.workation.dto.response;

import com.workit.domain.workation.vo.BudgetSpentVO;
import com.workit.domain.workation.vo.BudgetType;
import com.workit.domain.workation.vo.Region;
import com.workit.domain.workation.vo.WorkationStatus;
import com.workit.domain.workation.vo.WorkationVO;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 메인화면 상단 - 진행 중 워케이션 사용현황
 * 진행 중 워케이션이 없으면 workation = null, budgetSummary = 빈 배열
 */
@Getter
@Builder
public class WorkationCurrentResponseDTO {

    private WorkationSummary workation;
    private List<BudgetSummary> budgetSummary;
    private int uncheckedExpenseCount;

    /** 진행 중 워케이션이 없을 때 */
    public static WorkationCurrentResponseDTO empty() {
        return WorkationCurrentResponseDTO.builder()
                .workation(null)
                .budgetSummary(Collections.emptyList())
                .uncheckedExpenseCount(0)
                .build();
    }

    public static WorkationCurrentResponseDTO of(WorkationVO vo,
                                                 List<BudgetSpentVO> spentList,
                                                 int uncheckedExpenseCount) {

        List<BudgetSummary> summaries = new ArrayList<>();
        summaries.add(BudgetSummary.of(BudgetType.WORK, vo.getBusinessBudgetTotal(), spentList));
        summaries.add(BudgetSummary.of(BudgetType.PERSONAL, vo.getPersonalBudgetTotal(), spentList));

        return WorkationCurrentResponseDTO.builder()
                .workation(WorkationSummary.from(vo))
                .budgetSummary(summaries)
                .uncheckedExpenseCount(uncheckedExpenseCount)
                .build();
    }

    // =================================================================

    @Getter
    @Builder
    public static class WorkationSummary {

        private Long id;
        private String title;
        private RegionSummary region;
        private LocalDate startDate;
        private LocalDate endDate;
        private int totalDays;
        private int elapsedDays;
        private BigDecimal progressRate;
        private WorkationStatus status;

        public static WorkationSummary from(WorkationVO vo) {

            int totalDays = (int) ChronoUnit.DAYS.between(vo.getStartDate(), vo.getEndDate()) + 1;

            // 경과일 : 시작 전이면 0, 종료 후면 전체 일수로 고정
            LocalDate today = LocalDate.now();
            int elapsedDays;
            if (today.isBefore(vo.getStartDate())) {
                elapsedDays = 0;
            } else if (today.isAfter(vo.getEndDate())) {
                elapsedDays = totalDays;
            } else {
                elapsedDays = (int) ChronoUnit.DAYS.between(vo.getStartDate(), today) + 1;
            }

            BigDecimal progressRate = BigDecimal.valueOf(elapsedDays)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalDays), 1, RoundingMode.HALF_UP);

            return WorkationSummary.builder()
                    .id(vo.getId())
                    .title(vo.getTitle())
                    .region(RegionSummary.from(vo.getRegion()))
                    .startDate(vo.getStartDate())
                    .endDate(vo.getEndDate())
                    .totalDays(totalDays)
                    .elapsedDays(elapsedDays)
                    .progressRate(progressRate)
                    .status(vo.getStatus())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class RegionSummary {

        private Long id;
        private String name;

        public static RegionSummary from(Region region) {
            if (region == null) {
                return null;
            }
            return RegionSummary.builder()
                    .id(region.getId())
                    .name(region.getName())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class BudgetSummary {

        private BudgetType budgetType;
        private BigDecimal budgetTotal;
        private BigDecimal spentTotal;
        private BigDecimal remainAmount;
        private BigDecimal usageRate;

        public static BudgetSummary of(BudgetType type,
                                       BigDecimal budgetTotal,
                                       List<BudgetSpentVO> spentList) {

            BigDecimal total = budgetTotal == null ? BigDecimal.ZERO : budgetTotal;

            BigDecimal spent = spentList.stream()
                    .filter(s -> type == s.getBudgetType())
                    .map(BudgetSpentVO::getSpentTotal)
                    .findFirst()
                    .orElse(BigDecimal.ZERO);

            // 총예산이 0이면 나눗셈이 불가하므로 사용률 0 처리
            BigDecimal usageRate = total.compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.ZERO
                    : spent.multiply(BigDecimal.valueOf(100))
                           .divide(total, 1, RoundingMode.HALF_UP);

            return BudgetSummary.builder()
                    .budgetType(type)
                    .budgetTotal(total)
                    .spentTotal(spent)
                    .remainAmount(total.subtract(spent))
                    .usageRate(usageRate)
                    .build();
        }
    }
}
