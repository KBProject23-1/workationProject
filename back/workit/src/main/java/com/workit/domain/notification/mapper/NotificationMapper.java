package com.workit.domain.notification.mapper;

import com.workit.domain.notification.vo.NotificationVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

// Notification 도메인 Mapper (notification_histories 테이블 담당)
// - SQL 은 XML 매퍼(com/workit/domain/notification/mapper/NotificationMapper.xml)에 작성한다 (MyBatis 규칙)
// - Mapper 에 비즈니스 로직 금지 — 조회만 담당
public interface NotificationMapper {

    /**
     * 알림 목록 조회 - 커서 기반 페이지네이션
     * - notification_histories 테이블에서 user_id 로 사용자 알림 조회
     * - ORDER BY id DESC (최신순 정렬)
     * - cursor 가 있으면 id < cursor 조건 적용
     * - size + 1 개 조회하여 hasNext 판단 (21번째 데이터 존재 여부)
     *
     * @param userId 사용자 ID
     * @param cursor 이전 조회 결과의 마지막 알림 ID (null이면 최신부터)
     * @param size   조회할 알림 수 (기본값 20)
     * @return 알림 목록
     */
    List<NotificationVO> selectNotificationList(@Param("userId") Long userId,
                                                 @Param("cursor") Long cursor,
                                                 @Param("size") int size);

    /**
     * 읽지 않은 알림 개수 조회 - 단순 COUNT 쿼리
     * - notification_histories 테이블에서 user_id + read = 0 조건으로 COUNT
     * - 서비스 계층에서 사용자별 unread count를 반환
     *
     * @param userId 사용자 ID
     * @return 읽지 않은 알림 개수
     */
    int countUnreadNotifications(@Param("userId") Long userId);

    /**
     * 알림 존재 여부 확인 - notificationId와 userId가 모두 일치하는 알림이 존재하는지 확인한다.
     * - 소유권 검증 포함 (user_id 조건)
     * - 알림이 존재하면 true, 존재하지 않으면 false를 반환한다.
     *
     * @param userId         현재 로그인한 사용자 ID
     * @param notificationId 확인할 알림 ID
     * @return 존재 여부 (true 또는 false)
     */
    boolean existsNotification(@Param("userId") Long userId, @Param("notificationId") Long notificationId);

    /**
     * 알림 단건 읽음 처리 - notificationId와 userId가 모두 일치하는 알림의 read 값을 1로 변경한다.
     * - 소유권 검증: user_id 조건을 반드시 포함하여 다른 사용자의 알림을 변경하지 않도록 한다.
     * - 이미 읽은 알림(read=1)을 다시 요청해도 정상적으로 영향 행 수 0을 반환하지만,
     *   existsNotification으로 사전 검증하므로 404 없이 정상 처리된다.
     * - notificationId가 존재하지 않거나 다른 사용자의 알림이면 영향 행 수 0을 반환한다.
     *
     * @param userId         현재 로그인한 사용자 ID
     * @param notificationId 읽음 처리할 알림 ID
     * @return 영향을 받은 행 수 (0 또는 1)
     */
    int markAsRead(@Param("userId") Long userId, @Param("notificationId") Long notificationId);
}
