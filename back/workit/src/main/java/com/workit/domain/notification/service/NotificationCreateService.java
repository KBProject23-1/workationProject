package com.workit.domain.notification.service;

import com.workit.domain.notification.dto.request.NotificationCreateRequestDTO;

// 공통 알림 생성 서비스 인터페이스
// - 각 도메인에서 공통으로 호출할 수 있는 알림 생성 기능을 제공한다
// - notificationType별 if/switch 분기 없이 일반적인 흐름으로 동작한다
// - 흐름: 사용자 수신 설정 확인 → 활성 템플릿 조회 → placeholder 치환 → notification_histories 저장
public interface NotificationCreateService {

    /**
     * 공통 알림 생성 - 카테고리, 알림 타입, placeholder 데이터를 받아 알림을 생성한다.
     *
     * 흐름:
     * 1. 사용자 알림 수신 설정 확인
     *    - 해당 카테고리의 알림 설정이 OFF면 notification_histories에 저장하지 않고 종료
     * 2. notification_templates 조회
     *    - category + notification_type + is_active = 1 조건으로 활성 템플릿 조회
     *    - 템플릿이 존재하지 않으면 NOTIFICATION_TEMPLATE_NOT_FOUND(404) 예외 발생
     * 3. title/content placeholder 치환
     *    - DB 템플릿의 {placeholder}를 실제 전달받은 값으로 치환
     * 4. notification_histories 저장
     *    - 치환된 title/content와 함께 알림 이력을 저장한다
     *
     * @param userId  알림을 받을 사용자 ID
     * @param request 알림 생성 요청 (category, notificationType, important, placeholders, referenceType, referenceId)
     */
    void createNotification(Long userId, NotificationCreateRequestDTO request);
}
