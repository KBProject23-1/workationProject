package com.workit.domain.settlement.dto.response;

import lombok.Builder;
import lombok.Getter;

import com.workit.domain.workation.vo.WorkationStatus;

import java.math.BigDecimal;
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
        // 클라이언트가 계정과목 배정액 합계로 총예산을 역산하지 않도록 서버가 직접 내려준다
        // (배정 합계와 총예산이 어긋난 상태에서 화면 금액이 틀어지는 것을 방지)
        private final BigDecimal businessBudgetTotal;
        private final BigDecimal personalBudgetTotal;
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
