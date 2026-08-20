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
}
