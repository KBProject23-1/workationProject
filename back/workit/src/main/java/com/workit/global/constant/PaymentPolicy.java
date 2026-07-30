package com.workit.global.constant;

import java.math.BigDecimal;

public class PaymentPolicy {

    public static final BigDecimal MIN_CHARGE_AMOUNT = BigDecimal.valueOf(10_000);
    public static final BigDecimal MAX_TRANSACTION_AMOUNT = BigDecimal.valueOf(2_000_000);

    private PaymentPolicy() {
    }
}