package com.workit.domain.category.service;

import com.workit.domain.category.dto.request.CategoryLabelRequestDTO;
import com.workit.domain.category.dto.response.CategoryLabelResponseDTO;
import com.workit.domain.category.dto.response.CategoryListResponseDTO;
import com.workit.domain.workation.vo.BudgetType;

public interface CategoryService {

    // 2.1 카테고리 목록 조회
    CategoryListResponseDTO getCategoryList(Long userId, BudgetType budgetType);

    // 2.2 카테고리 이름 변경
    CategoryLabelResponseDTO modifyCategoryLabel(Long userId, Long categoryId, CategoryLabelRequestDTO dto);
}
