package com.workit.domain.notification.service;

import com.workit.domain.notification.dto.response.NotificationDTO;
import com.workit.domain.notification.dto.response.NotificationListResponseDTO;
import com.workit.domain.notification.exception.NotificationErrorCode;
import com.workit.domain.notification.mapper.NotificationMapper;
import com.workit.domain.notification.vo.NotificationVO;
import com.workit.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

// NotificationServiceImpl (알림 목록 조회) 테스트
// - NotificationMapper 를 Mockito @Mock 으로 주입한다 (Service 계층 검증에 집중)
@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationMapper notificationMapper;

    private NotificationServiceImpl notificationService;

    private static final Long TEST_USER_ID = 100L;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationServiceImpl(notificationMapper);
    }

    // ---------- 테스트 데이터 헬퍼 ----------

    /** 테스트용 알림 VO 생성 */
    private NotificationVO createNotificationVO(Long id, String category, boolean important,
                                                 String title, String content,
                                                 String referenceType, Long referenceId,
                                                 boolean isRead, LocalDateTime createdAt) {
        NotificationVO vo = new NotificationVO();
        vo.setNotificationId(id);
        vo.setCategory(category);
        vo.setImportant(important);
        vo.setTitle(title);
        vo.setContent(content);
        vo.setReferenceType(referenceType);
        vo.setReferenceId(referenceId);
        vo.setIsRead(isRead);
        vo.setCreatedAt(createdAt);
        return vo;
    }

    /** 테스트용 알림 목록 생성 (size 개수) */
    private List<NotificationVO> createNotificationList(int size, Long startId) {
        List<NotificationVO> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            Long id = startId - i;
            list.add(createNotificationVO(
                    id, "BUDGET_WARNING", i % 2 == 0,
                    "알림 제목 " + id, "알림 내용 " + id,
                    "BUDGET", id,
                    i % 3 == 0,
                    LocalDateTime.of(2026, 7, 24, 15, 0, 0).plusMinutes(i)
            ));
        }
        return list;
    }

    // ---------- 알림 목록 조회 성공 ----------

    @Test
    @DisplayName("알림 목록 조회 성공 - 커서 없이 최신순 조회")
    void getNotificationList_success_noCursor() {
        // Given — 3개의 알림 (size + 1 = 4개 미만이면 hasNext=false)
        List<NotificationVO> notifications = createNotificationList(3, 1005L);
        when(notificationMapper.selectNotificationList(TEST_USER_ID, null, 21))
                .thenReturn(notifications);

        // When
        NotificationListResponseDTO result = notificationService.getNotificationList(
                TEST_USER_ID, null, null);

        // Then
        assertNotNull(result);
        assertEquals(3, result.getNotifications().size());

        assertFalse(result.getHasNext());
        assertNull(result.getNextCursor());

        // Mapper 호출 확인
        verify(notificationMapper).selectNotificationList(TEST_USER_ID, null, 21);
    }

    @Test
    @DisplayName("알림 목록 조회 성공 - cursor가 있는 경우")
    void getNotificationList_success_withCursor() {
        // Given — 5개의 알림 (size + 1 = 21개 미만이면 hasNext=false)
        List<NotificationVO> notifications = createNotificationList(5, 1000L);
        when(notificationMapper.selectNotificationList(TEST_USER_ID, 1005L, 21))
                .thenReturn(notifications);

        // When
        NotificationListResponseDTO result = notificationService.getNotificationList(
                TEST_USER_ID, 1005L, 20);

        // Then
        assertNotNull(result);
        assertEquals(5, result.getNotifications().size());
        assertFalse(result.getHasNext());
        assertNull(result.getNextCursor());

        // Mapper 호출 확인 — cursor 포함
        verify(notificationMapper).selectNotificationList(TEST_USER_ID, 1005L, 21);
    }

    @Test
    @DisplayName("알림 목록 조회 성공 - hasNext가 true인 경우 nextCursor가 올바르게 반환")
    void getNotificationList_success_hasNext() {
        // Given — size + 1 = 21개의 알림 (hasNext=true)
        List<NotificationVO> notifications = createNotificationList(21, 1020L);
        when(notificationMapper.selectNotificationList(TEST_USER_ID, null, 21))
                .thenReturn(notifications);

        // When
        NotificationListResponseDTO result = notificationService.getNotificationList(
                TEST_USER_ID, null, 20);

        // Then
        assertNotNull(result);
        assertEquals(20, result.getNotifications().size()); // 21번째 제거
        assertTrue(result.getHasNext());
        assertEquals(1001L, result.getNextCursor()); // 마지막 알림 ID (21번째 제거 후 20번째)

        // Mapper 호출 확인
        verify(notificationMapper).selectNotificationList(TEST_USER_ID, null, 21);
    }

    @Test
    @DisplayName("알림 목록 조회 성공 - 마지막 페이지에서 hasNext=false / nextCursor=null")
    void getNotificationList_success_lastPage() {
        // Given — 정확히 20개의 알림 (hasNext=false)
        List<NotificationVO> notifications = createNotificationList(20, 1019L);
        when(notificationMapper.selectNotificationList(TEST_USER_ID, 1020L, 21))
                .thenReturn(notifications);

        // When
        NotificationListResponseDTO result = notificationService.getNotificationList(
                TEST_USER_ID, 1020L, 20);

        // Then
        assertNotNull(result);
        assertEquals(20, result.getNotifications().size());
        assertFalse(result.getHasNext());
        assertNull(result.getNextCursor());
    }

    @Test
    @DisplayName("알림 목록 조회 성공 - 알림이 없는 사용자 조회")
    void getNotificationList_emptyResult() {
        // Given — 빈 목록
        when(notificationMapper.selectNotificationList(TEST_USER_ID, null, 21))
                .thenReturn(Collections.emptyList());

        // When
        NotificationListResponseDTO result = notificationService.getNotificationList(
                TEST_USER_ID, null, null);

        // Then — 정상적인 200 OK 반환 (빈 목록)
        assertNotNull(result);
        assertTrue(result.getNotifications().isEmpty());
        assertFalse(result.getHasNext());
        assertNull(result.getNextCursor());
    }

    @Test
    @DisplayName("알림 목록 조회 성공 - size 기본값 적용 (null → 20)")
    void getNotificationList_defaultSize() {
        // Given — size null → 기본값 20 적용, size + 1 = 21개 조회
        List<NotificationVO> notifications = createNotificationList(21, 1020L);
        when(notificationMapper.selectNotificationList(TEST_USER_ID, null, 21))
                .thenReturn(notifications);

        // When
        NotificationListResponseDTO result = notificationService.getNotificationList(
                TEST_USER_ID, null, null);

        // Then — 21개 중 20개 반환 (hasNext=true)
        assertEquals(20, result.getNotifications().size());
        assertTrue(result.getHasNext());

        // Mapper 호출 확인 — size=21 (기본값 20 + 1)
        verify(notificationMapper).selectNotificationList(TEST_USER_ID, null, 21);
    }

    @Test
    @DisplayName("알림 목록 조회 성공 - size 최대값 적용 (100)")
    void getNotificationList_maxSize() {
        // Given — size 100, size + 1 = 101개 조회
        List<NotificationVO> notifications = createNotificationList(101, 200L);
        when(notificationMapper.selectNotificationList(TEST_USER_ID, null, 101))
                .thenReturn(notifications);

        // When
        NotificationListResponseDTO result = notificationService.getNotificationList(
                TEST_USER_ID, null, 100);

        // Then — 101개 중 100개 반환 (hasNext=true)
        assertEquals(100, result.getNotifications().size());
        assertTrue(result.getHasNext());

        // Mapper 호출 확인 — size=101 (100 + 1)
        verify(notificationMapper).selectNotificationList(TEST_USER_ID, null, 101);
    }

    @Test
    @DisplayName("알림 목록 조회 - size가 100을 초과하는 경우 → NOTIFICATION_SIZE_EXCEEDED")
    void getNotificationList_sizeExceeded() {
        // When & Then — 101 → NOTIFICATION_SIZE_EXCEEDED(400)
        BusinessException ex = assertThrows(BusinessException.class,
                () -> notificationService.getNotificationList(TEST_USER_ID, null, 101));
        assertEquals(NotificationErrorCode.NOTIFICATION_SIZE_EXCEEDED, ex.getErrorCode());

        // Mapper 미호출 (검증 실패 시 DB 조회 없음)
        verify(notificationMapper, never()).selectNotificationList(any(), any(), anyInt());
    }

    @Test
    @DisplayName("알림 목록 조회 - size가 음수인 경우 → 기본값 20 적용")
    void getNotificationList_negativeSize() {
        // Given — size -1 → 기본값 20 적용
        List<NotificationVO> notifications = createNotificationList(3, 1005L);
        when(notificationMapper.selectNotificationList(TEST_USER_ID, null, 21))
                .thenReturn(notifications);

        // When
        NotificationListResponseDTO result = notificationService.getNotificationList(
                TEST_USER_ID, null, -1);

        // Then — 기본값 20으로 조회됨
        assertEquals(3, result.getNotifications().size());
        assertFalse(result.getHasNext());

        // Mapper 호출 확인 — size=21 (기본값 20 + 1)
        verify(notificationMapper).selectNotificationList(TEST_USER_ID, null, 21);
    }

    @Test
    @DisplayName("알림 목록 조회 - size가 0인 경우 → 기본값 20 적용")
    void getNotificationList_zeroSize() {
        // Given — size 0 → 기본값 20 적용
        List<NotificationVO> notifications = createNotificationList(5, 1005L);
        when(notificationMapper.selectNotificationList(TEST_USER_ID, null, 21))
                .thenReturn(notifications);

        // When
        NotificationListResponseDTO result = notificationService.getNotificationList(
                TEST_USER_ID, null, 0);

        // Then — 기본값 20으로 조회됨
        assertEquals(5, result.getNotifications().size());
        assertFalse(result.getHasNext());

        // Mapper 호출 확인 — size=21 (기본값 20 + 1)
        verify(notificationMapper).selectNotificationList(TEST_USER_ID, null, 21);
    }

    @Test
    @DisplayName("보안 - 다른 사용자의 알림이 조회되지 않는지 확인")
    void getNotificationList_otherUserNotAccessible() {
        // Given — 다른 사용자 ID로 조회
        Long otherUserId = 200L;
        List<NotificationVO> notifications = createNotificationList(3, 1005L);
        when(notificationMapper.selectNotificationList(otherUserId, null, 21))
                .thenReturn(notifications);

        // When — 다른 사용자 ID로 조회
        NotificationListResponseDTO result = notificationService.getNotificationList(
                otherUserId, null, null);

        // Then — 해당 사용자의 알림만 조회됨
        assertEquals(3, result.getNotifications().size());

        // Mapper 호출 확인 — 다른 사용자 ID 사용
        verify(notificationMapper).selectNotificationList(otherUserId, null, 21);
    }

    @Test
    @DisplayName("알림 목록 조회 - referenceType / referenceId가 null인 알림 조회")
    void getNotificationList_nullReference() {
        // Given — referenceType/referenceId가 null인 알림
        NotificationVO vo = createNotificationVO(
                1001L, "SYSTEM_NOTICE", true,
                "시스템 공지", "시스템 공지 내용입니다.",
                null, null,
                false,
                LocalDateTime.of(2026, 7, 24, 15, 0, 0)
        );
        List<NotificationVO> notifications = Collections.singletonList(vo);
        when(notificationMapper.selectNotificationList(TEST_USER_ID, null, 21))
                .thenReturn(notifications);

        // When
        NotificationListResponseDTO result = notificationService.getNotificationList(
                TEST_USER_ID, null, null);

        // Then — null reference도 정상 반환
        assertEquals(1, result.getNotifications().size());
        assertNull(result.getNotifications().get(0).getReferenceType());
        assertNull(result.getNotifications().get(0).getReferenceId());
    }

    @Test
    @DisplayName("알림 목록 조회 - DTO 변환 검증")
    void getNotificationList_dtoConversion() {
        // Given — 특정 알림
        NotificationVO vo = createNotificationVO(
                1024L, "BUDGET_WARNING", true,
                "예산 초과 경고", "이번 달 설정하신 예산의 80%를 사용하셨습니다.",
                "BUDGET", 123L,
                false,
                LocalDateTime.of(2026, 7, 24, 15, 0, 0)
        );
        List<NotificationVO> notifications = Collections.singletonList(vo);
        when(notificationMapper.selectNotificationList(TEST_USER_ID, null, 21))
                .thenReturn(notifications);

        // When
        NotificationListResponseDTO result = notificationService.getNotificationList(
                TEST_USER_ID, null, null);

        // Then — DTO 필드 매핑 확인
        assertEquals(1, result.getNotifications().size());
        NotificationDTO dto = result.getNotifications().get(0);
        assertEquals(1024L, dto.getNotificationId());
        assertEquals("BUDGET_WARNING", dto.getCategory());
        assertTrue(dto.getImportant());
        assertEquals("예산 초과 경고", dto.getTitle());
        assertEquals("이번 달 설정하신 예산의 80%를 사용하셨습니다.", dto.getContent());
        assertEquals("BUDGET", dto.getReferenceType());
        assertEquals(123L, dto.getReferenceId());
        assertFalse(dto.getIsRead());
        assertEquals(LocalDateTime.of(2026, 7, 24, 15, 0, 0), dto.getCreatedAt());
    }

    @Test
    @DisplayName("알림 목록 조회 - 커서가 있는 경우 hasNext=true에서 nextCursor 검증")
    void getNotificationList_cursorWithHasNext() {
        // Given — 21개의 알림 (hasNext=true, nextCursor=마지막 알림 ID)
        List<NotificationVO> notifications = createNotificationList(21, 1020L);
        when(notificationMapper.selectNotificationList(TEST_USER_ID, 1000L, 21))
                .thenReturn(notifications);

        // When
        NotificationListResponseDTO result = notificationService.getNotificationList(
                TEST_USER_ID, 1000L, 20);

        // Then
        assertEquals(20, result.getNotifications().size());
        assertTrue(result.getHasNext());
        // nextCursor는 20번째 알림의 ID (21번째 제거 후)
        assertEquals(1001L, result.getNextCursor());
    }

    // ================================================================
    // 읽지 않은 알림 개수 조회 테스트
    // ================================================================

    @Test
    @DisplayName("읽지 않은 알림 개수 조회 성공 - 여러 개 있는 경우")
    void getUnreadCount_success_multipleUnread() {
        // Given — 읽지 않은 알림 5개
        when(notificationMapper.countUnreadNotifications(TEST_USER_ID)).thenReturn(5);

        // When
        int result = notificationService.getUnreadCount(TEST_USER_ID);

        // Then
        assertEquals(5, result);
        verify(notificationMapper).countUnreadNotifications(TEST_USER_ID);
    }

    @Test
    @DisplayName("읽지 않은 알림 개수 조회 성공 - 읽지 않은 알림이 없는 경우")
    void getUnreadCount_success_zeroUnread() {
        // Given — 읽지 않은 알림 0개
        when(notificationMapper.countUnreadNotifications(TEST_USER_ID)).thenReturn(0);

        // When
        int result = notificationService.getUnreadCount(TEST_USER_ID);

        // Then
        assertEquals(0, result);
        verify(notificationMapper).countUnreadNotifications(TEST_USER_ID);
    }

    @Test
    @DisplayName("읽지 않은 알림 개수 조회 - read=0인 알림만 카운트되는지 확인")
    void getUnreadCount_onlyUnreadCounted() {
        // Given — read=0인 알림 3개, read=1인 알림 7개 (Mapper에서 COUNT로 처리)
        when(notificationMapper.countUnreadNotifications(TEST_USER_ID)).thenReturn(3);

        // When
        int result = notificationService.getUnreadCount(TEST_USER_ID);

        // Then — read=0인 알림만 카운트
        assertEquals(3, result);
        verify(notificationMapper).countUnreadNotifications(TEST_USER_ID);
    }

    @Test
    @DisplayName("읽지 않은 알림 개수 조회 - 다른 사용자의 알림은 카운트되지 않는지 확인")
    void getUnreadCount_otherUserNotCounted() {
        // Given — 다른 사용자 ID
        Long otherUserId = 200L;
        when(notificationMapper.countUnreadNotifications(otherUserId)).thenReturn(0);

        // When
        int result = notificationService.getUnreadCount(otherUserId);

        // Then — 다른 사용자는 0 반환
        assertEquals(0, result);
        verify(notificationMapper).countUnreadNotifications(otherUserId);
    }

    @Test
    @DisplayName("읽지 않은 알림 개수 조회 - userId가 정상적으로 Mapper까지 전달되는지 확인")
    void getUnreadCount_userIdPassedToMapper() {
        // Given
        when(notificationMapper.countUnreadNotifications(TEST_USER_ID)).thenReturn(2);

        // When
        notificationService.getUnreadCount(TEST_USER_ID);

        // Then — TEST_USER_ID가 정확히 전달됨
        verify(notificationMapper).countUnreadNotifications(TEST_USER_ID);
    }

    @Test
    @DisplayName("읽지 않은 알림 개수 조회 - 알림이 많은 경우")
    void getUnreadCount_largeCount() {
        // Given — 읽지 않은 알림 1000개
        when(notificationMapper.countUnreadNotifications(TEST_USER_ID)).thenReturn(1000);

        // When
        int result = notificationService.getUnreadCount(TEST_USER_ID);

        // Then
        assertEquals(1000, result);
        verify(notificationMapper).countUnreadNotifications(TEST_USER_ID);
    }

    // ================================================================
    // 알림 단건 읽음 처리 테스트
    // ================================================================

    @Test
    @DisplayName("알림 단건 읽음 처리 성공 - 읽지 않은 알림(read=0)을 읽음으로 변경")
    void markAsRead_success_unreadNotification() {
        // Given — 알림이 존재하고, 읽지 않은 알림이 정상적으로 읽음 처리됨
        Long notificationId = 1024L;
        when(notificationMapper.existsNotification(TEST_USER_ID, notificationId)).thenReturn(true);
        when(notificationMapper.markAsRead(TEST_USER_ID, notificationId)).thenReturn(1);

        // When & Then — 예외 없이 정상 완료
        notificationService.markAsRead(TEST_USER_ID, notificationId);

        // Mapper 호출 확인 — existsNotification + markAsRead 순서대로 호출
        verify(notificationMapper).existsNotification(TEST_USER_ID, notificationId);
        verify(notificationMapper).markAsRead(TEST_USER_ID, notificationId);
    }

    @Test
    @DisplayName("알림 단건 읽음 처리 성공 - 이미 읽은 알림(read=1) 재요청 시 성공")
    void markAsRead_success_alreadyReadNotification() {
        // Given — 알림이 존재하나 이미 읽은 상태
        //   - existsNotification: 알림 존재 (1)
        //   - markAsRead: MySQL은 affected_rows=0 반환 (값 변경 없음)
        //   - 서비스는 existsNotification 확인 후 200 OK 반환
        Long notificationId = 1024L;
        when(notificationMapper.existsNotification(TEST_USER_ID, notificationId)).thenReturn(true);
        when(notificationMapper.markAsRead(TEST_USER_ID, notificationId)).thenReturn(0);

        // When & Then — 예외 없이 정상 완료 (200 OK)
        notificationService.markAsRead(TEST_USER_ID, notificationId);

        // Mapper 호출 확인 — existsNotification + markAsRead 순서대로 호출
        verify(notificationMapper).existsNotification(TEST_USER_ID, notificationId);
        verify(notificationMapper).markAsRead(TEST_USER_ID, notificationId);
    }

    @Test
    @DisplayName("알림 단건 읽음 처리 실패 - 존재하지 않는 notificationId")
    void markAsRead_fail_notificationNotFound() {
        // Given — 존재하지 않는 알림 ID
        Long notificationId = 9999L;
        when(notificationMapper.existsNotification(TEST_USER_ID, notificationId)).thenReturn(false);

        // When & Then — NOTIFICATION_NOT_FOUND(404) 예외 발생
        BusinessException ex = assertThrows(BusinessException.class,
                () -> notificationService.markAsRead(TEST_USER_ID, notificationId));
        assertEquals(NotificationErrorCode.NOTIFICATION_NOT_FOUND, ex.getErrorCode());

        // Mapper 호출 확인 — existsNotification만 호출됨 (markAsRead 호출 안 됨)
        verify(notificationMapper).existsNotification(TEST_USER_ID, notificationId);
        verify(notificationMapper, never()).markAsRead(any(), any());
    }

    @Test
    @DisplayName("알림 단건 읽음 처리 실패 - 다른 사용자의 notificationId")
    void markAsRead_fail_otherUserNotification() {
        // Given — 다른 사용자의 알림 ID로 요청
        Long notificationId = 1024L;
        Long otherUserId = 200L;
        when(notificationMapper.existsNotification(otherUserId, notificationId)).thenReturn(false);

        // When & Then — NOTIFICATION_NOT_FOUND(404) 예외 발생
        BusinessException ex = assertThrows(BusinessException.class,
                () -> notificationService.markAsRead(otherUserId, notificationId));
        assertEquals(NotificationErrorCode.NOTIFICATION_NOT_FOUND, ex.getErrorCode());

        // Mapper 호출 확인 — existsNotification만 호출됨 (markAsRead 호출 안 됨)
        verify(notificationMapper).existsNotification(otherUserId, notificationId);
        verify(notificationMapper, never()).markAsRead(any(), any());
    }

    @Test
    @DisplayName("알림 단건 읽음 처리 - userId가 Mapper까지 정상 전달되는지 확인")
    void markAsRead_userIdPassedToMapper() {
        // Given
        Long notificationId = 1024L;
        when(notificationMapper.existsNotification(TEST_USER_ID, notificationId)).thenReturn(true);
        when(notificationMapper.markAsRead(TEST_USER_ID, notificationId)).thenReturn(1);

        // When
        notificationService.markAsRead(TEST_USER_ID, notificationId);

        // Then — TEST_USER_ID가 existsNotification과 markAsRead에 정확히 전달됨
        verify(notificationMapper).existsNotification(TEST_USER_ID, notificationId);
        verify(notificationMapper).markAsRead(TEST_USER_ID, notificationId);
    }

    @Test
    @DisplayName("알림 단건 읽음 처리 - 다른 사용자의 알림 상태가 변경되지 않음")
    void markAsRead_otherUserNotificationNotModified() {
        // Given — 사용자 A의 알림 ID
        Long notificationId = 1024L;
        Long userA = 100L;
        Long userB = 200L;

        // 사용자 B가 사용자 A의 알림을 읽음 처리 시도 → existsNotification = 0
        when(notificationMapper.existsNotification(userB, notificationId)).thenReturn(false);

        // When & Then — NOTIFICATION_NOT_FOUND(404) 예외 발생
        BusinessException ex = assertThrows(BusinessException.class,
                () -> notificationService.markAsRead(userB, notificationId));
        assertEquals(NotificationErrorCode.NOTIFICATION_NOT_FOUND, ex.getErrorCode());

        // Mapper 호출 확인 — existsNotification만 호출됨 (markAsRead 호출 안 됨)
        verify(notificationMapper).existsNotification(userB, notificationId);
        verify(notificationMapper, never()).markAsRead(any(), any());
    }

    // ================================================================
    // 전체 알림 읽음 처리 테스트
    // ================================================================

    @Test
    @DisplayName("전체 알림 읽음 처리 성공 - 읽지 않은 알림 여러 개 존재")
    void markAllAsRead_success_multipleUnread() {
        // Given — read=0인 알림 5개가 읽음 처리됨
        when(notificationMapper.markAllAsRead(TEST_USER_ID)).thenReturn(5);

        // When
        int updatedRows = notificationService.markAllAsRead(TEST_USER_ID);

        // Then
        assertEquals(5, updatedRows);
        verify(notificationMapper).markAllAsRead(TEST_USER_ID);
    }

    @Test
    @DisplayName("전체 알림 읽음 처리 성공 - 읽지 않은 알림이 하나도 없는 경우")
    void markAllAsRead_success_noUnread() {
        // Given — 읽지 않은 알림이 없음 (affected_rows=0)
        when(notificationMapper.markAllAsRead(TEST_USER_ID)).thenReturn(0);

        // When
        int updatedRows = notificationService.markAllAsRead(TEST_USER_ID);

        // Then — 0이 반환되지만 예외 발생 없음 (정상 성공)
        assertEquals(0, updatedRows);
        verify(notificationMapper).markAllAsRead(TEST_USER_ID);
    }

    @Test
    @DisplayName("전체 알림 읽음 처리 - 현재 사용자의 알림만 처리되는지 확인")
    void markAllAsRead_onlyCurrentUserAffected() {
        // Given — 사용자 A의 알림 3개가 unread 상태
        Long userA = 100L;
        when(notificationMapper.markAllAsRead(userA)).thenReturn(3);

        // When — 사용자 A가 전체 읽음 처리
        int userAUpdated = notificationService.markAllAsRead(userA);

        // Then — 사용자 A의 알림 3개만 변경됨
        assertEquals(3, userAUpdated);

        // Mapper 호출 확인 — userA의 ID로만 호출됨
        verify(notificationMapper).markAllAsRead(userA);
    }

    @Test
    @DisplayName("전체 알림 읽음 처리 - userId가 Mapper까지 정상적으로 전달되는지 확인")
    void markAllAsRead_userIdPassedToMapper() {
        // Given
        when(notificationMapper.markAllAsRead(TEST_USER_ID)).thenReturn(2);

        // When
        notificationService.markAllAsRead(TEST_USER_ID);

        // Then — TEST_USER_ID가 정확히 전달됨
        verify(notificationMapper).markAllAsRead(TEST_USER_ID);
    }

    @Test
    @DisplayName("전체 알림 읽음 처리 성공 - 이미 모든 알림이 read=1인 경우")
    void markAllAsRead_success_allAlreadyRead() {
        // Given — 이미 모든 알림이 읽음 상태 (affected_rows=0)
        when(notificationMapper.markAllAsRead(TEST_USER_ID)).thenReturn(0);

        // When
        int updatedRows = notificationService.markAllAsRead(TEST_USER_ID);

        // Then — 예외 발생 없이 정상 성공
        assertEquals(0, updatedRows);
        verify(notificationMapper).markAllAsRead(TEST_USER_ID);
    }
}
