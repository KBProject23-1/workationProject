package com.workit.domain.expense.dto.response;

import lombok.Builder;
import lombok.Getter;

// 5.9 지출 예산유형 일괄 변경 응답
@Getter
@Builder
public class ExpenseBudgetTypeBulkResponseDTO {

    // 실제로 옮겨진 건수. 이미 해당 유형이거나 남의 지출이면 제외된다
    private final Integer changedCount;

    // 옮긴 뒤의 미확인 건수. 같은 코드의 계정과목이 예산에 없으면 기타로 가면서 확인 필요가 된다
    private final Integer uncheckedCount;

    public static ExpenseBudgetTypeBulkResponseDTO of(int changedCount, Integer uncheckedCount) {
        return ExpenseBudgetTypeBulkResponseDTO.builder()
                .changedCount(changedCount)
                .uncheckedCount(uncheckedCount == null ? 0 : uncheckedCount)
                .build();
    }
}
