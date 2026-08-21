package com.workit.domain.payment.mapper;

import org.apache.ibatis.annotations.Param;

// 결제 알림 중복 방지용 Mapper
// - notification_histories 에서 userId + referenceType + referenceId + notificationType 으로 기존 알림 존재 여부를 확인한다
// - PaymentAlertService에서 사용한다
public interface PaymentAlertMapper {

    /**
     * 특정 결제에 대한 알림이 이미 존재하는지 확인한다.
     * - userId + referenceType + referenceId + notificationType 조건으로 EXISTS 쿼리
     * - PAYMENT_SUCCESS와 REFUND_SUCCESS는 notificationType이 다르므로 별도로 중복 여부를 판단한다
     *
     * @param userId           사용자 ID
     * @param referenceType    참조 타입 (예: "TRANSACTION")
     * @param referenceId      참조 ID (거래 ID)
     * @param notificationType 알림 타입 (예: "PAYMENT_SUCCESS", "REFUND_SUCCESS")
     * @return 알림 존재 여부 (true: 이미 알림 있음, false: 알림 없음)
     */
    boolean existsNotificationByReference(@Param("userId") Long userId,
                                           @Param("referenceType") String referenceType,
                                           @Param("referenceId") Long referenceId,
                                           @Param("notificationType") String notificationType);
}
