package com.workit.domain.settlement.vo;

import com.workit.domain.expense.vo.WorkationExpenseVO;
import com.workit.domain.settlement.dto.response.SettlementSummaryDTO;
import com.workit.domain.workation.vo.WorkationVO;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

// Excel / PDF 문서에 필요한 데이터를 한 번에 담는다
// 문서 생성기가 Mapper 를 직접 호출하지 않게 해서 조회 책임을 Service 에 둔다
@Getter
@Builder
public class SettlementDocumentVO {

    private final WorkationVO workation;
    private final String userName;
    private final List<String> cardLabels;          // "법인 신한카드 5310-****-****-1234"
    private final SettlementSummaryDTO summary;     // 법인 경비 집계
    private final List<WorkationExpenseVO> expenses;

    // 배정 예산 합계
    public BigDecimal getTargetTotal() {
        return summary.getCategories().stream()
                .map(SettlementSummaryDTO.CategoryItem::getTargetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getSpentTotal() {
        return summary.getTotalAmount();
    }

    // 차액 = 배정 예산 − 집행 금액. 음수면 예산 초과다
    public BigDecimal getRemainTotal() {
        return getTargetTotal().subtract(getSpentTotal());
    }

    // 앱 내 결제 건수. 매출전표가 자동 생성되어 증빙이 완료된 건
    public long getAppPaymentCount() {
        return expenses.stream().filter(e -> e.getTransactionId() != null).count();
    }

    // 실물 법인카드 결제 건수. 매출전표가 없어 카드 사용내역으로 갈음하는 건
    public long getCardRecordCount() {
        return expenses.stream()
                .filter(e -> e.getTransactionId() == null && e.getCardId() != null)
                .count();
    }

}
