package com.workit.domain.payment;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 결제 거래 상태머신.
 * DB 컬럼(transactions.status)은 값 집합만 보유하고, 합법적인 상태 전이는 이 enum 에서 강제한다.
 *
 * 전이 규칙:
 *   내부(충전/환불/지갑결제):  REQUESTED -> PAID | FAILED
 *   카드결제(PG 2단계):        REQUESTED -> AUTHORIZED -> PAID | FAILED
 *                             AUTHORIZED -> CANCELED
 *   완료 후:                   PAID -> REFUNDED | CANCELED
 *   종료상태(전이 불가):        FAILED, CANCELED, REFUNDED
 */
public enum TransactionStatus {
    REQUESTED,
    AUTHORIZED,
    PAID,
    FAILED,
    CANCELED,
    REFUNDED;

    private static final Map<TransactionStatus, Set<TransactionStatus>> ALLOWED_TRANSITIONS =
            new EnumMap<>(TransactionStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(REQUESTED, EnumSet.of(AUTHORIZED, PAID, FAILED));
        ALLOWED_TRANSITIONS.put(AUTHORIZED, EnumSet.of(PAID, FAILED, CANCELED));
        ALLOWED_TRANSITIONS.put(PAID, EnumSet.of(REFUNDED, CANCELED));
        ALLOWED_TRANSITIONS.put(FAILED, EnumSet.noneOf(TransactionStatus.class));
        ALLOWED_TRANSITIONS.put(CANCELED, EnumSet.noneOf(TransactionStatus.class));
        ALLOWED_TRANSITIONS.put(REFUNDED, EnumSet.noneOf(TransactionStatus.class));
    }

    /** 이 상태에서 target 상태로 전이할 수 있는지 여부. */
    public boolean canTransitionTo(TransactionStatus target) {
        return ALLOWED_TRANSITIONS.getOrDefault(this, Collections.emptySet()).contains(target);
    }

    /** DB varchar <-> enum 변환용. 알 수 없는 값이면 예외. */
    public static TransactionStatus from(String value) {
        return TransactionStatus.valueOf(value);
    }
}
