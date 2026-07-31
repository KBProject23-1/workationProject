package com.workit.domain.expense.dto.response;

import com.workit.domain.expense.vo.ExpenseSummaryVO;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

// 지출 목록 상단 요약
@Getter
@Builder
public class ExpenseSummaryResponseDTO {

    private final Integer totalCount;
    private final BigDecimal totalAmount;
    private final Integer uncheckedCount;
    private final Integer unproofedCount;

    public static ExpenseSummaryResponseDTO from(ExpenseSummaryVO vo) {

        // 지출이 한 건도 없으면 집계 쿼리가 null 을 반환할 수 있다
        if (vo == null) {
            return ExpenseSummaryResponseDTO.builder()
                    .totalCount(0)
                    .totalAmount(BigDecimal.ZERO)
                    .uncheckedCount(0)
                    .unproofedCount(0)
                    .build();
        }

        return ExpenseSummaryResponseDTO.builder()
                .totalCount(nvl(vo.getTotalCount()))
                .totalAmount(vo.getTotalAmount() != null ? vo.getTotalAmount() : BigDecimal.ZERO)
                .uncheckedCount(nvl(vo.getUncheckedCount()))
                .unproofedCount(nvl(vo.getUnproofedCount()))
                .build();
    }

    private static Integer nvl(Integer value) {
        return value != null ? value : 0;
    }
}
