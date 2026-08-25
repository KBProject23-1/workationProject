package com.workit.domain.expense.controller;

import com.workit.domain.expense.dto.request.ExpenseBudgetTypeChangeRequestDTO;
import com.workit.domain.expense.dto.request.ExpenseCategoryChangeRequestDTO;
import com.workit.domain.expense.dto.request.ExpenseUpdateRequestDTO;
import com.workit.domain.expense.dto.response.ExpenseBudgetTypeChangeResponseDTO;
import com.workit.domain.expense.dto.response.ExpenseCategoryChangeResponseDTO;
import com.workit.domain.expense.dto.response.ExpenseDetailResponseDTO;
import com.workit.domain.expense.service.WorkationExpenseService;
import com.workit.global.dto.CommonResponse;
import com.workit.global.response.GlobalResponseFactory;
import com.workit.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// 지출 단건 조작
// 지출 id 만으로 대상이 특정되므로 워케이션 경로 없이 최상위 경로를 쓴다
@RestController
@RequestMapping("/api/v1/expenses/{expenseId}")
@RequiredArgsConstructor
@Slf4j
public class ExpenseController {

    private final WorkationExpenseService expenseService;

    // 5.3 지출 단건 상세조회
    @GetMapping
    public ResponseEntity<CommonResponse<ExpenseDetailResponseDTO>> expenseDetailGet(
            @CurrentUser Long userId,
            @PathVariable("expenseId") Long expenseId) {

        return GlobalResponseFactory.success(expenseService.getExpenseDetail(userId, expenseId));
    }

    // 5.4 지출 수정
    // 등록 폼을 재사용하므로 전체 필드를 받는다
    @PutMapping
    public ResponseEntity<CommonResponse<ExpenseDetailResponseDTO>> expenseModify(
            @CurrentUser Long userId,
            @PathVariable("expenseId") Long expenseId,
            @RequestBody ExpenseUpdateRequestDTO dto) {

        return GlobalResponseFactory.success(expenseService.modifyExpense(userId, expenseId, dto));
    }

    // 5.5 지출 삭제
    @DeleteMapping
    public ResponseEntity<Void> expenseRemove(
            @CurrentUser Long userId,
            @PathVariable("expenseId") Long expenseId) {

        expenseService.removeExpense(userId, expenseId);
        return GlobalResponseFactory.noContent();
    }

    // 5.6 지출 카테고리 수동 변경
    // 일부 필드만 바꾸므로 PATCH
    @PatchMapping("/category")
    public ResponseEntity<CommonResponse<ExpenseCategoryChangeResponseDTO>> expenseCategoryModify(
            @CurrentUser Long userId,
            @PathVariable("expenseId") Long expenseId,
            @RequestBody ExpenseCategoryChangeRequestDTO dto) {

        return GlobalResponseFactory.success(expenseService.modifyExpenseCategory(userId, expenseId, dto));
    }

    // 5.7 경비/개인소비 구분 변경
    @PatchMapping("/budget-type")
    public ResponseEntity<CommonResponse<ExpenseBudgetTypeChangeResponseDTO>> expenseBudgetTypeModify(
            @CurrentUser Long userId,
            @PathVariable("expenseId") Long expenseId,
            @RequestBody ExpenseBudgetTypeChangeRequestDTO dto) {

        return GlobalResponseFactory.success(expenseService.modifyExpenseBudgetType(userId, expenseId, dto));
    }
}