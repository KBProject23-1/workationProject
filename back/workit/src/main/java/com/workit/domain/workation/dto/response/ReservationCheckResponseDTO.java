package com.workit.domain.workation.dto.response;

import com.workit.domain.workation.vo.WorkationReservationVO;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

// 1.8 기간 변경 영향 조회 응답
//
// 워케이션 기간을 바꾸기 전에 예약이 어떻게 어긋나는지 미리 보여준다.
// 예약을 고치지는 않고 무엇이 문제인지만 알려준다. 실제 취소·변경은 예약 화면에서 한다.
@Getter
@Builder
public class ReservationCheckResponseDTO {

    private final LocalDate startDate;
    private final LocalDate endDate;

    // 새 기간을 벗어나는 예약
    private final List<ReservationItem> outOfPeriod;

    // 숙소 예약이 없는 날. 기간을 늘렸을 때 추가 예약을 안내하는 데 쓴다
    private final List<LocalDate> uncoveredDates;

    // 화면 분기를 서버에서 정리해 내려준다
    private final boolean needsAction;

    // 삭제 화면용 요약.
    // 취소할 수 있는 예약(upcoming)이 있으면 먼저 취소해야 하고,
    // 이미 시작된 예약(ongoing)은 손댈 수 없으므로 그대로 두고 삭제한다.
    private final ReservationSummary upcoming;
    private final ReservationSummary ongoing;

    // 숙박과 공유오피스는 사용자가 다르게 받아들이므로 나눠서 센다
    @Getter
    @Builder
    public static class ReservationSummary {

        private final int room;
        private final int office;

        public int getTotal() {
            return room + office;
        }

        public boolean isEmpty() {
            return getTotal() == 0;
        }
    }

    @Getter
    @Builder
    public static class ReservationItem {

        private final Long reservationId;
        private final String reservationCode;
        private final String merchantName;
        private final String productName;
        private final String productDetailType;
        private final LocalDate startDate;
        private final LocalDate endDate;

        // 이용 시작일 당일부터는 취소할 수 없다 (예약 파트 정책)
        // 취소 가능 여부를 예약 파트가 내려주기 전까지는 날짜로만 판단한다
        private final boolean cancelable;

        public static ReservationItem from(WorkationReservationVO vo, LocalDate today) {
            return ReservationItem.builder()
                    .reservationId(vo.getReservationId())
                    .reservationCode(vo.getReservationCode())
                    .merchantName(vo.getMerchantName())
                    .productName(vo.getProductName())
                    .productDetailType(vo.getProductDetailType())
                    .startDate(vo.getStartDate())
                    .endDate(vo.getEndDate())
                    .cancelable(today.isBefore(vo.getStartDate()))
                    .build();
        }
    }
}
