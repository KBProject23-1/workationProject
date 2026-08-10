package com.workit.domain.transaction.gateway;

import java.math.BigDecimal;

/**
 * 외부 결제망(PG) 경계.
 * 카드결제 흐름을 승인(authorize) → 매입(capture) 2단계로 나누고, 실패 시 승인 취소(cancel)로 보상한다.
 * 구현체를 인터페이스 뒤로 숨겨, 지금은 {@link FakePgClient}(목)을 쓰고 추후 실제 PG 어댑터로 교체 가능하다.
 */
public interface PaymentGatewayClient {

    /** 카드 승인(hold). 실패 시 {@link PgException}. */
    PgAuthResult authorize(Long userId, Long cardId, BigDecimal amount, String merchantName);

    /** 승인건 매입(capture). 실패 시 {@link PgException} → 호출측이 cancel 로 보상한다. */
    void capture(String pgTransactionId, BigDecimal amount);

    /** 승인 취소(void). 매입 실패 시 보상 트랜잭션으로 사용. 멱등하게 동작(항상 성공 취급). */
    void cancel(String pgTransactionId);
}
