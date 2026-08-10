package com.workit.domain.ledger.mapper;

import com.workit.domain.ledger.vo.LedgerEntryVO;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

public interface LedgerEntryMapper {

    // 원장 기입 (append-only, INSERT 만 사용)
    void insertEntry(LedgerEntryVO entry);

    // 특정 거래의 모든 기입 조회 (zero-sum 검증/영수증용)
    List<LedgerEntryVO> findByTransactionId(@Param("transactionId") Long transactionId);

    // 대사(reconciliation): 특정 계정의 원장상 잔액 = SUM(CREDIT) - SUM(DEBIT)
    BigDecimal computeAccountBalance(@Param("accountType") String accountType,
                                     @Param("accountRefId") Long accountRefId);
}
