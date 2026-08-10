package com.workit.domain.expense.service;

import com.workit.domain.expense.dto.request.ExpenseBudgetTypeChangeRequestDTO;
import com.workit.domain.expense.dto.request.ExpenseCategoryChangeRequestDTO;
import com.workit.domain.expense.dto.request.ExpenseConfirmRequestDTO;
import com.workit.domain.expense.dto.request.ExpenseCreateRequestDTO;
import com.workit.domain.expense.dto.request.ExpenseUpdateRequestDTO;
import com.workit.domain.expense.dto.response.ExpenseBudgetTypeChangeResponseDTO;
import com.workit.domain.expense.dto.response.ExpenseCategoryChangeResponseDTO;
import com.workit.domain.expense.dto.response.ExpenseConfirmResponseDTO;
import com.workit.domain.expense.dto.response.ExpenseDetailResponseDTO;
import com.workit.domain.expense.dto.response.ExpenseItemResponseDTO;
import com.workit.domain.expense.dto.response.ExpenseListResponseDTO;
import com.workit.domain.workation.vo.BudgetType;

public interface WorkationExpenseService {

    // 5.1 지출 목록 조회. 조회 시점에 앱 결제내역을 유입하고 카테고리를 자동분류한다
    ExpenseListResponseDTO getExpenseList(Long userId, Long workationId, BudgetType budgetType,
                                         Long expenseCategoryId, Boolean uncheckedOnly,
                                         int page, int size);

    // 5.2 외부 결제내역 추가
    ExpenseItemResponseDTO addExpense(Long userId, Long workationId, ExpenseCreateRequestDTO dto);

    // 5.3 지출 단건 상세조회
    ExpenseDetailResponseDTO getExpenseDetail(Long userId, Long expenseId);

    // 5.4 지출 수정. 앱 내 결제 건은 금액·일시·가맹점명 수정 불가
    ExpenseDetailResponseDTO modifyExpense(Long userId, Long expenseId, ExpenseUpdateRequestDTO dto);

    // 5.5 지출 삭제. 앱 내 결제 건은 삭제 불가
    void removeExpense(Long userId, Long expenseId);

    // 5.6 카테고리 수동 변경. 가맹점이 식별되면 정정 규칙을 저장한다
    ExpenseCategoryChangeResponseDTO modifyExpenseCategory(Long userId, Long expenseId,
                                                          ExpenseCategoryChangeRequestDTO dto);

    // 5.7 경비/개인소비 구분 변경
    ExpenseBudgetTypeChangeResponseDTO modifyExpenseBudgetType(Long userId, Long expenseId,
                                                              ExpenseBudgetTypeChangeRequestDTO dto);

    // 5.8 지출 일괄 확정. 자동분류 결과를 카테고리 변경 없이 승인한다
    ExpenseConfirmResponseDTO confirmExpenses(Long userId, Long workationId,
                                             ExpenseConfirmRequestDTO dto);
}
