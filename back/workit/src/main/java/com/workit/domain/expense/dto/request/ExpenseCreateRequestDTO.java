package com.workit.domain.expense.dto.request;

import com.workit.domain.workation.vo.BudgetType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;

// 5.2 외부 결제내역 추가 요청
// 앱이 수집하지 못한 실물카드·현금 결제를 사용자가 직접 등록한다
@Getter
@Setter
@ToString
public class ExpenseCreateRequestDTO {

    private String merchantName;
    private Long cardId;              // 법인 지출이면 필수
    private BigDecimal amount;
    private LocalDate spentDate;
    private BudgetType budgetType;
    private Long expenseCategoryId;
    private String memo;
}
