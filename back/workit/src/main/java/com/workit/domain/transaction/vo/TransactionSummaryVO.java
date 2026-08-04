package com.workit.domain.transaction.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

// 거래 내역 조회 기간 전체에 대한 집계. 페이징과 무관하게 필터 조건 전체를 기준으로 계산한다
@Getter
@Setter
@ToString
public class TransactionSummaryVO {

    private BigDecimal totalPaymentAmount;   // 결제 합계 (취소 제외)
    private BigDecimal totalChargeAmount;    // 충전 합계 (자동충전 포함)
    private Long totalPaymentCount;          // 결제 건수 (취소 제외)
}
