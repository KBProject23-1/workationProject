package com.workit.domain.wallet.service;

import com.workit.domain.notification.dto.request.NotificationCreateRequestDTO;
import com.workit.domain.notification.enums.NotificationCategory;
import com.workit.domain.notification.service.NotificationCreateService;
import com.workit.domain.wallet.dto.response.ChargeResponse;
import com.workit.domain.wallet.dto.response.RefundResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

// 입출금 알림 판단 서비스 구현체
// - 지갑 충전 완료 시 WALLET_CHARGE_SUCCESS 알림을 생성한다
// - 계좌 환불 완료 시 ACCOUNT_REFUND_SUCCESS 알림을 생성한다
// - NotificationCreateService를 통해 알림을 생성하며, 입출금 도메인에서 직접 notification_histories에 INSERT하지 않는다
// - 중복 알림 방지는 NotificationCreateServiceImpl.createNotification() 내부에서 원자적으로 처리한다
@Service
@RequiredArgsConstructor
@Slf4j
public class TransferAlertServiceImpl implements TransferAlertService {

    private static final String REFERENCE_TYPE = "TRANSACTION";
    private static final String NOTIFICATION_TYPE_CHARGE_SUCCESS = "WALLET_CHARGE_SUCCESS";
    private static final String NOTIFICATION_TYPE_REFUND_SUCCESS = "ACCOUNT_REFUND_SUCCESS";

    private final NotificationCreateService notificationCreateService;

    @Override
    public void notifyChargeSuccess(Long userId, ChargeResponse response) {
        if (response == null || response.getTransactionId() == null) {
            log.warn("충전 성공 알림 - 거래 정보 없음. userId={}", userId);
            return;
        }

        doNotifyChargeSuccess(userId, response.getTransactionId(), response.getChargedAmount());
    }

    @Override
    public void notifyChargeSuccess(Long userId, Long transactionId, BigDecimal chargedAmount) {
        if (transactionId == null) {
            log.warn("자동 충전 성공 알림 - 거래 ID 없음. userId={}", userId);
            return;
        }

        doNotifyChargeSuccess(userId, transactionId, chargedAmount);
    }

    @Override
    public void notifyRefundSuccess(Long userId, RefundResponse response) {
        if (response == null || response.getTransactionId() == null) {
            log.warn("환불 완료 알림 - 거래 정보 없음. userId={}", userId);
            return;
        }

        Long transactionId = response.getTransactionId();

        // placeholder 구성 — DB 템플릿의 {amount}를 실제 환불 금액으로 치환
        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put("amount", response.getRefundedAmount());

        NotificationCreateRequestDTO request = NotificationCreateRequestDTO.builder()
                .category(NotificationCategory.TRANSFER_NOTIFY)
                .notificationType(NOTIFICATION_TYPE_REFUND_SUCCESS)
                .important(true)
                .placeholders(placeholders)
                .referenceType(REFERENCE_TYPE)
                .referenceId(transactionId)
                .build();

        notificationCreateService.createNotification(userId, request);

        log.info("환불 완료 알림 생성 - userId={}, transactionId={}, amount={}",
                userId, transactionId, response.getRefundedAmount());
    }

    /**
     * 충전 성공 알림 생성 공통 로직 (수동 충전 + 자동 충전 공용)
     * - 중복 알림 방지는 NotificationCreateServiceImpl 내부에서 처리한다
     */
    private void doNotifyChargeSuccess(Long userId, Long transactionId, BigDecimal amount) {
        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put("amount", amount);

        NotificationCreateRequestDTO request = NotificationCreateRequestDTO.builder()
                .category(NotificationCategory.TRANSFER_NOTIFY)
                .notificationType(NOTIFICATION_TYPE_CHARGE_SUCCESS)
                .important(true)
                .placeholders(placeholders)
                .referenceType(REFERENCE_TYPE)
                .referenceId(transactionId)
                .build();

        notificationCreateService.createNotification(userId, request);

        log.info("충전 성공 알림 생성 - userId={}, transactionId={}, amount={}",
                userId, transactionId, amount);
    }
}
