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
    private final String companyName;
    private final List<String> cardLabels;
    private final SettlementSummaryDTO summary;
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

    public BigDecimal getRemainTotal() {
        return getTargetTotal().subtract(getSpentTotal());
    }

    public long getAppPaymentCount() {
        return expenses.stream().filter(e -> e.getTransactionId() != null).count();
    }

    public long getCardRecordCount() {
        return expenses.stream()
                .filter(e -> e.getTransactionId() == null && e.getCardId() != null)
                .count();
    }

}
