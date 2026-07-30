package com.workit.domain.wallet.dto.response;

import com.workit.domain.transaction.vo.TransactionVO;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ChargeResponse {
    private Long transactionId;
    private BigDecimal chargedAmount;
    private BigDecimal currentBalance;
    private String paymentSourceType;
    private String transactionType;
    private LocalDateTime approvedAt;

    public static ChargeResponse of(TransactionVO tx, BigDecimal currentBalance) {
        ChargeResponse response = new ChargeResponse();
        response.setTransactionId(tx.getId());
        response.setChargedAmount(tx.getAmount());
        response.setCurrentBalance(currentBalance);
        response.setPaymentSourceType(tx.getPaymentSourceType());
        response.setTransactionType(tx.getTransactionType());
        response.setApprovedAt(tx.getApprovedAt());
        return response;
    }
}