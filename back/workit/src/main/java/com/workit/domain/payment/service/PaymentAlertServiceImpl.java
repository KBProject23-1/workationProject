package com.workit.domain.payment.service;

import com.workit.domain.notification.dto.request.NotificationCreateRequestDTO;
import com.workit.domain.notification.enums.NotificationCategory;
import com.workit.domain.notification.service.NotificationCreateService;
import com.workit.domain.transaction.dto.response.PaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

// 결제 알림 판단 서비스 구현체
// - 결제 성공 시 PAYMENT_SUCCESS 알림을 생성한다
// - 환불 완료 시 REFUND_SUCCESS 알림을 생성한다
// - NotificationCreateService를 통해 알림을 생성하며, 결제 도메인에서 직접 INSERT하지 않는다
// - 중복 알림 방지는 NotificationCreateServiceImpl.createNotification() 내부에서 원자적으로 처리한다
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentAlertServiceImpl implements PaymentAlertService {

    private static final String REFERENCE_TYPE = "TRANSACTION";
    private static final String NOTIFICATION_TYPE_PAYMENT_SUCCESS = "PAYMENT_SUCCESS";
    private static final String NOTIFICATION_TYPE_REFUND_SUCCESS = "REFUND_SUCCESS";

    private final NotificationCreateService notificationCreateService;

    @Override
    public void notifyPaymentSuccess(Long userId, PaymentResponse response) {
        if (response == null || response.getTransactionId() == null) {
            log.warn("결제 성공 알림 - 거래 정보 없음. userId={}", userId);
            return;
        }

        // 결제 거래(PAYMENT)인 경우만 알림 생성
        if (!"PAYMENT".equals(response.getTransactionType())) {
            log.debug("결제 성공 알림 스킵 - 결제 거래가 아님. transactionType={}, transactionId={}",
                    response.getTransactionType(), response.getTransactionId());
            return;
        }

        Long transactionId = response.getTransactionId();

        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put("merchant", response.getMerchantName());
        placeholders.put("amount", response.getAmount());

        NotificationCreateRequestDTO request = NotificationCreateRequestDTO.builder()
                .category(NotificationCategory.PAYMENT_NOTIFY)
                .notificationType(NOTIFICATION_TYPE_PAYMENT_SUCCESS)
                .important(true)
                .placeholders(placeholders)
                .referenceType(REFERENCE_TYPE)
                .referenceId(transactionId)
                .build();

        notificationCreateService.createNotification(userId, request);

        log.info("결제 성공 알림 생성 - userId={}, transactionId={}, merchant={}, amount={}",
                userId, transactionId, response.getMerchantName(), response.getAmount());
    }

    @Override
    public void notifyRefundSuccess(Long userId, PaymentResponse response) {
        if (response == null || response.getTransactionId() == null) {
            log.warn("환불 완료 알림 - 거래 정보 없음. userId={}", userId);
            return;
        }

        // 결제 거래(PAYMENT)인 경우만 알림 생성
        if (!"PAYMENT".equals(response.getTransactionType())) {
            log.debug("환불 완료 알림 스킵 - 결제 거래가 아님. transactionType={}, transactionId={}",
                    response.getTransactionType(), response.getTransactionId());
            return;
        }

        Long transactionId = response.getTransactionId();

        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put("merchant", response.getMerchantName());
        placeholders.put("amount", response.getAmount());

        NotificationCreateRequestDTO request = NotificationCreateRequestDTO.builder()
                .category(NotificationCategory.PAYMENT_NOTIFY)
                .notificationType(NOTIFICATION_TYPE_REFUND_SUCCESS)
                .important(true)
                .placeholders(placeholders)
                .referenceType(REFERENCE_TYPE)
                .referenceId(transactionId)
                .build();

        notificationCreateService.createNotification(userId, request);

        log.info("환불 완료 알림 생성 - userId={}, transactionId={}, merchant={}, amount={}",
                userId, transactionId, response.getMerchantName(), response.getAmount());
    }
}
