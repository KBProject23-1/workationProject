package com.workit.domain.expense.service;

import com.workit.domain.expense.mapper.WorkationExpenseMapper;
import com.workit.domain.expense.vo.WorkationExpenseVO;
import com.workit.domain.workation.vo.WorkationStatus;
import com.workit.domain.workation.vo.WorkationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 앱 내 결제를 워케이션 지출로 옮긴다.
// 지출 목록뿐 아니라 정산 조회·문서 출력에서도 필요해 별도 서비스로 분리했다.
// 지출 목록만 유입을 태우면, 목록을 한 번도 열지 않고 정산으로 간 사용자의
// 정산 문서에서 앱 결제가 통째로 빠진다.
@Service
@RequiredArgsConstructor
@Slf4j
public class ExpenseImportService {

    private final WorkationExpenseMapper expenseMapper;

    // transactions 는 결제 파트의 원본이므로 읽기만 하고 수정하지 않는다
    @Transactional
    public int importAppPayments(Long userId, WorkationVO workation) {

        // 정산이 끝나면 금액이 확정된 것으로 본다.
        // 여기서 막지 않으면 지난 기록을 열어볼 때마다 지출이 늘어 제출한 문서와 달라진다.
        if (workation.getStatus() == WorkationStatus.SETTLED) {
            return 0;
        }

        List<WorkationExpenseVO> targets = expenseMapper.selectImportTargets(
                workation.getId(), userId, workation.getStartDate(), workation.getEndDate());

        if (targets.isEmpty()) {
            return 0;
        }

        expenseMapper.insertImportedExpenses(targets);
        log.info("앱 결제 유입 완료 - workationId: {}, {}건", workation.getId(), targets.size());

        return targets.size();
    }
}
