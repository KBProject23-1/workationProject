package com.workit.domain.expense.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

// 지출 목록 상단 요약. 필터와 무관하게 워케이션 전체를 집계한다
@Getter
@Setter
@ToString
public class ExpenseSummaryVO {

    private Integer totalCount;
    private BigDecimal totalAmount;
    private Integer uncheckedCount;    // 자동분류 상태 그대로인 건수 (사용자 확인 필요)
    private Integer unproofedCount;    // 법인 지출인데 카드 정보가 없어 증빙이 안 된 건수
}
