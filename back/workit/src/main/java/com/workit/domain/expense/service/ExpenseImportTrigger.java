package com.workit.domain.expense.service;

import com.workit.domain.workation.mapper.WorkationMapper;
import com.workit.domain.workation.vo.WorkationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

// 결제가 끝난 직후 그 결제를 워케이션 지출로 옮긴다.
//
// 유입 자체는 지출·정산 조회 시점에도 돌지만, 그때까지는 결제한 내역이 지출 목록에 보이지 않는다.
// 결제 직후에 한 번 더 돌려 그 시차를 없앤다.
//
// 결제 파트가 워케이션을 알 필요가 없도록 워케이션 조회와 예외 처리를 여기서 감싼다.
@Component
@RequiredArgsConstructor
@Slf4j
public class ExpenseImportTrigger {

    private final WorkationMapper workationMapper;
    private final ExpenseImportService expenseImportService;

    // 결제 직후 호출한다.
    //
    // 호출부가 두 가지다.
    // - 단건 결제(TransactionController -> PaymentService.pay): 바깥 트랜잭션이 없다
    // - 예약 결제(ReservationServiceImpl.addReservation): 예약 트랜잭션 안에서 결제가 일어난다
    //
    // 예약 쪽에서 바로 유입시키면 두 가지가 걸린다.
    // 아직 커밋되지 않은 결제를 읽어야 하고, 유입이 실패하면 참여 중인 트랜잭션이
    // rollback-only 로 찍혀 예약까지 통째로 실패한다.
    // 그래서 바깥 트랜잭션이 있으면 커밋된 뒤로 미룬다.
    public void onPaymentCompleted(Long userId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    runImport(userId, true);
                }
            });
            return;
        }
        runImport(userId, false);
    }

    // 결제는 이미 끝난 뒤라 유입이 실패해도 예외를 밖으로 던지지 않는다.
    // 여기서 놓친 건은 지출·정산 조회 시점의 유입이 다시 가져간다.
    private void runImport(Long userId, boolean afterCommit) {
        try {
            WorkationVO workation = workationMapper.selectActiveWorkation(userId);
            if (workation == null) {
                return;
            }
            if (afterCommit) {
                expenseImportService.importAppPaymentsInNewTransaction(userId, workation);
            } else {
                expenseImportService.importAppPayments(userId, workation);
            }
        } catch (RuntimeException e) {
            log.warn("결제 후 지출 유입 실패 - userId: {}", userId, e);
        }
    }
}
