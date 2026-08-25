package com.workit.domain.notification.dto.request;

import lombok.Getter;
import lombok.Setter;

// 알림 수신 설정 변경 요청 DTO
// - PATCH 방식: 전달된 필드만 변경, 미전달 필드는 기존 값 유지
// - 모든 필드는 선택 (Boolean wrapper type 사용으로 null 구분 가능)
// - 모든 필드가 null인 요청은 서비스 계층에서 400 Bad Request 처리
@Getter
@Setter
public class NotificationSettingsUpdateRequestDTO {

    /** 예산 알림 수신 여부 (선택) */
    private Boolean budgetNotify;

    /** 입출금 알림 수신 여부 (선택) */
    private Boolean transferNotify;

    /** 결제 알림 수신 여부 (선택) */
    private Boolean paymentNotify;

    /** 워케이션 알림 수신 여부 (선택) */
    private Boolean workationNotify;

    /** 정산 알림 수신 여부 (선택) */
    private Boolean settlementNotify;

    /** 일정 알림 수신 여부 (선택) */
    private Boolean scheduleNotify;

    /**
     * 변경할 필드가 하나 이상 있는지 확인한다.
     * 모든 필드가 null이면 false를 반환한다.
     *
     * @return 변경할 필드가 하나 이상 있으면 true
     */
    public boolean hasAnyField() {
        return budgetNotify != null
                || transferNotify != null
                || paymentNotify != null
                || workationNotify != null
                || settlementNotify != null
                || scheduleNotify != null;
    }
}
