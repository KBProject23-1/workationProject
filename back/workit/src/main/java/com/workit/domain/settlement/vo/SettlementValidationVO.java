package com.workit.domain.settlement.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// 정산 전 보완이 필요한 항목의 건수
// 막지는 않고 안내만 한다. 사용자가 확인 후 진행할 수 있다
@Getter
@Setter
@ToString
public class SettlementValidationVO {

    private Integer uncheckedCount;   // 자동분류 결과를 사용자가 확인하지 않은 건수
}
