package com.workit.domain.category.dto.request;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// 2.2 카테고리 이름 변경 요청
// customName 을 null 로 보내면 별칭을 삭제하고 기본 이름으로 복원
@Getter
@Setter
@ToString
public class CategoryLabelRequestDTO {

    private String customName;
}
