package com.workit.domain.ledger.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 복식부기 원장 기입(불변, append-only).
 * 규칙: CREDIT = 잔액 증가(들어옴), DEBIT = 잔액 감소(나감).
 *       한 거래(transactionId)의 SUM(DEBIT) == SUM(CREDIT) 이 항상 성립(zero-sum).
 * 잔액 컬럼(wallets/bank_accounts)은 성능용 캐시이고, 이 원장이 진실(source of truth).
 */
@Data
public class LedgerEntryVO {

    // account_type 값
    public static final String ACCOUNT_WALLET = "WALLET";
    public static final String ACCOUNT_BANK = "BANK";
    public static final String ACCOUNT_MERCHANT = "MERCHANT";
    public static final String ACCOUNT_CARD = "CARD";
    public static final String ACCOUNT_SYSTEM = "SYSTEM";

    // direction 값
    public static final String DEBIT = "DEBIT";
    public static final String CREDIT = "CREDIT";

    private Long id;
    private Long transactionId;
    private Integer entrySeq;
    private String accountType;
    private Long accountRefId;
    private String direction;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private LocalDateTime createdAt;

    /** 잔액 감소(나감) leg. transactionId/entrySeq 는 LedgerService.post 가 부여한다. */
    public static LedgerEntryVO debit(String accountType, Long accountRefId,
                                      BigDecimal amount, BigDecimal balanceAfter) {
        return of(accountType, accountRefId, DEBIT, amount, balanceAfter);
    }

    /** 잔액 증가(들어옴) leg. transactionId/entrySeq 는 LedgerService.post 가 부여한다. */
    public static LedgerEntryVO credit(String accountType, Long accountRefId,
                                       BigDecimal amount, BigDecimal balanceAfter) {
        return of(accountType, accountRefId, CREDIT, amount, balanceAfter);
    }

    private static LedgerEntryVO of(String accountType, Long accountRefId,
                                    String direction, BigDecimal amount, BigDecimal balanceAfter) {
        LedgerEntryVO e = new LedgerEntryVO();
        e.setAccountType(accountType);
        e.setAccountRefId(accountRefId);
        e.setDirection(direction);
        e.setAmount(amount);
        e.setBalanceAfter(balanceAfter);
        return e;
    }
}
