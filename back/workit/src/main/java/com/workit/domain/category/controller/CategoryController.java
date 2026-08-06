package com.workit.domain.category.controller;

import com.workit.domain.category.dto.request.CategoryLabelRequestDTO;
import com.workit.domain.category.dto.response.CategoryLabelResponseDTO;
import com.workit.domain.category.dto.response.CategoryListResponseDTO;
import com.workit.domain.category.service.CategoryService;
import com.workit.domain.workation.vo.BudgetType;
import com.workit.global.dto.CommonResponse;
import com.workit.global.response.GlobalResponseFactory;
import com.workit.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/expense-categories")
@RequiredArgsConstructor
@Slf4j
public class CategoryController {

    private final CategoryService categoryService;

    // 2.1 카테고리 목록 조회
    // budgetType 을 enum 으로 직접 받으므로 잘못된 값이 오면 Spring 이 예외를 던지고
    // CommonExceptionAdvice 가 400 으로 응답한다
    @GetMapping
    public ResponseEntity<CommonResponse<CategoryListResponseDTO>> categoryListGet(
            @CurrentUser Long userId,
            @RequestParam("budgetType") BudgetType budgetType) {

        return GlobalResponseFactory.success(categoryService.getCategoryList(userId, budgetType));
    }

    // 2.2 카테고리 이름 변경
    @PatchMapping("/{categoryId}/label")
    public ResponseEntity<CommonResponse<CategoryLabelResponseDTO>> categoryLabelModify(
            @CurrentUser Long userId,
            @PathVariable("categoryId") Long categoryId,
            @RequestBody CategoryLabelRequestDTO dto) {

        return GlobalResponseFactory.success(categoryService.modifyCategoryLabel(userId, categoryId, dto));
    }
}