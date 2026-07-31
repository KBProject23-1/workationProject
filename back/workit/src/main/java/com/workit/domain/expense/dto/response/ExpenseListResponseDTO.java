package com.workit.domain.expense.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

// 5.1 지출 목록 조회 응답
// 요약은 필터와 무관하게 워케이션 전체 기준이고, content 는 필터·페이징이 적용된 결과다
@Getter
@Builder
public class ExpenseListResponseDTO {

    private final Long workationId;
    private final ExpenseSummaryResponseDTO summary;
    private final List<ExpenseItemResponseDTO> content;
    private final int page;
    private final int size;
    private final long totalElements;

    public static ExpenseListResponseDTO of(Long workationId,
                                            ExpenseSummaryResponseDTO summary,
                                            List<ExpenseItemResponseDTO> content,
                                            int page, int size, long totalElements) {
        return ExpenseListResponseDTO.builder()
                .workationId(workationId)
                .summary(summary)
                .content(content)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .build();
    }
}
