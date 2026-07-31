package com.workit.domain.expense.controller;

import com.workit.domain.expense.dto.request.ExpenseCreateRequestDTO;
import com.workit.domain.expense.dto.response.ExpenseItemResponseDTO;
import com.workit.domain.expense.dto.response.ExpenseListResponseDTO;
import com.workit.domain.expense.service.WorkationExpenseService;
import com.workit.domain.workation.vo.BudgetType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// 워케이션에 속한 지출 목록·등록
// 단건 조작은 ExpenseController 가 담당한다
@RestController
@RequestMapping("/api/v1/workations/{workationId}/expenses")
@RequiredArgsConstructor
@Slf4j
public class WorkationExpenseController {

    private final WorkationExpenseService expenseService;

    // 5.1 지출 목록 조회
    // budgetType, expenseCategoryId, uncheckedOnly 는 선택 필터
    @GetMapping
    public ResponseEntity<ExpenseListResponseDTO> expenseListGet(
            @PathVariable("workationId") Long workationId,
            @RequestParam(value = "budgetType", required = false) BudgetType budgetType,
            @RequestParam(value = "expenseCategoryId", required = false) Long expenseCategoryId,
            @RequestParam(value = "uncheckedOnly", required = false) Boolean uncheckedOnly,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        // JWT 토큰에서 userId 추출로 교체 필요(추후 삭제)
        Long userId = 1L;

        return ResponseEntity.ok(expenseService.getExpenseList(
                userId, workationId, budgetType, expenseCategoryId, uncheckedOnly, page, size));
    }

    // 5.2 외부 결제내역 추가
    @PostMapping
    public ResponseEntity<ExpenseItemResponseDTO> expenseAdd(
            @PathVariable("workationId") Long workationId,
            @RequestBody ExpenseCreateRequestDTO dto) {

        // JWT 토큰에서 userId 추출로 교체 필요(추후 삭제)
        Long userId = 1L;

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(expenseService.addExpense(userId, workationId, dto));
    }
}
