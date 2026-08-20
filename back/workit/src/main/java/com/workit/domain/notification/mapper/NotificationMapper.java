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
}
