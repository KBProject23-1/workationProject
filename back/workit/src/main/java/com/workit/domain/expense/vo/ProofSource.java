package com.workit.domain.expense.vo;

// 증빙이 무엇으로 완료되었는지 구분한다
public enum ProofSource {

    TRANSACTION,   // 앱 내 결제. 거래내역 자체가 증빙
    CARD_RECORD,   // 법인카드 수기 등록. 카드·가맹점·금액 기재로 증빙
    NONE           // 개인 지출이거나 증빙 미완료
}
