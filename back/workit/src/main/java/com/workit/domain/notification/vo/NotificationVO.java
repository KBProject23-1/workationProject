package com.workit.domain.notification.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

// Notification 조회 전용 VO (MyBatis ResultType — notification_histories 테이블)
@Getter
@Setter
@ToString
public class NotificationVO {

    /** 알림 고유 번호 (notification_histories.id) */
    private Long notificationId;

    /** 알림 카테고리 (NotificationCategory enum 문자열: BUDGET_NOTIFY, TRANSFER_NOTIFY 등) */
    private String category;

    /** 중요 알림 여부 (TINYINT(1) → Boolean) */
    private Boolean important;

    /** 알림 제목 (VARCHAR(100)) */
    private String title;

    /** 알림 내용 (TEXT) */
    private String content;

    /** 참조 타입 (VARCHAR(30) — nullable) */
    private String referenceType;

    /** 참조 ID (BIGINT — nullable) */
    private Long referenceId;

    /** 읽음 여부 (TINYINT(1) → Boolean) */
    private Boolean isRead;

    /** 생성 일시 (DATETIME) */
    private LocalDateTime createdAt;
}
