package com.workit.domain.ledger.service;

import com.workit.domain.ledger.vo.LedgerEntryVO;

/**
 * 복식부기 원장 기입 서비스.
 * 서비스 계층이 legs(차변/대변)만 기술하면, 이중 기입·entry_seq 부여·zero-sum 검증을 캡슐화한다.
 * (기존에는 각 서비스가 insertEntry 를 두 번 호출하며 seq·zero-sum 을 직접 책임졌음)
 */
public interface LedgerService {

    /**
     * 한 거래에 대한 차변/대변 2건을 원장에 기입한다.
     * @param transactionId 대상 거래
     * @param debit  차변(DEBIT) leg — LedgerEntryVO.debit(...) 로 생성
     * @param credit 대변(CREDIT) leg — LedgerEntryVO.credit(...) 로 생성
     */
    void post(Long transactionId, LedgerEntryVO debit, LedgerEntryVO credit);
}
