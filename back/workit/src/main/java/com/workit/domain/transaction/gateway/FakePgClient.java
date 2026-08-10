package com.workit.domain.transaction.gateway;

import com.workit.domain.transaction.util.TransactionNumberGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 외부 PG를 흉내내는 목 구현.
 * - 인위적 네트워크 지연 주입 (pg.fake.latency-ms). 성능 테스트 시 0 으로 두면 순수 처리량 측정 가능.
 * - 확률적 승인 거절 / 매입 실패 주입 (authorize/capture-failure-rate) → 회복탄력성(보상) 경로 테스트용.
 * - 승인 시 PG 거래ID(pg_transaction_id)와 카드 승인번호(approval number)를 발급.
 *
 * 실제 PG 어댑터가 생기면 이 빈을 교체하면 된다({@link PaymentGatewayClient} 계약 동일).
 */
@Component
public class FakePgClient implements PaymentGatewayClient {

    /** 승인/매입 1건당 인위적 지연(ms). 기본 80. 성능 테스트 시 0 권장. */
    @Value("${pg.fake.latency-ms:80}")
    private long latencyMs;

    /** 승인 거절 확률(0.0~1.0). 기본 0(항상 승인). */
    @Value("${pg.fake.authorize-failure-rate:0.0}")
    private double authorizeFailureRate;

    /** 매입 실패 확률(0.0~1.0). 기본 0. 보상(cancel) 경로 테스트 시 올린다. */
    @Value("${pg.fake.capture-failure-rate:0.0}")
    private double captureFailureRate;

    @Override
    public PgAuthResult authorize(Long userId, Long cardId, BigDecimal amount, String merchantName) {
        simulateLatency();
        if (rolledFailure(authorizeFailureRate)) {
            throw new PgException(PgException.Type.DECLINED, "PG 승인 거절 (card=" + cardId + ", amount=" + amount + ")");
        }
        String pgTransactionId = "PG-" + UUID.randomUUID().toString().substring(0, 12);
        String approvalNumber = TransactionNumberGenerator.generateApprovalNumber();
        return PgAuthResult.of(pgTransactionId, approvalNumber);
    }

    @Override
    public void capture(String pgTransactionId, BigDecimal amount) {
        simulateLatency();
        if (rolledFailure(captureFailureRate)) {
            throw new PgException(PgException.Type.CAPTURE_FAILED, "PG 매입 실패 (pgTx=" + pgTransactionId + ")");
        }
    }

    @Override
    public void cancel(String pgTransactionId) {
        // 승인 취소(void)는 보상 경로. 목에서는 지연만 흉내내고 항상 성공 취급(멱등).
        simulateLatency();
    }

    private void simulateLatency() {
        if (latencyMs <= 0) {
            return;
        }
        // ±30% 지터
        long jitter = (long) (latencyMs * 0.3);
        long sleep = latencyMs + ThreadLocalRandom.current().nextLong(-jitter, jitter + 1);
        try {
            Thread.sleep(Math.max(0, sleep));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PgException(PgException.Type.TIMEOUT, "PG 응답 대기 중 인터럽트");
        }
    }

    private boolean rolledFailure(double rate) {
        return rate > 0 && ThreadLocalRandom.current().nextDouble() < rate;
    }
}
