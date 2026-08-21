package com.workit.domain.workation.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// 워케이션별 미확인 지출 건수 조회 결과 VO
// - is_auto_categorized = 1인 지출 건수를 워케이션 단위로 집계
// - SettlementAlertScheduler에서 미확인 지출 알림 대상 조회에 사용
@Getter
@Setter
@ToString
public class WorkationUncheckedCountVO {

    /** 회원 고유 번호 */
    private Long userId;

    /** 워케이션 고유 번호 */
    private Long workationId;

    /** 미확인 지출 건수 (is_auto_categorized = 1) */
    private int uncheckedCount;
}
