package com.workit.domain.budget.dto.request;

import com.workit.domain.workation.vo.BudgetType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

// 3.2 예산 세부 금액 설정 요청
// 예산 유형별로 따로 호출함
// 화면이 법인/개인 탭으로 나뉘어 있어 법인만 먼저 배분하고 개인은 나중에 하는 흐름이 가능해야 함
@Getter
@Setter
@ToString
public class BudgetSetupRequestDTO {

    private BudgetType budgetType;
    private List<BudgetItemRequestDTO> items;
}
