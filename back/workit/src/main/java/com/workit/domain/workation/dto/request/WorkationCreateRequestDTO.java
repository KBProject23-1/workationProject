package com.workit.domain.workation.dto.request;

import com.workit.domain.workation.vo.WorkationVO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@ToString
public class WorkationCreateRequestDTO {

    private String title;
    private Long regionId;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal businessBudgetTotal;
    private BigDecimal personalBudgetTotal;

    public WorkationVO toVO(Long userId) {
        WorkationVO vo = new WorkationVO();
        vo.setUserId(userId);
        vo.setRegionId(regionId);
        vo.setTitle(title);
        vo.setStartDate(startDate);
        vo.setEndDate(endDate);
        vo.setBusinessBudgetTotal(businessBudgetTotal);
        vo.setPersonalBudgetTotal(personalBudgetTotal);
        return vo;
    }
}
