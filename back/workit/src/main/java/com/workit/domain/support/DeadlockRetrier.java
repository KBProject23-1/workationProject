package com.workit.domain.support;

import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * 일시적 DB 예외(데드락 / 락 대기 타임아웃)에 대한 재시도 지원.
 *
 * 반드시 @Transactional 경계 "밖"(컨트롤러)에서 감싸 호출한다 — 그래야 매 시도가 새 트랜잭션으로 실행된다.
 * MySQL 데드락은 victim 트랜잭션이 통째로 롤백되므로, 같은 멱등키로 재시도해도 부분 반영이나 중복 없이 안전하다
 * (혹시 남아있어도 (user_id, idempotency_key) UNIQUE 가 409 로 막아줌).
 */
@Component
public class DeadlockRetrier {

    private static final int MAX_ATTEMPTS = 5;
    private static final long BASE_BACKOFF_MS = 25;

    public <T> T execute(Supplier<T> action) {
        int attempt = 1;
        while (true) {
            try {
                return action.get();
            } catch (TransientDataAccessException e) {
                // 데드락/락타임아웃 등 재시도 가능한 일시적 예외. 최대 시도 초과 시 그대로 전파.
                if (attempt >= MAX_ATTEMPTS) {
                    throw e;
                }
                backoff(attempt);
                attempt++;
            }
        }
    }

    /** 선형 백오프 + 지터(동시 재충돌 방지). */
    private void backoff(int attempt) {
        long base = BASE_BACKOFF_MS * attempt;
        long sleep = base + ThreadLocalRandom.current().nextLong(0, base + 1);
        try {
            Thread.sleep(sleep);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("재시도 대기 중 인터럽트", ie);
        }
    }
}
