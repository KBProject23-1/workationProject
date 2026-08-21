package com.workit.domain.payment.service;

import com.workit.domain.transaction.dto.response.PaymentResponse;

// 결제 알림 판단 서비스 인터페이스
// - 결제 성공/환불 완료 시 NotificationCreateService를 통해 알림을 생성한다
// - 결제 도메인에서 직접 notification_histories에 INSERT하지 않는다
// - 중복 알림 방지: notification_histories의 userId + referenceType + referenceId + notificationType을 활용한다
public interface PaymentAlertService {

    /**
     * 결제 성공 알림 생성 여부를 판단하고, 필요시 NotificationCreateService를 통해 알림을 생성한다.
     *
     * @param userId   사용자 ID
     * @param response 완료된 결제 응답 (PAID 상태)
     */
    void notifyPaymentSuccess(Long userId, PaymentResponse response);

    /**
     * 환불 완료 알림 생성 여부를 판단하고, 필요시 NotificationCreateService를 통해 알림을 생성한다.
     *
     * @param userId   사용자 ID
     * @param response 환불 완료된 결제 응답 (REFUNDED 상태)
     */
    void notifyRefundSuccess(Long userId, PaymentResponse response);
}
