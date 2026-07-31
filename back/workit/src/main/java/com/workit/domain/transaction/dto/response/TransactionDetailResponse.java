package com.workit.domain.transaction.dto.response;

import com.workit.domain.transaction.vo.TransactionVO;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TransactionDetailResponse {
    private Long transactionId;
    private Long userId;
    private Long workationId;
    private String paymentSourceType;
    private Long cardId;
    private Long bankAccountId;
    private Long merchantId;
    private String merchantName;
    private BigDecimal amount;
    private String transactionType;
    private String categoryAssigned;
    private Boolean isBusinessExpense;
    private String approvedNumber;
    private String status;
    private LocalDateTime approvedAt;
    private LocalDateTime cancelledAt;
    private Long reservationId;

    public static TransactionDetailResponse from(TransactionVO vo) {
        TransactionDetailResponse response = new TransactionDetailResponse();
        response.setTransactionId(vo.getId());
        response.setUserId(vo.getUserId());
        response.setWorkationId(vo.getWorkationId());
        response.setPaymentSourceType(vo.getPaymentSourceType());
        response.setCardId(vo.getCardId());
        response.setBankAccountId(vo.getBankAccountId());
        response.setMerchantId(vo.getMerchantId());
        response.setMerchantName(vo.getMerchantName());
        response.setAmount(vo.getAmount());
        response.setTransactionType(vo.getTransactionType());
        response.setCategoryAssigned(vo.getCategoryAssigned());
        response.setIsBusinessExpense(vo.getIsBusinessExpense());
        response.setApprovedNumber(vo.getApprovedNumber());
        response.setStatus(vo.getStatus());
        response.setApprovedAt(vo.getApprovedAt());
        response.setCancelledAt(vo.getCancelledAt());
        response.setReservationId(vo.getReservationId());
        return response;
    }
}