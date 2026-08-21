package com.workit.domain.notification.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

// Notification 조회 전용 VO (MyBatis ResultType — notification_templates 테이블)
@Getter
@Setter
@ToString
public class NotificationTemplateVO {

    /** 알림 템플릿 고유 번호 (PK) */
    private Long id;

    /** 알림 카테고리 (NotificationCategory enum 문자열: BUDGET_NOTIFY, TRANSFER_NOTIFY 등) */
    private String category;

    /** 알림 세부 유형 (VARCHAR — 예: WORK_ACCOMMODATION_80_PERCENT) */
    private String notificationType;

    /** 알림 제목 템플릿 (VARCHAR(255)) */
    private String titleTemplate;

    /** 알림 내용 템플릿 (TEXT) */
    private String contentTemplate;

    /** 템플릿 활성화 여부 (TINYINT(1) → Boolean) */
    private Boolean isActive;

    /** 생성 일시 */
    private LocalDateTime createdAt;

    /** 수정 일시 */
    private LocalDateTime updatedAt;
}
