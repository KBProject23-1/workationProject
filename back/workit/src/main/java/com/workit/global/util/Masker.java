package com.workit.global.util;

/**
 * 계좌번호/카드번호 등 식별 정보의 중간 구간을 '*' 로 가리는 공용 마스킹 유틸.
 */
public final class Masker {

    private Masker() {
    }

    /**
     * value 의 앞 frontLen 자, 뒤 backLen 자를 남기고 중간 구간을 '*' 로 마스킹한다.
     * value 길이가 frontLen+backLen 이하라 가릴 구간이 없으면 원본을 그대로 반환한다
     * (앞/뒤로 이미 다 드러나는 값을 별 0개로 "마스킹했다"고 속이지 않는다).
     */
    public static String mask(String value, int frontLen, int backLen) {
        if (value == null || value.length() <= frontLen + backLen) {
            return value;
        }
        int len = value.length();
        String front = value.substring(0, frontLen);
        String back = value.substring(len - backLen);

        StringBuilder maskedBuilder = new StringBuilder();
        for (int i = 0; i < len - frontLen - backLen; i++) {
            maskedBuilder.append("*");
        }

        return front + maskedBuilder + back;
    }
}
