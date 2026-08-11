package com.workit.domain.expense.dto.response;

import com.workit.domain.card.util.CardNumberMasker;
import com.workit.domain.expense.vo.ExpenseSourceType;
import com.workit.domain.expense.vo.WorkationExpenseVO;
import com.workit.domain.workation.vo.BudgetType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

// 지출 목록 한 줄 + 5.2 등록 응답
@Getter
@Builder
public class ExpenseItemResponseDTO {

    private final Long expenseId;
    private final Long merchantId;
    private final String merchantName;
    private final Long cardId;
    private final String cardName;
    private final String maskedCardNumber;
    private final BigDecimal amount;
    private final LocalDate spentDate;
    private final BudgetType budgetType;
    private final Long expenseCategoryId;
    private final String categoryName;
    private final Boolean isAutoCategorized;
    private final ExpenseSourceType sourceType;

    // CARD, WALLET. 지갑 결제는 카드가 없는 게 정상이라 카드 미지정과 구분해야 한다
    private final String paymentSourceType;

    private final String memo;

    public static ExpenseItemResponseDTO from(WorkationExpenseVO vo) {
        return ExpenseItemResponseDTO.builder()
                .expenseId(vo.getId())
                .merchantId(vo.getMerchantId())
                .merchantName(vo.getMerchantName())
                .cardId(vo.getCardId())
                .cardName(vo.getCardName())
                .maskedCardNumber(CardNumberMasker.mask(vo.getCardNumber()))
                .amount(vo.getAmount())
                .spentDate(vo.getSpentDate())
                .budgetType(vo.getBudgetType())
                .expenseCategoryId(vo.getExpenseCategoryId())
                .categoryName(vo.getDisplayCategoryName())
                .isAutoCategorized(vo.getIsAutoCategorized())
                .sourceType(vo.getSourceType())
                .paymentSourceType(vo.getPaymentSourceType())
                .memo(vo.getMemo())
                .build();
    }
}
