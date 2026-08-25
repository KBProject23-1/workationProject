package com.workit.domain.wallet.dto.response;

import com.workit.domain.account.util.AccountNumberMasker;
import com.workit.domain.account.vo.BankAccountVO;
import com.workit.domain.transaction.vo.TransactionVO;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RefundResponse {
    private Long transactionId;
    private BigDecimal refundedAmount;
    private BigDecimal remainingBalance;
    private TargetAccountInfo targetAccount;
    private String paymentSourceType;
    private String transactionType;
    private LocalDateTime approvedAt;

    public static RefundResponse of(TransactionVO tx, BigDecimal remainingBalance, BankAccountVO targetAccount) {
        RefundResponse response = new RefundResponse();
        response.setTransactionId(tx.getId());
        response.setRefundedAmount(tx.getAmount());
        response.setRemainingBalance(remainingBalance);

        TargetAccountInfo info = new TargetAccountInfo();
        info.setBankCode(targetAccount.getBankCode());
        info.setMaskedAccountNumber(AccountNumberMasker.mask(targetAccount.getAccountNumber()));
        response.setTargetAccount(info);

        response.setPaymentSourceType(tx.getPaymentSourceType());
        response.setTransactionType(tx.getTransactionType());
        response.setApprovedAt(tx.getApprovedAt());
        return response;
    }

    @lombok.Data
    public static class TargetAccountInfo {
        private String bankCode;
        private String maskedAccountNumber;
    }
}