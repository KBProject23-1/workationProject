package com.workit.domain.notification.service;

import com.workit.domain.notification.dto.response.NotificationListResponseDTO;

// Notification 도메인 Service (알림 목록 조회 담당)
public interface NotificationService {

    /**
     * 알림 목록 조회 - 커서 기반 페이지네이션으로 사용자의 알림 목록을 최신순으로 조회한다
     *
     * 흐름:
     * 1. size 검증 — null/빈 값이면 기본값 20, 최대값 100 초과 시 NOTIFICATION_SIZE_EXCEEDED(400)
     * 2. DB 조회 — NotificationMapper.selectNotificationList (size + 1 개 조회)
     * 3. hasNext 판단 — 21번째 데이터가 존재하면 hasNext=true, nextCursor=마지막 알림 ID
     * 4. 21번째 데이터가 없으면 hasNext=false, nextCursor=null
     * 5. 알림이 없는 경우도 정상적인 200 OK 반환 (빈 목록 + hasNext=false + nextCursor=null)
     *
     * @param userId JWT 인증된 로그인 사용자 id (@CurrentUser — Controller 에서 주입)
     * @param cursor 이전 조회 결과의 마지막 알림 ID (null이면 최신부터)
     * @param size   조회할 알림 수 (null이면 기본값 20, 최대 100)
     * @return 알림 목록 + 다음 페이지 정보
     */
    NotificationListResponseDTO getNotificationList(Long userId, Long cursor, Integer size);

    /**
     * 읽지 않은 알림 개수 조회 - 현재 로그인한 사용자의 읽지 않은 알림 개수를 반환한다
     *
     * 흐름:
     * 1. DB 조회 — NotificationMapper.countUnreadNotifications (COUNT 쿼리)
     * 2. 결과 반환 — 읽지 않은 알림이 없는 경우도 정상적인 0 반환
     *
     * @param userId JWT 인증된 로그인 사용자 id (@CurrentUser — Controller 에서 주입)
     * @return 읽지 않은 알림 개수
     */
    int getUnreadCount(Long userId);

    /**
     * 알림 단건 읽음 처리 - 특정 알림을 읽음 상태로 변경한다.
     *
     * 흐름:
     * 1. DB UPDATE — NotificationMapper.markAsRead (user_id + notificationId 조건)
     * 2. 영향 행 수 검증 — 0이면 NOTIFICATION_NOT_FOUND(404), 1이면 성공
     * 3. 이미 읽은 알림 재요청 시에도 성공 (read=1 → read=1, 영향 행 수 1)
     *
     * @param userId         JWT 인증된 로그인 사용자 id (@CurrentUser — Controller 에서 주입)
     * @param notificationId 읽음 처리할 알림 ID
     */
    void markAsRead(Long userId, Long notificationId);

    /**
     * 전체 알림 읽음 처리 - 현재 사용자의 읽지 않은 모든 알림을 읽음 상태로 변경한다.
     *
     * 흐름:
     * 1. DB UPDATE — NotificationMapper.markAllAsRead (user_id 조건)
     * 2. 영향 행 수 반환 — affected_rows가 0이어도 예외를 발생시키지 않는다
     *    (읽지 않은 알림이 없었거나 이미 모두 읽음 상태였을 수 있음)
     * 3. 정상 성공 시 200 OK 반환
     *
     * @param userId JWT 인증된 로그인 사용자 id (@CurrentUser — Controller 에서 주입)
     * @return 읽음 처리된 알림 개수
     */
    int markAllAsRead(Long userId);
}
