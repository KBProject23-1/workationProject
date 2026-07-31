package com.workit.domain.transaction.dto.response;

import com.workit.domain.transaction.vo.TransactionVO;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CancelResponse {
    private Long transactionId;
    private String status;
    private LocalDateTime cancelledAt;
    private BigDecimal refundedAmount;
    private String refundedTo;

    public static CancelResponse of(TransactionVO vo, BigDecimal refundedAmount, String refundedTo) {
        CancelResponse response = new CancelResponse();
        response.setTransactionId(vo.getId());
        response.setStatus(vo.getStatus());
        response.setCancelledAt(vo.getCancelledAt());
        response.setRefundedAmount(refundedAmount);
        response.setRefundedTo(refundedTo);
        return response;
    }
}