package com.workit.domain.settlement.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

// 6.1 정산 내역 조회 응답
@Getter
@Builder
public class SettlementResponseDTO {

    private final WorkationInfo workation;
    private final List<SettlementSummaryDTO> settlements;
    private final ValidationInfo validation;

    @Getter
    @Builder
    public static class WorkationInfo {
        private final Long id;
        private final String title;
        private final RegionInfo region;
        private final LocalDate startDate;
        private final LocalDate endDate;
        private final Integer totalDays;
    }

    @Getter
    @Builder
    public static class RegionInfo {
        private final Long id;
        private final String name;
    }

    @Getter
    @Builder
    public static class ValidationInfo {
        private final Integer uncheckedCount;
        private final Boolean canProceed;
        private final String message;
    }
}
