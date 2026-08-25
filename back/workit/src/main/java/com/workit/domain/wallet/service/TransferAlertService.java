package com.workit.domain.wallet.service;

import com.workit.domain.wallet.dto.response.ChargeResponse;
import com.workit.domain.wallet.dto.response.RefundResponse;

import java.math.BigDecimal;

// 입출금 알림 판단 서비스 인터페이스
// - 지갑 충전 완료 시 WALLET_CHARGE_SUCCESS 알림을 생성한다 (수동 충전 + 자동 충전)
// - 계좌 환불 완료 시 ACCOUNT_REFUND_SUCCESS 알림을 생성한다
// - NotificationCreateService를 통해 알림을 생성하며, 입출금 도메인에서 직접 notification_histories에 INSERT하지 않는다
// - 중복 알림 방지: notification_histories의 userId + referenceType + referenceId + notificationType을 활용한다
public interface TransferAlertService {

    /**
     * 지갑 충전 완료 알림 생성 여부를 판단하고, 필요시 NotificationCreateService를 통해 알림을 생성한다.
     * (수동 충전 전용 — ChargeResponse 기반)
     *
     * @param userId   사용자 ID
     * @param response 완료된 충전 응답 (PAID 상태)
     */
    void notifyChargeSuccess(Long userId, ChargeResponse response);

    /**
     * 지갑 충전 완료 알림 생성 여부를 판단하고, 필요시 NotificationCreateService를 통해 알림을 생성한다.
     * (자동 충전 전용 — transactionId + 충전 금액 기반)
     * TransactionVO.forAutoCharge()로 생성된 거래의 ID와 충전 금액을 직접 전달한다.
     *
     * @param userId      사용자 ID
     * @param transactionId 자동 충전으로 생성된 Transaction의 ID
     * @param chargedAmount 충전된 금액
     */
    void notifyChargeSuccess(Long userId, Long transactionId, BigDecimal chargedAmount);

    /**
     * 계좌 환불 완료 알림 생성 여부를 판단하고, 필요시 NotificationCreateService를 통해 알림을 생성한다.
     *
     * @param userId   사용자 ID
     * @param response 완료된 환불 응답 (PAID 상태)
     */
    void notifyRefundSuccess(Long userId, RefundResponse response);
}
