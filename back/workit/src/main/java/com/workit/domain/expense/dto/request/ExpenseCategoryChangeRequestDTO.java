package com.workit.domain.expense.dto.request;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// 5.6 지출 카테고리 수동 변경 요청
@Getter
@Setter
@ToString
public class ExpenseCategoryChangeRequestDTO {

    private Long expenseCategoryId;

    // 같은 가맹점의 다음 결제에도 이 분류를 적용할지 여부
    // null 이면 true 로 본다
    private Boolean applyToMerchant;

    public boolean isApplyToMerchant() {
        return applyToMerchant == null || applyToMerchant;
    }
}
