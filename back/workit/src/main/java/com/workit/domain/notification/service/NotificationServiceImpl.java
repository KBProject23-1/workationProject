package com.workit.domain.notification.service;

import com.workit.domain.notification.dto.response.NotificationDTO;
import com.workit.domain.notification.dto.response.NotificationListResponseDTO;
import com.workit.domain.notification.dto.response.NotificationSettingsResponseDTO;
import com.workit.domain.notification.exception.NotificationErrorCode;
import com.workit.domain.notification.mapper.NotificationMapper;
import com.workit.domain.notification.vo.NotificationSettingsVO;
import com.workit.domain.notification.vo.NotificationVO;
import com.workit.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Notification 도메인 Service 구현체
// - 커서 기반 페이지네이션으로 알림 목록을 조회한다
// - size 기본값/최대값 검증은 Service Layer 에서 수행 (Mapper 에서 검증 금지)
// - hasNext/nextCursor 계산은 Service Layer 에서 수행 (Mapper 에서 비즈니스 로직 금지)
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;

    /** size 기본값 — 문서 스펙 기본값 20 */
    private static final int DEFAULT_SIZE = 20;

    /** size 최대값 — 문서 스펙 최대값 100 */
    private static final int MAX_SIZE = 100;

    @Override
    @Transactional(readOnly = true)
    public NotificationListResponseDTO getNotificationList(Long userId, Long cursor, Integer size) {

        // 1. size 검증 + 기본값 적용
        //    - null/빈 값이면 기본값 20 (문서 스펙)
        //    - 100 초과 시 NOTIFICATION_SIZE_EXCEEDED(400) (문서 스펙)
        int querySize = resolveSize(size);

        // 2. DB 조회 — size + 1 개 조회 (hasNext 판단용)
        //    - NotificationMapper.selectNotificationList: user_id 기반 조회, ORDER BY id DESC
        //    - cursor 가 있으면 id < cursor 조건 적용
        List<NotificationVO> notifications = notificationMapper.selectNotificationList(
                userId, cursor, querySize + 1);

        // 3. hasNext 판단 — 21번째 데이터가 존재하면 hasNext=true
        boolean hasNext = notifications.size() > querySize;

        // 4. hasNext가 true이면 21번째 데이터 제거 (반환은 최대 querySize 개)
        if (hasNext) {
            notifications = new ArrayList<>(notifications.subList(0, querySize));
        }

        // 5. nextCursor 계산 — hasNext가 true이면 마지막 알림 ID, false이면 null
        Long nextCursor = null;
        if (hasNext && !notifications.isEmpty()) {
            NotificationVO lastNotification = notifications.get(notifications.size() - 1);
            nextCursor = lastNotification.getNotificationId();
        }

        // 6. VO → DTO 변환
        List<NotificationDTO> notificationDTOs;
        if (notifications.isEmpty()) {
            notificationDTOs = Collections.emptyList();
        } else {
            notificationDTOs = notifications.stream()
                    .map(NotificationDTO::fromVO)
                    .collect(java.util.stream.Collectors.toList());
        }

        // 7. Audit 로그 — userId 만 기록 (알림 내용 로그 출력 금지)
        log.info("알림 목록 조회 성공 - userId={}, count={}, hasNext={}",
                userId, notificationDTOs.size(), hasNext);

        // 8. 응답 생성 — 알림이 없는 경우도 정상적인 200 OK 반환
        return NotificationListResponseDTO.of(notificationDTOs, nextCursor, hasNext);
    }

    @Override
    @Transactional(readOnly = true)
    public int getUnreadCount(Long userId) {

        // 1. DB 조회 — COUNT 쿼리로 읽지 않은 알림 개수 조회
        int unreadCount = notificationMapper.countUnreadNotifications(userId);

        // 2. Audit 로그 — userId 만 기록
        log.info("읽지 않은 알림 개수 조회 성공 - userId={}, unreadCount={}",
                userId, unreadCount);

        // 3. 응답 반환 — 읽지 않은 알림이 없는 경우도 정상적인 0 반환
        return unreadCount;
    }

    @Override
    @Transactional
    public void markAsRead(Long userId, Long notificationId) {

        // 1. 소유권 검증 — 알림 존재 여부 확인 (PK + user_id)
        //    - MySQL은 UPDATE 시 값이 동일하면 affected_rows=0을 반환하므로,
        //      UPDATE 단독으로 존재 여부를 판단하면 이미 읽은 알림에서 404 발생
        //    - 따라서 별도의 existsNotification 쿼리로 존재 여부를 먼저 확인
        boolean exists = notificationMapper.existsNotification(userId, notificationId);
        if (!exists) {
            throw new BusinessException(NotificationErrorCode.NOTIFICATION_NOT_FOUND);
        }

        // 2. 읽음 처리 — idempotent (이미 읽었어도 정상 처리)
        //    - MySQL affected_rows는 0일 수 있지만, 이미 존재 여부를 확인했으므로 무시
        notificationMapper.markAsRead(userId, notificationId);

        // 3. Audit 로그 — userId 만 기록 (알림 내용 로그 출력 금지)
        log.info("알림 읽음 처리 성공 - userId={}, notificationId={}",
                userId, notificationId);
    }

    @Override
    @Transactional
    public int markAllAsRead(Long userId) {

        // 1. 전체 읽음 처리 — 현재 사용자의 읽지 않은 모든 알림을 읽음 상태로 변경
        //    - SQL에 user_id 조건이 포함되어 있으므로 다른 사용자의 알림은 변경되지 않는다
        //    - 읽지 않은 알림이 없는 경우 affected_rows=0을 반환하지만, 이는 정상적인 상태이다
        int updatedRows = notificationMapper.markAllAsRead(userId);

        // 2. Audit 로그 — userId 만 기록 (변경된 행 수는 로그에 기록)
        log.info("전체 알림 읽음 처리 성공 - userId={}, updatedRows={}",
                userId, updatedRows);

        // 3. 정상 성공 반환 — affected_rows가 0이어도 예외를 발생시키지 않는다
        return updatedRows;
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationSettingsResponseDTO getNotificationSettings(Long userId) {

        // 1. DB 조회 — user_notification_settings 테이블에서 사용자 알림 설정 조회
        NotificationSettingsVO settingsVO = notificationMapper.selectNotificationSettings(userId);

        // 2. 조회 결과가 없는 경우 null 반환
        //    - 회원가입 시 알림 설정 레코드가 반드시 생성되므로 정상적인 사용자는 항상 존재
        //    - 하지만 방어적으로 null 체크 수행
        if (settingsVO == null) {
            log.warn("알림 수신 설정 조회 실패 - 설정 데이터 미존재 userId={}", userId);
            return null;
        }

        // 3. VO → DTO 변환
        NotificationSettingsResponseDTO responseDTO = NotificationSettingsResponseDTO.fromVO(settingsVO);

        // 4. Audit 로그 — userId 만 기록
        log.info("알림 수신 설정 조회 성공 - userId={}", userId);

        return responseDTO;
    }

    /**
     * size 파라미터 검증 + 기본값 적용
     * - null/빈 값 → 기본값 20
     * - 0 이하 → 기본값 20
     * - 100 초과 → NOTIFICATION_SIZE_EXCEEDED(400)
     *
     * @param size 요청 size 파라미터
     * @return 검증된 size 값
     */
    private int resolveSize(Integer size) {
        if (size == null || size <= 0) {
            return DEFAULT_SIZE;
        }
        if (size > MAX_SIZE) {
            throw new BusinessException(NotificationErrorCode.NOTIFICATION_SIZE_EXCEEDED);
        }
        return size;
    }
}
