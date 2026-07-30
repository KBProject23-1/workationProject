package com.workit.domain.transaction.dto.response;

import com.workit.domain.transaction.vo.TransactionVO;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentResponse {
    private Long transactionId;
    private String transactionNumber;
    private String merchantName;
    private String paymentSourceType;
    private String transactionType;
    private BigDecimal amount;
    private BigDecimal currentBalance;
    private Boolean isAutoCharged;
    private BigDecimal autoChargedAmount;
    private String approvedNumber;
    private String status;
    private LocalDateTime approvedAt;

    public static PaymentResponse ofCard(TransactionVO tx) {
        PaymentResponse response = new PaymentResponse();
        response.setTransactionId(tx.getId());
        response.setTransactionNumber(tx.getTransactionNumber());
        response.setMerchantName(tx.getMerchantName());
        response.setPaymentSourceType(tx.getPaymentSourceType());
        response.setTransactionType(tx.getTransactionType());
        response.setAmount(tx.getAmount());
        response.setCurrentBalance(null);
        response.setIsAutoCharged(false);
        response.setAutoChargedAmount(null);
        response.setApprovedNumber(tx.getApprovedNumber());
        response.setStatus(tx.getStatus());
        response.setApprovedAt(tx.getApprovedAt());
        return response;
    }

    public static PaymentResponse ofWallet(TransactionVO tx, BigDecimal currentBalance,
                                           boolean isAutoCharged, BigDecimal autoChargedAmount) {
        PaymentResponse response = new PaymentResponse();
        response.setTransactionId(tx.getId());
        response.setTransactionNumber(tx.getTransactionNumber());
        response.setMerchantName(tx.getMerchantName());
        response.setPaymentSourceType(tx.getPaymentSourceType());
        response.setTransactionType(tx.getTransactionType());
        response.setAmount(tx.getAmount());
        response.setCurrentBalance(currentBalance);
        response.setIsAutoCharged(isAutoCharged);
        response.setAutoChargedAmount(autoChargedAmount);
        response.setApprovedNumber(null);
        response.setStatus(tx.getStatus());
        response.setApprovedAt(tx.getApprovedAt());
        return response;
    }
}