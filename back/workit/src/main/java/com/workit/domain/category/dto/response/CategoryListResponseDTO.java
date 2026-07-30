package com.workit.domain.category.dto.response;

import com.workit.domain.workation.vo.BudgetType;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

// 카테고리 목록 응답
// 프론트가 여러 요청을 동시에 보냈을 때 구분할 수 있도록 budgetType 을 같이 반환
@Getter
@Builder
public class CategoryListResponseDTO {

    private BudgetType budgetType;
    private List<CategoryResponseDTO> categories;

    public static CategoryListResponseDTO of(BudgetType budgetType, List<CategoryResponseDTO> categories) {
        return CategoryListResponseDTO.builder()
                .budgetType(budgetType)
                .categories(categories)
                .build();
    }
}
