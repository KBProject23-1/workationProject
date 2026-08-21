package com.workit.domain.notification.service;

import com.workit.domain.notification.dto.request.NotificationCreateRequestDTO;
import com.workit.domain.notification.enums.NotificationCategory;
import com.workit.domain.notification.exception.NotificationErrorCode;
import com.workit.domain.notification.mapper.NotificationMapper;
import com.workit.domain.notification.vo.NotificationSettingsVO;
import com.workit.domain.notification.vo.NotificationTemplateVO;
import com.workit.domain.notification.vo.NotificationVO;
import com.workit.exception.BusinessException;
import com.workit.exception.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

// 공통 알림 생성 서비스 구현체
// - 각 도메인에서 공통으로 호출할 수 있는 알림 생성 기능을 구현한다
// - notificationType별 if/switch 분기 없이 일반적인 흐름으로 동작한다
// - 새 템플릿이 추가되어도 서비스 코드 수정 없이 동작한다
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationCreateServiceImpl implements NotificationCreateService {

    private final NotificationMapper notificationMapper;

    @Override
    @Transactional
    public void createNotification(Long userId, NotificationCreateRequestDTO request) {

        // 0. 요청 검증 — 필수 필드가 null이면 COMMON_INVALID_REQUEST(400)
        if (request.getCategory() == null) {
            throw new BusinessException(CommonErrorCode.COMMON_INVALID_REQUEST,
                    "카테고리는 필수입니다.");
        }
        if (request.getNotificationType() == null || request.getNotificationType().isEmpty()) {
            throw new BusinessException(CommonErrorCode.COMMON_INVALID_REQUEST,
                    "알림 타입은 필수입니다.");
        }
        if (request.getImportant() == null) {
            throw new BusinessException(CommonErrorCode.COMMON_INVALID_REQUEST,
                    "중요 알림 여부는 필수입니다.");
        }

        // 1. 사용자 알림 수신 설정 확인
        //    - 해당 카테고리의 알림 설정이 OFF면 알림을 생성하지 않고 종료
        if (!isNotificationEnabled(userId, request.getCategory())) {
            log.info("알림 수신 설정 OFF - userId={}, category={}, notificationType={}",
                    userId, request.getCategory(), request.getNotificationType());
            return;
        }

        // 2. notification_templates 조회
        //    - category + notification_type + is_active = 1 조건으로 활성 템플릿 조회
        NotificationTemplateVO template = notificationMapper.selectActiveTemplate(
                request.getCategory().name(), request.getNotificationType());

        if (template == null) {
            log.warn("활성 알림 템플릿 미존재 - userId={}, category={}, notificationType={}",
                    userId, request.getCategory(), request.getNotificationType());
            throw new BusinessException(NotificationErrorCode.NOTIFICATION_TEMPLATE_NOT_FOUND);
        }

        // 3. placeholder 치환
        //    - DB 템플릿의 {placeholder}를 실제 전달받은 값으로 치환
        String title = replacePlaceholders(template.getTitleTemplate(), request.getPlaceholders());
        String content = replacePlaceholders(template.getContentTemplate(), request.getPlaceholders());

        // 4. notification_histories 저장
        NotificationVO historyVO = new NotificationVO();
        historyVO.setCategory(request.getCategory().name());
        historyVO.setImportant(request.getImportant());
        historyVO.setTitle(title);
        historyVO.setContent(content);
        historyVO.setReferenceType(request.getReferenceType());
        historyVO.setReferenceId(request.getReferenceId());

        notificationMapper.insertNotificationHistory(userId, historyVO);

        log.info("알림 생성 완료 - userId={}, category={}, notificationType={}, title={}",
                userId, request.getCategory(), request.getNotificationType(), title);
    }

    /**
     * 사용자 알림 수신 설정 확인
     * - NotificationCategory Enum을 기반으로 해당 설정 필드의 값을 확인한다
     * - 설정이 OFF(false)이면 false를 반환한다
     * - 설정이 없으면 기본값으로 true를 반환한다 (방어적 처리)
     *
     * @param userId   사용자 ID
     * @param category 알림 카테고리 (NotificationCategory Enum)
     * @return 알림 수신 설정 활성화 여부
     */
    private boolean isNotificationEnabled(Long userId, NotificationCategory category) {
        NotificationSettingsVO settings = notificationMapper.selectNotificationSettings(userId);

        // 설정이 없는 경우 기본값으로 알림 허용 (방어적 처리)
        if (settings == null) {
            log.warn("알림 수신 설정 미존재 - 기본값(true) 적용 userId={}", userId);
            return true;
        }

        switch (category) {
            case BUDGET_NOTIFY:
                return Boolean.TRUE.equals(settings.getBudgetNotify());
            case TRANSFER_NOTIFY:
                return Boolean.TRUE.equals(settings.getTransferNotify());
            case PAYMENT_NOTIFY:
                return Boolean.TRUE.equals(settings.getPaymentNotify());
            case WORKATION_NOTIFY:
                return Boolean.TRUE.equals(settings.getWorkationNotify());
            case SETTLEMENT_NOTIFY:
                return Boolean.TRUE.equals(settings.getSettlementNotify());
            case SCHEDULE_NOTIFY:
                return Boolean.TRUE.equals(settings.getScheduleNotify());
            default:
                return true;
        }
    }

    /**
     * 템플릿의 placeholder를 실제 값으로 치환한다.
     * - {key} 형태의 placeholder를 placeholders Map의 값으로 치환한다
     * - placeholders가 null이거나 일치하는 키가 없으면 원본 템플릿을 그대로 반환한다
     * - 치환은 순서대로 수행되며, 숫자 타입도 정상적으로 치환된다
     *
     * @param template     placeholder를 포함하는 템플릿 문자열
     * @param placeholders 치환할 값들의 Map (키: placeholder 이름, 값: 치환할 값)
     * @return 치환된 문자열
     */
    private String replacePlaceholders(String template, Map<String, Object> placeholders) {
        if (placeholders == null || placeholders.isEmpty()) {
            return template;
        }

        String result = template;
        for (Map.Entry<String, Object> entry : placeholders.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            result = result.replace(placeholder, value);
        }

        return result;
    }
}
