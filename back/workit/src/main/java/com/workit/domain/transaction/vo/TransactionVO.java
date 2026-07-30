package com.workit.domain.transaction.vo;

import com.workit.domain.account.vo.BankAccountVO;
import com.workit.domain.card.vo.CardVO;
import com.workit.domain.transaction.dto.request.PaymentRequest;
import com.workit.domain.transaction.util.TransactionNumberGenerator;
import com.workit.domain.wallet.vo.WalletVO;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TransactionVO {

    private Long id;
    private Long userId;
    private Long walletId;
    private Long bankAccountId;
    private Long cardId;
    private Long workationId;
    private Long reservationId;
    private Long merchantId;
    private String paymentSourceType;
    private String merchantName;
    private BigDecimal amount;
    private String transactionType;
    private String categoryAssigned;
    private Boolean isBusinessExpense;
    private String approvedNumber;
    private String transactionNumber;
    private String status;
    private LocalDateTime approvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime cancelledAt;

    private String merchantAddress;
    private String merchantTaxpayerNumber;
    private String merchantPhoneNumber;

    private String cardCompanyName;
    private String cardName;
    private String cardNumber;

    public static TransactionVO forCardPayment(Long userId, CardVO card, PaymentRequest request,
                                               boolean isBusinessExpense, String approvalNumber) {
        TransactionVO vo = new TransactionVO();
        vo.setUserId(userId);
        vo.setCardId(card.getId());
        vo.setReservationId(request.getReservationId());
        vo.setMerchantId(request.getMerchantId());
        vo.setPaymentSourceType("CARD");
        vo.setMerchantName(request.getMerchantName());
        vo.setAmount(request.getAmount());
        vo.setTransactionType("PAYMENT");
        vo.setIsBusinessExpense(isBusinessExpense);
        vo.setApprovedNumber(approvalNumber);
        vo.setTransactionNumber(TransactionNumberGenerator.generateTransactionNumber());
        vo.setStatus("PAID");
        vo.setApprovedAt(LocalDateTime.now());
        return vo;
    }

    public static TransactionVO forWalletPayment(Long userId, WalletVO wallet, PaymentRequest request) {
        TransactionVO vo = new TransactionVO();
        vo.setUserId(userId);
        vo.setWalletId(wallet.getId());
        vo.setReservationId(request.getReservationId());
        vo.setMerchantId(request.getMerchantId());
        vo.setPaymentSourceType("WALLET");
        vo.setMerchantName(request.getMerchantName());
        vo.setAmount(request.getAmount());
        vo.setTransactionType("PAYMENT");
        vo.setIsBusinessExpense(false);
        vo.setTransactionNumber(TransactionNumberGenerator.generateTransactionNumber());
        vo.setStatus("PAID");
        vo.setApprovedAt(LocalDateTime.now());
        return vo;
    }

    public static TransactionVO forAutoCharge(Long userId, WalletVO wallet, BankAccountVO account, BigDecimal shortage) {
        TransactionVO vo = new TransactionVO();
        vo.setUserId(userId);
        vo.setWalletId(wallet.getId());
        vo.setBankAccountId(account.getId());
        vo.setPaymentSourceType("WALLET");
        vo.setMerchantName("지갑 자동 충전");
        vo.setAmount(shortage);
        vo.setTransactionType("DEPOSIT");
        vo.setIsBusinessExpense(false);
        vo.setTransactionNumber(TransactionNumberGenerator.generateTransactionNumber());
        vo.setStatus("PAID");
        vo.setApprovedAt(LocalDateTime.now());
        return vo;
    }

    public static TransactionVO forWalletCharge(Long userId, WalletVO wallet, BankAccountVO account, BigDecimal amount) {
        TransactionVO vo = new TransactionVO();
        vo.setUserId(userId);
        vo.setWalletId(wallet.getId());
        vo.setBankAccountId(account.getId());
        vo.setPaymentSourceType("WALLET");
        vo.setMerchantName("지갑 충전");
        vo.setAmount(amount);
        vo.setTransactionType("DEPOSIT");
        vo.setIsBusinessExpense(false);
        vo.setTransactionNumber(TransactionNumberGenerator.generateTransactionNumber());
        vo.setStatus("PAID");
        vo.setApprovedAt(LocalDateTime.now());
        return vo;
    }

    public static TransactionVO forWalletRefund(Long userId, WalletVO wallet, BankAccountVO account, BigDecimal amount) {
        TransactionVO vo = new TransactionVO();
        vo.setUserId(userId);
        vo.setWalletId(wallet.getId());
        vo.setBankAccountId(account.getId());
        vo.setPaymentSourceType("WALLET");
        vo.setMerchantName("지갑 환불");
        vo.setAmount(amount);
        vo.setTransactionType("WITHDRAWAL");
        vo.setIsBusinessExpense(false);
        vo.setTransactionNumber(TransactionNumberGenerator.generateTransactionNumber());
        vo.setStatus("PAID");
        vo.setApprovedAt(LocalDateTime.now());
        return vo;
    }
}