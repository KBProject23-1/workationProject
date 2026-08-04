package com.workit.domain.budget.controller;

import com.workit.domain.budget.dto.request.BudgetItemAddRequestDTO;
import com.workit.domain.budget.dto.request.BudgetSetupRequestDTO;
import com.workit.domain.budget.dto.response.BudgetItemResponseDTO;
import com.workit.domain.budget.dto.response.BudgetStatusResponseDTO;
import com.workit.domain.budget.dto.response.BudgetSummaryResponseDTO;
import com.workit.domain.budget.service.BudgetService;
import com.workit.domain.workation.vo.BudgetType;
import com.workit.global.dto.CommonResponse;
import com.workit.global.response.GlobalResponseFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/workations/{workationId}/budgets")
@RequiredArgsConstructor
@Slf4j
public class BudgetController {

    private final BudgetService budgetService;

    // 3.1 예산 사용현황 조회
    // budgetType 은 선택 파라미터로, 없으면 법인·개인 모두 반환한다
    // enum 으로 직접 받으므로 잘못된 값은 Spring 이 예외를 던지고 공통 Advice 가 400 으로 응답한다
    @GetMapping
    public ResponseEntity<CommonResponse<BudgetStatusResponseDTO>> budgetStatusGet(
            @PathVariable("workationId") Long workationId,
            @RequestParam(value = "budgetType", required = false) BudgetType budgetType) {

        // JWT 토큰에서 userId 추출로 교체 필요(추후 삭제)
        Long userId = 1L;

        return GlobalResponseFactory.success(budgetService.getBudgetStatus(userId, workationId, budgetType));
    }

    // 3.2 예산 카테고리별 세부 금액 설정
    // 새로 배분하는 것이므로 POST + 201. 이미 설정돼 있으면 409 로 수정 API 를 안내한다
    @PostMapping
    public ResponseEntity<CommonResponse<BudgetSummaryResponseDTO>> budgetSetupAdd(
            @PathVariable("workationId") Long workationId,
            @RequestBody BudgetSetupRequestDTO dto) {

        // JWT 토큰에서 userId 추출로 교체 필요(추후 삭제)
        Long userId = 1L;

        return GlobalResponseFactory.created(budgetService.setupBudget(userId, workationId, dto));
    }

    // 3.3 예산 세부 금액 수정
    // 해당 예산 유형의 배분 전체를 덮어쓰므로 PUT
    @PutMapping
    public ResponseEntity<CommonResponse<BudgetSummaryResponseDTO>> budgetModify(
            @PathVariable("workationId") Long workationId,
            @RequestBody BudgetSetupRequestDTO dto) {

        // JWT 토큰에서 userId 추출로 교체 필요(추후 삭제)
        Long userId = 1L;

        return GlobalResponseFactory.success(budgetService.modifyBudget(userId, workationId, dto));
    }

    // 3.4 예산 카테고리 추가 (＋ 버튼)
    // 예산 배분 행 하나를 새로 만드는 것이므로 하위 경로 /items 에 POST + 201
    @PostMapping("/items")
    public ResponseEntity<CommonResponse<BudgetItemResponseDTO>> budgetItemAdd(
            @PathVariable("workationId") Long workationId,
            @RequestBody BudgetItemAddRequestDTO dto) {

        // JWT 토큰에서 userId 추출로 교체 필요(추후 삭제)
        Long userId = 1L;

        return GlobalResponseFactory.created(budgetService.addBudgetItem(userId, workationId, dto));
    }

    // 3.5 예산 카테고리 삭제 (− 버튼)
    // force=true 면 지출이 있어도 예산 배정만 삭제한다. 지출 내역은 유지된다
    @DeleteMapping("/items/{budgetId}")
    public ResponseEntity<Void> budgetItemRemove(
            @PathVariable("workationId") Long workationId,
            @PathVariable("budgetId") Long budgetId,
            @RequestParam(value = "force", defaultValue = "false") boolean force) {

        // JWT 토큰에서 userId 추출로 교체 필요(추후 삭제)
        Long userId = 1L;

        budgetService.removeBudgetItem(userId, workationId, budgetId, force);
        return GlobalResponseFactory.noContent();
    }
}
