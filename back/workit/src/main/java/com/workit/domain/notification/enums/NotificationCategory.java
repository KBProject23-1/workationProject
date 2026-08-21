package com.workit.domain.notification.enums;

import com.workit.domain.notification.vo.NotificationSettingsVO;

import java.util.function.Function;

/**
 * 알림 카테고리 Enum
 *
 * user_notification_settings 테이블의 알림 설정 컬럼과 1:1 매핑된다.
 * notification_histories.category 컬럼에도 동일한 문자열이 저장된다.
 *
 * 각 Enum 값은 해당 카테고리의 수신 설정 필드에 매핑되는 Getter 함수를持有한다.
 * 새 카테고리를 추가할 때 Enum만 추가하면 서비스 코드 수정 없이 동작한다.
 *
 * - BUDGET_NOTIFY   : 예산 경고 알림 (budget_notify)
 * - TRANSFER_NOTIFY : 충전/환불 알림 (transfer_notify)
 * - PAYMENT_NOTIFY  : 결제 알림 (payment_notify)
 * - WORKATION_NOTIFY: 워케이션 진행 알림 (workation_notify)
 * - SETTLEMENT_NOTIFY: 정산 알림 (settlement_notify)
 * - SCHEDULE_NOTIFY : 일정 알림 (schedule_notify)
 */
public enum NotificationCategory {

    BUDGET_NOTIFY(NotificationSettingsVO::getBudgetNotify),
    TRANSFER_NOTIFY(NotificationSettingsVO::getTransferNotify),
    PAYMENT_NOTIFY(NotificationSettingsVO::getPaymentNotify),
    WORKATION_NOTIFY(NotificationSettingsVO::getWorkationNotify),
    SETTLEMENT_NOTIFY(NotificationSettingsVO::getSettlementNotify),
    SCHEDULE_NOTIFY(NotificationSettingsVO::getScheduleNotify);

    private final Function<NotificationSettingsVO, Boolean> settingsGetter;

    NotificationCategory(Function<NotificationSettingsVO, Boolean> settingsGetter) {
        this.settingsGetter = settingsGetter;
    }

    /**
     * 해당 카테고리의 알림 수신 설정 값을 반환한다.
     *
     * @param settings 사용자 알림 수신 설정 VO
     * @return 알림 수신 설정 활성화 여부 (null이면 false로 처리)
     */
    public boolean isEnabled(NotificationSettingsVO settings) {
        if (settings == null) {
            return true; // 설정 없으면 기본 허용
        }
        return Boolean.TRUE.equals(settingsGetter.apply(settings));
    }
}
