package com.workit.domain.account.dto.response;

import com.workit.domain.account.util.AccountNumberMasker;
import com.workit.domain.account.vo.LinkableAccountVO;
import lombok.Data;

@Data
public class AvailableAccountResponse {
    private Long linkableAccountId;
    private String bankCode;
    private String bankName;
    private String bankLogoUrl;
    private String maskedAccountNumber;
    private String productName;

    public static AvailableAccountResponse from(LinkableAccountVO vo) {
        AvailableAccountResponse response = new AvailableAccountResponse();
        response.setLinkableAccountId(vo.getId());
        response.setBankCode(vo.getBankCode());
        response.setBankName(vo.getBankName());
        response.setBankLogoUrl(vo.getBankLogoUrl());
        response.setMaskedAccountNumber(AccountNumberMasker.mask(vo.getAccountNumber()));
        response.setProductName(vo.getProductName());
        return response;
    }
}