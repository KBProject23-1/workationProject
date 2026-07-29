package com.workit.domain.account.util;

public final class AccountNumberMasker {

    private AccountNumberMasker() {
    }

    public static String mask(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 8) {
            return accountNumber;
        }
        int len = accountNumber.length();
        String front = accountNumber.substring(0, 3);
        String back = accountNumber.substring(len - 4);

        StringBuilder maskedBuilder = new StringBuilder();
        for (int i = 0; i < len - 7; i++) {
            maskedBuilder.append("*");
        }
        String masked = maskedBuilder.toString();

        return front + masked + back;
    }
}