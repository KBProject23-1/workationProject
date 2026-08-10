package com.workit.domain.expense.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

// 5.8 지출 일괄 확정 요청
@Getter
@Setter
public class ExpenseConfirmRequestDTO {

    // 확정할 지출 id 목록. 카테고리는 현재 값을 그대로 쓴다
    private List<Long> expenseIds;
}
