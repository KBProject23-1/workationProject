package com.workit.domain.notification.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

// 알림 목록 조회 응답 DTO
// API 스펙(docs): GET /api/v1/users/me/notifications → data { notifications, nextCursor, hasNext }
@Getter
@Builder
public class NotificationListResponseDTO {

    /** 알림 목록 */
    private List<NotificationDTO> notifications;

    /** 다음 조회에 사용할 cursor (hasNext가 false면 null) */
    private Long nextCursor;

    /** 다음 페이지 존재 여부 */
    private Boolean hasNext;

    public static NotificationListResponseDTO of(List<NotificationDTO> notifications,
                                                  Long nextCursor, Boolean hasNext) {
        return NotificationListResponseDTO.builder()
                .notifications(notifications)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .build();
    }
}
