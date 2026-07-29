package com.workit.domain.workation.dto.request;

import com.workit.domain.workation.vo.WorkationVO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;

// 1.4 워케이션 수정 요청(PUT으로 전체 필드 덮어씀)
@Getter
@Setter
@ToString
public class WorkationUpdateRequestDTO {

    private String title;
    private Long regionId;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal businessBudgetTotal;
    private BigDecimal personalBudgetTotal;

    // 수정 대상 id 와 함께 VO 로 변환
    public WorkationVO toVO(Long workationId) {
        WorkationVO vo = new WorkationVO();
        vo.setId(workationId);
        vo.setTitle(title);
        vo.setRegionId(regionId);
        vo.setStartDate(startDate);
        vo.setEndDate(endDate);
        vo.setBusinessBudgetTotal(businessBudgetTotal);
        vo.setPersonalBudgetTotal(personalBudgetTotal);
        return vo;
    }
}