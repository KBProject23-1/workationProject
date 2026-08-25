package com.workit.domain.account.util;

import com.workit.global.util.Masker;

public final class AccountNumberMasker {

    private static final int FRONT_LEN = 3;
    private static final int BACK_LEN = 4;

    private AccountNumberMasker() {
    }

    public static String mask(String accountNumber) {
        return Masker.mask(accountNumber, FRONT_LEN, BACK_LEN);
    }
}
