package com.workit.domain.card.util;

import com.workit.global.util.Masker;

public class CardNumberMasker {

    private static final int FRONT_LEN = 4;
    private static final int BACK_LEN = 4;

    private CardNumberMasker() {
        // 유틸 클래스, 인스턴스화 방지
    }

    public static String mask(String cardNumber) {
        return Masker.mask(cardNumber, FRONT_LEN, BACK_LEN);
    }
}
