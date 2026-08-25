package com.workit.domain.notification.vo;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

// 알림 중복 체크용 VO
// - existsNotificationsByReferencesBatch 쿼리의 입력/출력으로 사용한다
// - referenceType + referenceId + notificationType 조합으로 알림 유니크 키를 표현한다
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"referenceType", "referenceId", "notificationType"})
@ToString
public class NotificationDuplicateKeyVO {

    /** 참조 타입 (예: "TRANSACTION", "WORKATION", "BUDGET", "SCHEDULE") */
    private String referenceType;

    /** 참조 ID */
    private Long referenceId;

    /** 알림 타입 (예: "WALLET_CHARGE_SUCCESS", "WORKATION_START_D_MINUS_1") */
    private String notificationType;
}
