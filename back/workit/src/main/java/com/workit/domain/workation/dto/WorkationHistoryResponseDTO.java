package com.workit.domain.workation.dto;

import com.workit.domain.workation.domain.WorkationHistoryVO;
import com.workit.domain.workation.domain.WorkationStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class WorkationHistoryResponseDTO {

    private Long id;
    private String title;
    private WorkationResponseDTO.RegionDTO region;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal businessSpentTotal;
    private BigDecimal personalSpentTotal;
    private WorkationStatus status;
    private LocalDateTime settledAt;

    public static WorkationHistoryResponseDTO from(WorkationHistoryVO vo) {
        return WorkationHistoryResponseDTO.builder()
                .id(vo.getId())
                .title(vo.getTitle())
                .region(WorkationResponseDTO.RegionDTO.from(vo.getRegion()))
                .startDate(vo.getStartDate())
                .endDate(vo.getEndDate())
                .businessSpentTotal(vo.getBusinessSpentTotal())
                .personalSpentTotal(vo.getPersonalSpentTotal())
                .status(vo.getStatus())
                .settledAt(vo.getSettledAt())
                .build();
    }
}