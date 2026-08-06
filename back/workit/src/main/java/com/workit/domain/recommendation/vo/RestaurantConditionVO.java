package com.workit.domain.recommendation.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class RestaurantConditionVO {
    private Long workationId;
    private Long regionId;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal foodBudget;
    private String priorityOptionCode;
    private String priorityOptionName;
    private String mealStyleOptionCode;
    private String mealStyleOptionName;
}
