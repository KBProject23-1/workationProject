package com.workit.domain.expense.vo;

import com.workit.domain.workation.vo.BudgetType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

// 지출 내역 + 조인으로 얻는 카테고리·카드·가맹점 정보
// merchantId 는 workation_expenses 에 저장하지 않고 transactions 를 따라가서 얻는다
@Getter
@Setter
@ToString
public class WorkationExpenseVO {

    private Long id;
    private Long workationId;
    private Long transactionId;      // 앱 내 결제에서 유입된 건만 값 존재
    private Long expenseCategoryId;
    private Long cardId;
    private BudgetType budgetType;
    private BigDecimal amount;
    private String merchantName;
    private LocalDate spentDate;
    private String memo;
    private Boolean isAutoCategorized;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 카테고리 조인
    private String categoryName;     // 마스터 기본 이름
    private String customName;       // 사용자 별칭. 없으면 null
    private String categoryDescription;

    // 카드 조인
    private String cardName;
    private String cardNumber;
    private String cardType;         // WORK, PERSONAL

    // 가맹점 조인 (transactions → merchants)
    private Long merchantId;
    private String merchantCategory; // ACCOMMODATION, RESTAURANT, OFFICE, ACTIVITY

    // 화면에 보여줄 카테고리 이름. 별칭이 있으면 별칭이 우선한다
    public String getDisplayCategoryName() {
        return (customName != null && !customName.trim().isEmpty()) ? customName : categoryName;
    }

    public ExpenseSourceType getSourceType() {
        return (transactionId != null) ? ExpenseSourceType.APP_PAYMENT : ExpenseSourceType.MANUAL;
    }

    // 증빙 판정은 저장하지 않고 매번 계산한다
    // 컬럼으로 두면 카드나 가맹점명이 바뀔 때 갱신을 빠뜨려 정산 경고가 틀어진다
    public ProofSource getProofSource() {

        if (transactionId != null) {
            return ProofSource.TRANSACTION;
        }
        // 법인 지출은 카드와 가맹점명이 모두 있어야 증빙으로 인정한다
        if (budgetType == BudgetType.WORK
                && cardId != null
                && merchantName != null && !merchantName.trim().isEmpty()) {
            return ProofSource.CARD_RECORD;
        }
        return ProofSource.NONE;
    }

    // 개인 지출은 법인 정산 대상이 아니므로 증빙 완료로 간주한다
    public boolean isProofCompleted() {
        return getProofSource() != ProofSource.NONE || budgetType == BudgetType.PERSONAL;
    }
}
