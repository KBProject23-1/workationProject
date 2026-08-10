package com.workit.domain.expense.dto.response;

import lombok.Builder;
import lombok.Getter;

// 5.8 지출 일괄 확정 응답
@Getter
@Builder
public class ExpenseConfirmResponseDTO {

    // 실제로 확정된 건수. 이미 확정됐거나 남의 지출이면 제외된다
    private final Integer confirmedCount;

    // 확정 후에도 남아 있는 미확인 건수. 목록 상단 배지를 갱신하는 데 쓴다
    private final Integer uncheckedCount;

    public static ExpenseConfirmResponseDTO of(int confirmedCount, Integer uncheckedCount) {
        return ExpenseConfirmResponseDTO.builder()
                .confirmedCount(confirmedCount)
                .uncheckedCount(uncheckedCount == null ? 0 : uncheckedCount)
                .build();
    }
}
