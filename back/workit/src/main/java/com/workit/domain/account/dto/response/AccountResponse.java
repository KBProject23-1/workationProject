package com.workit.domain.account.dto.response;

import com.workit.domain.account.util.AccountNumberMasker;
import com.workit.domain.account.vo.BankAccountVO;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AccountResponse {
    private Long accountId;
    private String bankCode;
    private String maskedAccountNumber;
    private String productName;
    private String bankName;
    private BigDecimal balance;
    private Boolean isPrimary;
    private LocalDateTime withdrawalAgreedAt;
    private LocalDateTime balanceUpdatedAt;

    public static AccountResponse from(BankAccountVO vo) {
        AccountResponse response = new AccountResponse();
        response.setAccountId(vo.getId());
        response.setBankCode(vo.getBankCode());
        response.setBankName(vo.getBankName());
        response.setMaskedAccountNumber(AccountNumberMasker.mask(vo.getAccountNumber()));
        response.setProductName(vo.getProductName());
        response.setBalance(vo.getBalance());
        response.setIsPrimary(vo.getIsPrimary());
        response.setWithdrawalAgreedAt(vo.getWithdrawalAgreedAt());
        response.setBalanceUpdatedAt(vo.getBalanceUpdatedAt());
        return response;
    }
}