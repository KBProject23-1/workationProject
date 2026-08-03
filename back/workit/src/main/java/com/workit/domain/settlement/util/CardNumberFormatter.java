package com.workit.domain.settlement.util;

import com.workit.domain.card.util.CardNumberMasker;


public class CardNumberFormatter {

    private CardNumberFormatter() {
        // 유틸 클래스, 인스턴스화 방지
    }

    public static String format(String cardNumber) {

        String masked = CardNumberMasker.mask(cardNumber);

        if (masked == null || masked.length() != 16) {
            return masked;
        }
        return masked.substring(0, 4) + "-" + masked.substring(4, 8) + "-"
                + masked.substring(8, 12) + "-" + masked.substring(12);
    }
}
