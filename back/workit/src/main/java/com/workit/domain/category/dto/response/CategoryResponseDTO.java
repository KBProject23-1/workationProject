package com.workit.domain.category.dto.response;

import com.workit.domain.category.vo.ExpenseCategoryVO;
import lombok.Builder;
import lombok.Getter;

// 카테고리 1건 응답
@Getter
@Builder
public class CategoryResponseDTO {

    private Long id;
    private String code;
    private String name;          // 별칭이 있으면 별칭, 없으면 기본 이름
    private String description;
    private Boolean isDefault;
    private Boolean isDeletable;
    private Integer sortOrder;

    public static CategoryResponseDTO from(ExpenseCategoryVO vo) {
        return CategoryResponseDTO.builder()
                .id(vo.getId())
                .code(vo.getCode())
                // 별칭이 있으면 별칭을 표시명으로 사용한다
                .name(vo.getCustomName() != null ? vo.getCustomName() : vo.getName())
                .description(vo.getDescription())
                .isDefault(vo.getIsDefault())
                .isDeletable(vo.getIsDeletable())
                .sortOrder(vo.getSortOrder())
                .build();
    }
}
