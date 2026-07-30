package com.workit.domain.card.dto.response;

import com.workit.domain.card.util.CardNumberMasker;
import com.workit.domain.card.vo.LinkableCardVO;
import lombok.Data;

@Data
public class AvailableCardResponse {
    private Long linkableCardId;
    private String cardCompanyCode;
    private String cardCompanyName;
    private String cardCompanyLogoUrl;
    private String cardName;
    private String maskedNumber;
    private String cardClassification;
    private String cardType;

    public static AvailableCardResponse from(LinkableCardVO vo) {
        AvailableCardResponse response = new AvailableCardResponse();
        response.setLinkableCardId(vo.getId());
        response.setCardCompanyCode(vo.getCardCompanyCode());
        response.setCardCompanyName(vo.getCardCompanyName());
        response.setCardCompanyLogoUrl(vo.getCardCompanyLogoUrl());
        response.setCardName(vo.getCardName());
        response.setMaskedNumber(CardNumberMasker.mask(vo.getCardNumber()));
        response.setCardClassification(vo.getCardClassification());
        response.setCardType(vo.getCardType());
        return response;
    }
}