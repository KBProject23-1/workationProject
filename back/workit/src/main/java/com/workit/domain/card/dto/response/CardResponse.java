package com.workit.domain.card.dto.response;

import com.workit.domain.card.util.CardNumberMasker;
import com.workit.domain.card.vo.CardVO;
import lombok.Data;

@Data
public class CardResponse {
    private Long cardId;
    private String cardCompanyCode;
    private String cardCompanyName;
    private String cardName;
    private String maskedNumber;
    private String cardClassification;
    private String cardType;
    private Boolean isPrimary;
    private Boolean isDeleted;

    public static CardResponse from(CardVO vo) {
        CardResponse response = new CardResponse();
        response.setCardId(vo.getId());
        response.setCardCompanyCode(vo.getCardCompanyCode());
        response.setCardCompanyName(vo.getCardCompanyName());
        response.setCardName(vo.getCardName());
        response.setMaskedNumber(CardNumberMasker.mask(vo.getCardNumber()));
        response.setCardClassification(vo.getCardClassification());
        response.setCardType(vo.getCardType());
        response.setIsPrimary(vo.getIsPrimary());
        response.setIsDeleted(vo.getIsDeleted());
        return response;
    }
}