package com.workit.domain.notification.dto.response;

import com.workit.domain.notification.vo.NotificationVO;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// 개별 알림 정보 응답 DTO
@Getter
@Builder
public class NotificationDTO {

    /** 알림 고유 번호 */
    private Long notificationId;

    /** 알림 카테고리 */
    private String category;

    /** 중요 알림 여부 */
    private Boolean important;

    /** 알림 제목 */
    private String title;

    /** 알림 내용 */
    private String content;

    /** 참조 타입 (nullable) */
    private String referenceType;

    /** 참조 ID (nullable) */
    private Long referenceId;

    /** 읽음 여부 */
    private Boolean isRead;

    /** 생성 일시 */
    private LocalDateTime createdAt;

    public static NotificationDTO fromVO(NotificationVO vo) {
        return NotificationDTO.builder()
                .notificationId(vo.getNotificationId())
                .category(vo.getCategory())
                .important(vo.getImportant())
                .title(vo.getTitle())
                .content(vo.getContent())
                .referenceType(vo.getReferenceType())
                .referenceId(vo.getReferenceId())
                .isRead(vo.getIsRead())
                .createdAt(vo.getCreatedAt())
                .build();
    }
}
