package com.workit.domain.category.vo;

import com.workit.domain.workation.vo.BudgetType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// 지출 카테고리 마스터 + 사용자 별칭 조인 결과
@Getter
@Setter
@ToString
public class ExpenseCategoryVO {

    private Long id;
    private BudgetType budgetType;
    private String code;
    private String name;           // 마스터 기본 이름
    private String description;
    private Boolean isDefault;
    private Boolean isDeletable;   // 0 = 삭제 불가 (기타 카테고리는 삭제 불가)
    private Integer sortOrder;

    private String customName;     // 사용자가 지정한 별칭. 없으면 null
}
