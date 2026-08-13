package com.workit.domain.schedule.dto.response;

import com.workit.domain.schedule.vo.ScheduleVO;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// 일정 등록 응답 및 일정 상세
@Getter
@Builder
public class ScheduleDetailResponseDTO {

    private final Long scheduleId;
    private final Long workationId;
    private final Long merchantId;
    private final String merchantName;
    private final String merchantCategory;
    private final String address;
    private final String thumbnailUrl;
    private final LocalDateTime scheduledAt;

    public static ScheduleDetailResponseDTO from(ScheduleVO vo) {
        return ScheduleDetailResponseDTO.builder()
                .scheduleId(vo.getId())
                .workationId(vo.getWorkationId())
                .merchantId(vo.getMerchantId())
                .merchantName(vo.getMerchantName())
                .merchantCategory(vo.getMerchantCategory())
                .address(vo.getAddress())
                .thumbnailUrl(vo.getThumbnailUrl())
                .scheduledAt(vo.getScheduledAt())
                .build();
    }
}
