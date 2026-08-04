package com.workit.domain.expense.dto.response;

import com.workit.global.dto.PageResponseDTO;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

// 5.1 지출 목록 조회 응답
// 요약은 필터와 무관하게 워케이션 전체 기준이고, expenses 는 필터·페이징이 적용된 결과다
// 페이징 형식은 다른 목록 API 와 맞추기 위해 PageResponseDTO 를 쓴다
@Getter
@Builder
public class ExpenseListResponseDTO {

    private final Long workationId;
    private final ExpenseSummaryResponseDTO summary;
    private final PageResponseDTO<ExpenseItemResponseDTO> expenses;

    public static ExpenseListResponseDTO of(Long workationId,
                                            ExpenseSummaryResponseDTO summary,
                                            List<ExpenseItemResponseDTO> content,
                                            int page, int size, long totalElements) {
        return ExpenseListResponseDTO.builder()
                .workationId(workationId)
                .summary(summary)
                .expenses(PageResponseDTO.of(content, page, size, totalElements))
                .build();
    }
}
