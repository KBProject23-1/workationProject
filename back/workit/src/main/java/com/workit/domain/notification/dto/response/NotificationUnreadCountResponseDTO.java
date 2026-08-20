package com.workit.domain.notification.dto.response;

import lombok.Builder;
import lombok.Getter;

// 읽지 않은 알림 개수 조회 응답 DTO
@Getter
@Builder
public class NotificationUnreadCountResponseDTO {

    /** 읽지 않은 알림 개수 */
    private int unreadCount;

    public static NotificationUnreadCountResponseDTO of(int unreadCount) {
        return NotificationUnreadCountResponseDTO.builder()
                .unreadCount(unreadCount)
                .build();
    }
}
