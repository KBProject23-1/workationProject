package com.workit.domain.transaction.dto.response;

import com.workit.domain.card.util.CardNumberMasker;
import com.workit.domain.transaction.vo.TransactionVO;
import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Data
public class ReceiptResponse {
    private Long transactionId;
    private MerchantInfo merchant;
    private TransactionInfo transaction;
    private PaymentInfo payment;

    public static ReceiptResponse from(TransactionVO vo) {
        boolean isCard = "CARD".equals(vo.getPaymentSourceType());

        BigDecimal totalAmount = vo.getAmount();
        BigDecimal supplyAmount = totalAmount.divide(BigDecimal.valueOf(1.1), 0, RoundingMode.DOWN);
        BigDecimal vat = totalAmount.subtract(supplyAmount);

        ReceiptResponse response = new ReceiptResponse();
        response.setTransactionId(vo.getId());

        MerchantInfo merchantInfo = new MerchantInfo();
        merchantInfo.setName(vo.getMerchantName());
        merchantInfo.setBusinessNumber(vo.getMerchantTaxpayerNumber());
        merchantInfo.setAddress(vo.getMerchantAddress());
        merchantInfo.setPhoneNumber(vo.getMerchantPhoneNumber());
        response.setMerchant(merchantInfo);

        TransactionInfo txInfo = new TransactionInfo();
        txInfo.setPaymentMethod(vo.getPaymentSourceType());
        txInfo.setCardName(isCard ? vo.getCardName() : null);
        txInfo.setMaskedCardNumber(isCard ? CardNumberMasker.mask(vo.getCardNumber()) : null);
        txInfo.setApprovalStatus(isCard ? "매입" : null);
        txInfo.setApprovedAt(vo.getApprovedAt());
        txInfo.setApprovalNumber(isCard ? vo.getApprovedNumber() : null);
        response.setTransaction(txInfo);

        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setSupplyAmount(supplyAmount);
        paymentInfo.setVat(vat);
        paymentInfo.setTotalAmount(totalAmount);
        response.setPayment(paymentInfo);

        return response;
    }

    @lombok.Data
    public static class MerchantInfo {
        private String name;
        private String businessNumber;
        private String address;
        private String phoneNumber;
    }

    @lombok.Data
    public static class TransactionInfo {
        private String paymentMethod;
        private String cardName;
        private String maskedCardNumber;
        private String approvalStatus;
        private LocalDateTime approvedAt;
        private String approvalNumber;
    }

    @lombok.Data
    public static class PaymentInfo {
        private BigDecimal supplyAmount;
        private BigDecimal vat;
        private BigDecimal totalAmount;
    }
}