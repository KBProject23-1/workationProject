package com.workit.domain.workation.mapper;

import org.apache.ibatis.annotations.Param;

// 워케이션 알림 중복 방지용 Mapper
// - notification_histories 에서 userId + referenceType + referenceId + notificationType 으로 기존 알림 존재 여부를 확인한다
// - WorkationAlertService에서 사용한다
public interface WorkationAlertMapper {

    /**
     * 특정 워케이션에 대한 알림이 이미 존재하는지 확인한다.
     * - userId + referenceType + referenceId + notificationType 조건으로 EXISTS 쿼리
     * - 시작 알림과 종료 알림은 notificationType이 다르므로 별도로 중복 여부를 판단한다
     *
     * @param userId           사용자 ID
     * @param referenceType    참조 타입 (예: "WORKATION")
     * @param referenceId      참조 ID (워케이션 ID)
     * @param notificationType 알림 타입 (예: "WORKATION_START_D_MINUS_1", "WORKATION_END_D_MINUS_1")
     * @return 알림 존재 여부 (true: 이미 알림 있음, false: 알림 없음)
     */
    boolean existsNotificationByReference(@Param("userId") Long userId,
                                           @Param("referenceType") String referenceType,
                                           @Param("referenceId") Long referenceId,
                                           @Param("notificationType") String notificationType);
}
