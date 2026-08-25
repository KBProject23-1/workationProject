package com.workit.domain.expense.dto.request;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;

// 5.4 지출 수정 요청
// 프론트가 등록 폼을 재사용하므로 전체 필드를 받는다(PUT)
// 예산 유형·카테고리는 5.6 / 5.7 전용 API 로 변경한다
@Getter
@Setter
@ToString
public class ExpenseUpdateRequestDTO {

    private String merchantName;
    private Long cardId;
    private BigDecimal amount;
    private LocalDate spentDate;
    private String memo;
}
