package com.workit.domain.category.dto.response;

import com.workit.domain.category.vo.ExpenseCategoryVO;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// 2.2 카테고리 이름 변경 응답
// 기본 이름과 별칭을 모두 내려주어 프론트가 "원래대로 되돌리기" 를 안내
@Getter
@Builder
public class CategoryLabelResponseDTO {

    private Long categoryId;
    private String defaultName;   // 마스터에 정의된 원래 이름
    private String customName;    // 사용자 별칭. 없으면 null
    private String displayName;   // 실제 화면에 표시할 이름
    private LocalDateTime updatedAt;

    public static CategoryLabelResponseDTO of(ExpenseCategoryVO vo, LocalDateTime updatedAt) {

        String customName = vo.getCustomName();

        return CategoryLabelResponseDTO.builder()
                .categoryId(vo.getId())
                .defaultName(vo.getName())
                .customName(customName)
                .displayName(customName != null ? customName : vo.getName())
                .updatedAt(updatedAt)
                .build();
    }
}
