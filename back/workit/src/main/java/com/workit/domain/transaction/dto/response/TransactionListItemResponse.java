package com.workit.domain.transaction.dto.response;

import com.workit.domain.transaction.vo.TransactionVO;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TransactionListItemResponse {
    private Long transactionId;
    private Long workationId;
    private String paymentSourceType;
    private String merchantName;
    private BigDecimal amount;
    private String transactionType;
    private String categoryAssigned;
    private Boolean isBusinessExpense;
    private String approvedNumber;
    private LocalDateTime approvedAt;

    public static TransactionListItemResponse from(TransactionVO vo) {
        TransactionListItemResponse response = new TransactionListItemResponse();
        response.setTransactionId(vo.getId());
        response.setWorkationId(vo.getWorkationId());
        response.setPaymentSourceType(vo.getPaymentSourceType());
        response.setMerchantName(vo.getMerchantName());
        response.setAmount(vo.getAmount());
        response.setTransactionType(vo.getTransactionType());
        response.setCategoryAssigned(vo.getCategoryAssigned());
        response.setIsBusinessExpense(vo.getIsBusinessExpense());
        response.setApprovedNumber(vo.getApprovedNumber());
        response.setApprovedAt(vo.getApprovedAt());
        return response;
    }
}