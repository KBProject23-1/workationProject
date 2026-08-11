package com.workit.domain.ledger.service;

import com.workit.domain.ledger.mapper.LedgerEntryMapper;
import com.workit.domain.ledger.vo.LedgerEntryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class LedgerServiceImpl implements LedgerService {

    private final LedgerEntryMapper ledgerEntryMapper;

    @Override
    public void post(Long transactionId, LedgerEntryVO debit, LedgerEntryVO credit) {
        validate(debit, credit);

        // 이미 기입된 건수 다음부터 seq 부여 — 최초 기입은 1,2 / 취소 역기입은 3,4 로 이어붙어 zero-sum 유지
        int base = ledgerEntryMapper.countByTransactionId(transactionId);
        debit.setTransactionId(transactionId);
        debit.setEntrySeq(base + 1);
        credit.setTransactionId(transactionId);
        credit.setEntrySeq(base + 2);

        ledgerEntryMapper.insertEntry(debit);
        ledgerEntryMapper.insertEntry(credit);
    }

    /** 방향(차변/대변)과 zero-sum(차변 금액 == 대변 금액) 불변식 검증. 위반 시 프로그래밍 오류로 롤백. */
    private void validate(LedgerEntryVO debit, LedgerEntryVO credit) {
        if (!LedgerEntryVO.DEBIT.equals(debit.getDirection())
                || !LedgerEntryVO.CREDIT.equals(credit.getDirection())) {
            throw new IllegalStateException("원장 post 는 (DEBIT, CREDIT) 순서여야 합니다.");
        }
        BigDecimal d = debit.getAmount();
        BigDecimal c = credit.getAmount();
        if (d == null || c == null || d.compareTo(c) != 0) {
            throw new IllegalStateException("원장 zero-sum 위반: debit=" + d + ", credit=" + c);
        }
    }
}
