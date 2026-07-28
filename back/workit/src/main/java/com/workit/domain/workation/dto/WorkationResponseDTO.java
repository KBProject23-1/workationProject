package com.workit.domain.workation.dto;

import com.workit.domain.workation.domain.Region;
import com.workit.domain.workation.domain.WorkationStatus;
import com.workit.domain.workation.domain.WorkationVO;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Getter
@Builder
public class WorkationResponseDTO {

    private Long id;
    private String title;
    private RegionDTO region;
    private LocalDate startDate;
    private LocalDate endDate;
    private int totalDays;
    private BigDecimal businessBudgetTotal;
    private BigDecimal personalBudgetTotal;
    private WorkationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static WorkationResponseDTO from(WorkationVO vo) {
        return WorkationResponseDTO.builder()
                .id(vo.getId())
                .title(vo.getTitle())
                .region(RegionDTO.from(vo.getRegion()))
                .startDate(vo.getStartDate())
                .endDate(vo.getEndDate())
                .totalDays((int) ChronoUnit.DAYS.between(vo.getStartDate(), vo.getEndDate()) + 1)
                .businessBudgetTotal(vo.getBusinessBudgetTotal())
                .personalBudgetTotal(vo.getPersonalBudgetTotal())
                .status(vo.getStatus())
                .createdAt(vo.getCreatedAt())
                .updatedAt(vo.getUpdatedAt())
                .build();
    }

    @Getter
    @Builder
    public static class RegionDTO {
        private Long id;
        private String name;

        public static RegionDTO from(Region region) {
            if (region == null) {
                return null;
            }
            return RegionDTO.builder()
                    .id(region.getId())
                    .name(region.getName())
                    .build();
        }
    }
}
