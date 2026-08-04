package com.workit.domain.expense.dto.response;

import lombok.Builder;
import lombok.Getter;

// 5.6 카테고리 수동 변경 응답
// ruleSaved 는 같은 가맹점 정정 규칙이 저장됐는지 여부다
// 수기 입력 건은 가맹점 식별이 안 되므로 규칙을 저장할 수 없다
@Getter
@Builder
public class ExpenseCategoryChangeResponseDTO {

    private final Long expenseId;
    private final Long expenseCategoryId;
    private final String categoryName;
    private final Boolean isAutoCategorized;
    private final Boolean ruleSaved;
    private final String ruleMessage;
}
