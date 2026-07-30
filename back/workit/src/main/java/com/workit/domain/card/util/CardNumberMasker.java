package com.workit.domain.card.util;

public class CardNumberMasker {

    private CardNumberMasker() {
        // 유틸 클래스, 인스턴스화 방지
    }

    public static String mask(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 8) {
            return cardNumber;
        }
        int len = cardNumber.length();
        String front = cardNumber.substring(0, 4);
        String back = cardNumber.substring(len - 4);

        StringBuilder maskedBuilder = new StringBuilder();
        for (int i = 0; i < len - 8; i++) {
            maskedBuilder.append("*");
        }
        String masked = maskedBuilder.toString();

        return front + masked + back;
    }
}