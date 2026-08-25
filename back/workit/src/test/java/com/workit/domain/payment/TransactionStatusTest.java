package com.workit.domain.payment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 결제 상태머신(TransactionStatus)의 전이 규칙을 강제하는지 검증한다.
 *
 * 이 테스트가 지키는 계약:
 *   내부(충전/환불/지갑결제):  REQUESTED -> PAID | FAILED
 *   카드결제(PG 2단계):        REQUESTED -> AUTHORIZED -> PAID | FAILED,  AUTHORIZED -> CANCELED
 *   완료 후:                   PAID -> REFUNDED | CANCELED
 *   종료상태(전이 불가):        FAILED, CANCELED, REFUNDED
 *
 * 인프라(DB/목) 없이 순수 도메인 규칙만 검증하므로 빠르고 결정적이다.
 * (프로젝트에 junit-jupiter-params 가 없어 파라미터화 대신 평범한 @Test 로 작성)
 */
class TransactionStatusTest {

    @Nested
    @DisplayName("허용된 전이")
    class AllowedTransitions {

        @Test
        void 정의된_전이는_모두_허용된다() {
            assertAll(
                    () -> assertTrue(TransactionStatus.REQUESTED.canTransitionTo(TransactionStatus.AUTHORIZED)),
                    () -> assertTrue(TransactionStatus.REQUESTED.canTransitionTo(TransactionStatus.PAID)),
                    () -> assertTrue(TransactionStatus.REQUESTED.canTransitionTo(TransactionStatus.FAILED)),
                    () -> assertTrue(TransactionStatus.AUTHORIZED.canTransitionTo(TransactionStatus.PAID)),
                    () -> assertTrue(TransactionStatus.AUTHORIZED.canTransitionTo(TransactionStatus.FAILED)),
                    () -> assertTrue(TransactionStatus.AUTHORIZED.canTransitionTo(TransactionStatus.CANCELED)),
                    () -> assertTrue(TransactionStatus.PAID.canTransitionTo(TransactionStatus.REFUNDED)),
                    () -> assertTrue(TransactionStatus.PAID.canTransitionTo(TransactionStatus.CANCELED))
            );
        }
    }

    @Nested
    @DisplayName("거부된 전이")
    class DeniedTransitions {

        @Test
        void 정의되지_않은_전이는_거부된다() {
            assertAll(
                    // 승인/결제 단계를 건너뛰거나 되돌아가는 전이
                    () -> assertFalse(TransactionStatus.REQUESTED.canTransitionTo(TransactionStatus.REFUNDED)),
                    () -> assertFalse(TransactionStatus.REQUESTED.canTransitionTo(TransactionStatus.CANCELED)),
                    () -> assertFalse(TransactionStatus.AUTHORIZED.canTransitionTo(TransactionStatus.REQUESTED)),
                    () -> assertFalse(TransactionStatus.AUTHORIZED.canTransitionTo(TransactionStatus.REFUNDED)),
                    () -> assertFalse(TransactionStatus.PAID.canTransitionTo(TransactionStatus.AUTHORIZED)),
                    () -> assertFalse(TransactionStatus.PAID.canTransitionTo(TransactionStatus.REQUESTED)),
                    // 완료된 결제는 실패로 바뀔 수 없다
                    () -> assertFalse(TransactionStatus.PAID.canTransitionTo(TransactionStatus.FAILED))
            );
        }

        @Test
        void 같은_상태로의_자기전이는_모두_거부된다() {
            for (TransactionStatus s : TransactionStatus.values()) {
                assertFalse(s.canTransitionTo(s), s + " 는 자기 자신으로 전이할 수 없어야 한다");
            }
        }
    }

    @Nested
    @DisplayName("종료 상태")
    class TerminalStates {

        @Test
        void 종료상태는_어떤_전이도_불가하다() {
            Set<TransactionStatus> terminals = EnumSet.of(
                    TransactionStatus.FAILED, TransactionStatus.CANCELED, TransactionStatus.REFUNDED);

            for (TransactionStatus terminal : terminals) {
                for (TransactionStatus target : TransactionStatus.values()) {
                    assertFalse(terminal.canTransitionTo(target),
                            terminal + " 는 종료 상태이므로 " + target + " 로 전이할 수 없어야 한다");
                }
            }
        }
    }

    @Nested
    @DisplayName("DB 문자열 <-> enum 변환")
    class Conversion {

        @Test
        void 유효한_문자열은_enum으로_변환된다() {
            assertEquals(TransactionStatus.PAID, TransactionStatus.from("PAID"));
            assertEquals(TransactionStatus.CANCELED, TransactionStatus.from("CANCELED"));
        }

        @Test
        void 알수없는_문자열은_예외를_던진다() {
            assertAll(
                    () -> assertThrows(IllegalArgumentException.class, () -> TransactionStatus.from("UNKNOWN")),
                    () -> assertThrows(IllegalArgumentException.class, () -> TransactionStatus.from("paid")) // 대소문자 구분
            );
        }
    }
}
