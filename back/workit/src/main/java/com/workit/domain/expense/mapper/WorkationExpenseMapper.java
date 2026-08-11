package com.workit.domain.expense.mapper;

import com.workit.domain.expense.vo.ExpenseSummaryVO;
import com.workit.domain.expense.vo.WorkationExpenseVO;
import com.workit.domain.workation.vo.BudgetType;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface WorkationExpenseMapper {

    // 지출 목록. budgetType / expenseCategoryId / uncheckedOnly 는 모두 선택 필터
    List<WorkationExpenseVO> selectExpenseList(@Param("workationId") Long workationId,
                                               @Param("userId") Long userId,
                                               @Param("budgetType") BudgetType budgetType,
                                               @Param("expenseCategoryId") Long expenseCategoryId,
                                               @Param("uncheckedOnly") Boolean uncheckedOnly,
                                               @Param("offset") int offset,
                                               @Param("size") int size);

    // 목록 페이징용 전체 건수. 필터 조건이 목록과 같아야 한다
    long countExpenseList(@Param("workationId") Long workationId,
                          @Param("budgetType") BudgetType budgetType,
                          @Param("expenseCategoryId") Long expenseCategoryId,
                          @Param("uncheckedOnly") Boolean uncheckedOnly);

    // 계정과목 코드. 없으면 null
    String selectCategoryCode(@Param("expenseCategoryId") Long expenseCategoryId);

    // 상단 요약. 건수·금액은 목록과 같은 필터를 쓰고, 확인 필요 건수만 전체 기준이다
    ExpenseSummaryVO selectExpenseSummary(@Param("workationId") Long workationId,
                                         @Param("budgetType") BudgetType budgetType,
                                         @Param("expenseCategoryId") Long expenseCategoryId,
                                         @Param("uncheckedOnly") Boolean uncheckedOnly);

    // 지출 단건. 없으면 null
    WorkationExpenseVO selectExpenseById(@Param("expenseId") Long expenseId,
                                         @Param("userId") Long userId);

    int insertExpense(WorkationExpenseVO vo);

    int updateExpense(@Param("expenseId") Long expenseId,
                      @Param("merchantName") String merchantName,
                      @Param("cardId") Long cardId,
                      @Param("amount") BigDecimal amount,
                      @Param("spentDate") LocalDate spentDate,
                      @Param("memo") String memo);

    // 카테고리 변경. 사용자가 직접 고른 것이므로 is_auto_categorized 를 0 으로 내린다
    int updateExpenseCategory(@Param("expenseId") Long expenseId,
                              @Param("expenseCategoryId") Long expenseCategoryId);

    int updateExpenseBudgetType(@Param("expenseId") Long expenseId,
                                @Param("budgetType") BudgetType budgetType,
                                @Param("expenseCategoryId") Long expenseCategoryId);

    int deleteExpense(@Param("expenseId") Long expenseId);

    // 앱 내 결제 유입 대상
    // 예약 결제는 결제일과 무관하게 reservations.workation_id 로, 일반 결제는 기간으로 찾는다
    List<WorkationExpenseVO> selectImportTargets(@Param("workationId") Long workationId,
                                                 @Param("userId") Long userId,
                                                 @Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate);

    // 일괄 확정. 카테고리는 두고 is_auto_categorized 만 0 으로 내린다
    int confirmExpenses(@Param("workationId") Long workationId,
                        @Param("userId") Long userId,
                        @Param("expenseIds") List<Long> expenseIds);

    // 예산 유형 일괄 변경. 계정과목은 코드를 맞춰 옮기고, 대응이 없으면 기타로 보낸다
    int updateExpensesBudgetType(@Param("workationId") Long workationId,
                                 @Param("userId") Long userId,
                                 @Param("budgetType") BudgetType budgetType,
                                 @Param("expenseIds") List<Long> expenseIds);

    // 유입 대상 일괄 저장. UNIQUE(transaction_id) 로 중복 유입을 막는다
    int insertImportedExpenses(@Param("items") List<WorkationExpenseVO> items);

    // 카테고리가 해당 예산 유형에 속하는지 확인
    int countValidCategory(@Param("budgetType") BudgetType budgetType,
                           @Param("userId") Long userId,
                           @Param("expenseCategoryId") Long expenseCategoryId);

    // 기타 카테고리 id. 자동분류 실패 시 여기로 떨어뜨린다
    Long selectEtcCategoryId(@Param("budgetType") BudgetType budgetType);

    // 카드 소유자 검증까지 포함한 카드 조회
    String selectOwnedCardType(@Param("cardId") Long cardId,
                               @Param("userId") Long userId);

    // 5.6 정정 규칙 저장. 같은 가맹점·예산유형이면 카테고리만 갱신한다
    int upsertCategoryRule(@Param("userId") Long userId,
                           @Param("merchantId") Long merchantId,
                           @Param("expenseCategoryId") Long expenseCategoryId,
                           @Param("budgetType") BudgetType budgetType);
}
