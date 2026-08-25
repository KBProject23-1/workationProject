package com.workit.domain.transaction.dto.response;

import com.workit.domain.transaction.vo.TransactionSummaryVO;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

// 거래 내역 조회 기간 전체 집계 (결제 합계 / 충전 합계)
@Getter
@Builder
public class TransactionSummaryResponse {

    private final BigDecimal totalPaymentAmount;
    private final BigDecimal totalChargeAmount;
    private final Long totalPaymentCount;

    public static TransactionSummaryResponse from(TransactionSummaryVO vo) {

        if (vo == null) {
            return TransactionSummaryResponse.builder()
                    .totalPaymentAmount(BigDecimal.ZERO)
                    .totalChargeAmount(BigDecimal.ZERO)
                    .totalPaymentCount(0L)
                    .build();
        }

        return TransactionSummaryResponse.builder()
                .totalPaymentAmount(vo.getTotalPaymentAmount() != null ? vo.getTotalPaymentAmount() : BigDecimal.ZERO)
                .totalChargeAmount(vo.getTotalChargeAmount() != null ? vo.getTotalChargeAmount() : BigDecimal.ZERO)
                .totalPaymentCount(vo.getTotalPaymentCount() != null ? vo.getTotalPaymentCount() : 0L)
                .build();
    }
}
