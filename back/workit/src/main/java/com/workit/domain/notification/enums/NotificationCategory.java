package com.workit.domain.notification.enums;

/**
 * 알림 카테고리 Enum
 *
 * user_notification_settings 테이블의 알림 설정 컬럼과 1:1 매핑된다.
 * notification_histories.category 컬럼에도 동일한 문자열이 저장된다.
 *
 * - BUDGET_NOTIFY   : 예산 경고 알림 (budget_notify)
 * - TRANSFER_NOTIFY : 충전/환불 알림 (transfer_notify)
 * - PAYMENT_NOTIFY  : 결제 알림 (payment_notify)
 * - WORKATION_NOTIFY: 워케이션 진행 알림 (workation_notify)
 * - SETTLEMENT_NOTIFY: 정산 알림 (settlement_notify)
 * - SCHEDULE_NOTIFY : 일정 알림 (schedule_notify)
 */
public enum NotificationCategory {

    BUDGET_NOTIFY,
    TRANSFER_NOTIFY,
    PAYMENT_NOTIFY,
    WORKATION_NOTIFY,
    SETTLEMENT_NOTIFY,
    SCHEDULE_NOTIFY
}
