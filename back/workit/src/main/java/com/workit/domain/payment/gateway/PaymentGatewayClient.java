package com.workit.domain.payment.gateway;

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

    /**
     * 승인/매입 취소. 멱등하게 동작(항상 성공 취급).
     * 매입(capture) 전 실패 시 승인 취소(void)로, 매입 후(=자금이 실제로 잡힌 뒤) 취소 시에는 환불(refund)로
     * 쓰인다 — 이 목/인터페이스는 두 경우를 하나의 호출로 묶어 두었다. 실제 PG 어댑터로 교체할 때는
     * 대개 이 두 케이스가 서로 다른 API(승인취소 vs 환불)이므로, 호출 시점이 capture 이전인지 이후인지에
     * 따라 적절히 분기해야 한다.
     */
    void cancel(String pgTransactionId);
}
