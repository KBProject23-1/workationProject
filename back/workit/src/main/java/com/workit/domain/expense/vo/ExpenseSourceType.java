package com.workit.domain.expense.vo;

// 지출이 어디서 들어왔는지 구분한다
// 저장 컬럼이 아니라 transaction_id 유무로 조회 시점에 판정한다
public enum ExpenseSourceType {

    APP_PAYMENT,  // 앱 내 결제에서 유입 (transaction_id 존재)
    MANUAL        // 사용자가 직접 등록
}
