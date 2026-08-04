package com.workit.domain.settlement.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class SettlementValidationVO {

    private Integer uncheckedCount;   // 자동분류 결과를 사용자가 확인하지 않은 건수
}
