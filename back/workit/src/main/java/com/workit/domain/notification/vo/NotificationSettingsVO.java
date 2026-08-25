package com.workit.domain.notification.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

// Notification 설정 조회 전용 VO (MyBatis ResultType — user_notification_settings 테이블)
@Getter
@Setter
@ToString
public class NotificationSettingsVO {

    /** 회원 고유번호 (PK, FK — users.id 참조) */
    private Long userId;

    /** 예산 경고 알림 ON(1) / OFF(0) — TINYINT(1) → Boolean */
    private Boolean budgetNotify;

    /** 충전/환불 알림 ON(1) / OFF(0) — TINYINT(1) → Boolean */
    private Boolean transferNotify;

    /** 결제 알림 ON(1) / OFF(0) — TINYINT(1) → Boolean */
    private Boolean paymentNotify;

    /** 워케이션 진행 파트 알림 ON(1) / OFF(0) — TINYINT(1) → Boolean */
    private Boolean workationNotify;

    /** 정산 알림 ON(1) / OFF(0) — TINYINT(1) → Boolean */
    private Boolean settlementNotify;

    /** 일정 알림 ON(1) / OFF(0) — TINYINT(1) → Boolean */
    private Boolean scheduleNotify;

    /** 알림 설정 변경 일시 */
    private LocalDateTime updatedAt;
}
