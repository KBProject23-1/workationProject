package com.workit.domain.expense.dto.response;

import com.workit.domain.card.util.CardNumberMasker;
import com.workit.domain.category.vo.ExpenseCategoryVO;
import com.workit.domain.expense.vo.ExpenseSourceType;
import com.workit.domain.expense.vo.WorkationExpenseVO;
import com.workit.domain.workation.vo.BudgetType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

// 5.3 상세조회 / 5.4 수정 응답
// availableCategories 를 함께 내려 프론트가 카테고리 변경 목록을 따로 호출하지 않게 한다
@Getter
@Builder
public class ExpenseDetailResponseDTO {

    private final Long expenseId;
    private final Long workationId;
    private final Long transactionId;
    private final MerchantInfo merchant;
    private final String merchantName;
    private final CardInfo card;
    private final BigDecimal amount;
    private final LocalDate spentDate;
    private final BudgetType budgetType;
    private final Long expenseCategoryId;
    private final String categoryName;
    private final Boolean isAutoCategorized;
    private final ExpenseSourceType sourceType;
    // 앱 내 결제 건만 값이 있다. 수기 입력 건은 null
    private final String approvedNumber;
    private final String memo;
    private final List<CategoryOption> availableCategories;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    @Getter
    @Builder
    public static class MerchantInfo {
        private final Long id;
        private final String name;
        private final String category;
    }

    @Getter
    @Builder
    public static class CardInfo {
        private final Long id;
        private final String cardName;
        private final String maskedNumber;
        private final String cardType;
    }

    @Getter
    @Builder
    public static class CategoryOption {
        private final Long id;
        private final String name;
        private final String description;
    }

    public static ExpenseDetailResponseDTO of(WorkationExpenseVO vo, List<ExpenseCategoryVO> categories) {

        return ExpenseDetailResponseDTO.builder()
                .expenseId(vo.getId())
                .workationId(vo.getWorkationId())
                .transactionId(vo.getTransactionId())
                .merchant(toMerchantInfo(vo))
                .merchantName(vo.getMerchantName())
                .card(toCardInfo(vo))
                .amount(vo.getAmount())
                .spentDate(vo.getSpentDate())
                .budgetType(vo.getBudgetType())
                .expenseCategoryId(vo.getExpenseCategoryId())
                .categoryName(vo.getDisplayCategoryName())
                .isAutoCategorized(vo.getIsAutoCategorized())
                .sourceType(vo.getSourceType())
                .approvedNumber(vo.getApprovedNumber())
                .memo(vo.getMemo())
                .availableCategories(toOptions(categories))
                .createdAt(vo.getCreatedAt())
                .updatedAt(vo.getUpdatedAt())
                .build();
    }

    // 수기 입력 건은 가맹점 정보가 없으므로 null 을 내려보낸다
    private static MerchantInfo toMerchantInfo(WorkationExpenseVO vo) {

        if (vo.getMerchantId() == null) {
            return null;
        }
        return MerchantInfo.builder()
                .id(vo.getMerchantId())
                .name(vo.getMerchantName())
                .category(vo.getMerchantCategory())
                .build();
    }

    private static CardInfo toCardInfo(WorkationExpenseVO vo) {

        if (vo.getCardId() == null) {
            return null;
        }
        return CardInfo.builder()
                .id(vo.getCardId())
                .cardName(vo.getCardName())
                .maskedNumber(CardNumberMasker.mask(vo.getCardNumber()))
                .cardType(vo.getCardType())
                .build();
    }

    private static List<CategoryOption> toOptions(List<ExpenseCategoryVO> categories) {

        if (categories == null) {
            return java.util.Collections.emptyList();
        }
        return categories.stream()
                .map(c -> CategoryOption.builder()
                        .id(c.getId())
                        // 별칭을 지정한 카테고리는 별칭으로 보여준다
                        .name(displayName(c))
                        .description(c.getDescription())
                        .build())
                .collect(Collectors.toList());
    }

    private static String displayName(ExpenseCategoryVO c) {
        return (c.getCustomName() != null && !c.getCustomName().trim().isEmpty())
                ? c.getCustomName()
                : c.getName();
    }
}
