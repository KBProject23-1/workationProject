package com.workit.domain.transaction.gateway;

import lombok.Getter;

/**
 * PG 승인 성공 결과. PG가 발급한 거래 식별자와 카드 승인번호를 담는다.
 */
@Getter
public class PgAuthResult {

    private final String pgTransactionId;
    private final String approvalNumber;

    private PgAuthResult(String pgTransactionId, String approvalNumber) {
        this.pgTransactionId = pgTransactionId;
        this.approvalNumber = approvalNumber;
    }

    public static PgAuthResult of(String pgTransactionId, String approvalNumber) {
        return new PgAuthResult(pgTransactionId, approvalNumber);
    }
}
