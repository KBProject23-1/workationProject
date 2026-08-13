package com.workit.domain.schedule.dto.response;

import com.workit.domain.schedule.vo.ScheduleItemType;
import com.workit.domain.schedule.vo.ScheduleItemVO;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

// 스케줄러 한 줄
@Getter
@Builder
public class ScheduleItemResponseDTO {

    // 눌렀을 때 예약 상세로 갈지 일정 상세로 갈지 정한다
    private final ScheduleItemType itemType;

    // 예약이면 reservationId, 일정이면 scheduleId 만 값이 있다
    private final Long reservationId;
    private final Long scheduleId;

    private final Long merchantId;
    private final String merchantName;
    private final String merchantCategory;
    private final String address;
    private final String thumbnailUrl;

    // 숙소·공유오피스는 기간을 쓴다
    private final LocalDate startDate;
    private final LocalDate endDate;

    // 음식점·여가는 시각을 쓴다
    private final LocalTime scheduledTime;

    public static ScheduleItemResponseDTO from(ScheduleItemVO vo) {
        return ScheduleItemResponseDTO.builder()
                .itemType(vo.getItemType())
                .reservationId(vo.getReservationId())
                .scheduleId(vo.getScheduleId())
                .merchantId(vo.getMerchantId())
                .merchantName(vo.getMerchantName())
                .merchantCategory(vo.getMerchantCategory())
                .address(vo.getAddress())
                .thumbnailUrl(vo.getThumbnailUrl())
                .startDate(vo.getStartDate())
                .endDate(vo.getEndDate())
                // 날짜는 상위 묶음이 이미 갖고 있어 시각만 내려준다
                .scheduledTime(vo.getScheduledAt() == null ? null : vo.getScheduledAt().toLocalTime())
                .build();
    }
}
