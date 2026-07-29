package com.workit.domain.workation.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
public class WorkationHistoryVO {

    private Long id;
    private String title;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal businessSpentTotal;
    private BigDecimal personalSpentTotal;
    private WorkationStatus status;
    private LocalDateTime settledAt;

    private Region region;   // 조인 결과
}