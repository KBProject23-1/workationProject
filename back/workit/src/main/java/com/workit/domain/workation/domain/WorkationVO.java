package com.workit.domain.workation.domain;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
public class WorkationVO {

    private Long id;
    private Long userId;
    private Long regionId;
    private String title;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal businessBudgetTotal;
    private BigDecimal personalBudgetTotal;
    private WorkationStatus status;
    private LocalDateTime settledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // region 테이블 조인 결과
    private Region region;
}
