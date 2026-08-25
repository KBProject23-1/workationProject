package com.workit.domain.notification.dto.request;

import com.workit.domain.notification.enums.NotificationCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

// 공통 알림 생성 요청 DTO
// - 각 도메인에서 공통 알림 생성 서비스를 호출할 때 사용한다
// - notificationType은 String으로 처리 (Enum 생성 없음)
// - javax.validation 의존성이 없는 프로젝트 구조이므로 필수 값 검증은 Service Layer에서 수행한다
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationCreateRequestDTO {

    /** 알림 카테고리 (NotificationCategory Enum) — 필수 */
    private NotificationCategory category;

    /** 알림 세부 유형 (String — DB notification_templates.notification_type과 일치) — 필수 */
    private String notificationType;

    /** 중요 알림 여부 */
    private Boolean important;

    /** placeholder 데이터 (키: placeholder 이름, 값: 치환할 값) */
    private Map<String, Object> placeholders;

    /** 참조 타입 (예: WORKATION, BUDGET 등 — nullable) */
    private String referenceType;

    /** 참조 ID (nullable) */
    private Long referenceId;
}
