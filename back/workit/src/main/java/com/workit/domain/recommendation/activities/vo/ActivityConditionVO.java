package com.workit.domain.recommendation.activities.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class ActivityConditionVO {
    private Long workationId;
    private Long regionId;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal leisureBudget;
    private String priorityOptionCode;
    private String priorityOptionName;
    private String selectedActivityCodes;
    private String selectedActivityNames;
}
