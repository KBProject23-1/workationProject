package com.workit.domain.settlement.dto.response;

import lombok.Builder;
import lombok.Getter;

import com.workit.domain.workation.vo.WorkationStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
        // 지난 워케이션 상세 화면에서 정산완료 배지와 정산일을 표시한다
        private final WorkationStatus status;
        private final LocalDateTime settledAt;
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
