package com.workit.domain.notification.controller;

import com.workit.domain.notification.dto.request.NotificationSettingsUpdateRequestDTO;
import com.workit.domain.notification.dto.response.NotificationListResponseDTO;
import com.workit.domain.notification.dto.response.NotificationSettingsResponseDTO;
import com.workit.domain.notification.dto.response.NotificationUnreadCountResponseDTO;
import com.workit.domain.notification.service.NotificationService;
import com.workit.global.dto.CommonResponse;
import com.workit.global.response.GlobalResponseFactory;
import com.workit.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Notification 도메인 컨트롤러 (알림 목록 조회 담당)
// - 로그인 사용자 전용 API: JWT 인증 필터 + @CurrentUser 로 userId 를 주입받는다
//   (인증 없이 접근하면 AUTH_TOKEN_NOT_FOUND 401 — CurrentUserArgumentResolver)
// - Controller 는 요청 수신과 CommonResponse 반환만 담당 (DB 조회 금지 — 전부 Service 책임)
@RestController
@RequestMapping("/api/v1/users/me/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 알림 수신 설정 조회 - 현재 로그인한 사용자의 알림 수신 설정 상태를 조회한다
     *
     * - 로그인 사용자 전용 API: JWT 인증 + @CurrentUser 로 userId 주입
     * - Request에서 userId를 직접 받지 않는다 (인증 컨텍스트에서 가져온다)
     * - GET 요청이므로 CSRF 검증 대상이 아니다
     * - 설정 데이터가 없는 경우 null 반환 (회원가입 시 항상 생성되므로 정상 사용자는 항상 존재)
     *
     * @param userId JWT 인증된 로그인 사용자 id (@CurrentUser — Controller 에서 주입)
     * @return 알림 수신 설정
     */
    @GetMapping("/settings")
    public ResponseEntity<CommonResponse<NotificationSettingsResponseDTO>> getNotificationSettings(
            @CurrentUser Long userId) {

        return GlobalResponseFactory.success(
                notificationService.getNotificationSettings(userId),
                "알림 설정 상태를 성공적으로 조회했습니다.");
    }

    /**
     * 알림 수신 설정 변경 - 현재 로그인한 사용자의 알림 수신 설정을 변경한다
     *
     * - 로그인 사용자 전용 API: JWT 인증 + @CurrentUser 로 userId 주입
     * - Request에서 userId를 직접 받지 않는다 (인증 컨텍스트에서 가져온다)
     * - PATCH 요청 — CSRF 검증 대상 (Cookie 기반 인증)
     * - 전달된 필드만 변경, 미전달 필드는 기존 값 유지
     * - 모든 필드가 null이거나 Request Body가 비어 있으면 400 Bad Request
     * - 변경 후 전체 알림 설정 상태를 반환
     *
     * @param userId  JWT 인증된 로그인 사용자 id (@CurrentUser — Controller 에서 주입)
     * @param request 변경할 알림 설정 (모든 필드 선택)
     * @return 변경 후 전체 알림 설정
     */
    @PatchMapping("/settings")
    public ResponseEntity<CommonResponse<NotificationSettingsResponseDTO>> updateNotificationSettings(
            @CurrentUser Long userId,
            @RequestBody NotificationSettingsUpdateRequestDTO request) {

        return GlobalResponseFactory.success(
                notificationService.updateNotificationSettings(userId, request),
                "알림 설정이 성공적으로 변경되었습니다.");
    }

    /**
     * 알림 목록 조회 - 커서 기반 페이지네이션으로 사용자의 알림 목록을 최신순으로 조회한다
     *
     * - 로그인 사용자 전용 API: JWT 인증 + @CurrentUser 로 userId 주입
     * - Request에서 userId를 직접 받지 않는다 (인증 컨텍스트에서 가져온다)
     * - GET 요청이므로 CSRF 검증 대상이 아니다 (knowledge.md: GET은 CSRF 검증 제외)
     *
     * @param userId JWT 인증된 로그인 사용자 id (@CurrentUser — Controller 에서 주입)
     * @param cursor 이전 조회 결과의 마지막 알림 ID (선택)
     * @param size   조회할 알림 수 (선택, 기본값 20, 최대 100)
     * @return 알림 목록 + 다음 페이지 정보
     */
    @GetMapping
    public ResponseEntity<CommonResponse<NotificationListResponseDTO>> getNotificationList(
            @CurrentUser Long userId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false) Integer size) {

        return GlobalResponseFactory.success(
                notificationService.getNotificationList(userId, cursor, size),
                "알림 목록을 성공적으로 조회했습니다.");
    }

    /**
     * 읽지 않은 알림 개수 조회 - 현재 로그인한 사용자의 읽지 않은 알림 개수를 반환한다
     *
     * - 로그인 사용자 전용 API: JWT 인증 + @CurrentUser 로 userId 주입
     * - Request에서 userId를 직접 받지 않는다 (인증 컨텍스트에서 가져온다)
     * - GET 요청이므로 CSRF 검증 대상이 아니다
     * - 읽지 않은 알림이 없는 경우도 정상적인 200 OK 반환 (unreadCount=0)
     *
     * @param userId JWT 인증된 로그인 사용자 id (@CurrentUser — Controller 에서 주입)
     * @return 읽지 않은 알림 개수
     */
    @GetMapping("/unread-count")
    public ResponseEntity<CommonResponse<NotificationUnreadCountResponseDTO>> getUnreadCount(
            @CurrentUser Long userId) {

        int unreadCount = notificationService.getUnreadCount(userId);

        return GlobalResponseFactory.success(
                NotificationUnreadCountResponseDTO.of(unreadCount),
                "읽지 않은 알림 개수를 성공적으로 조회했습니다.");
    }

    /**
     * 알림 단건 읽음 처리 - 특정 알림을 읽음 상태로 변경한다
     *
     * - 로그인 사용자 전용 API: JWT 인증 + @CurrentUser 로 userId 주입
     * - Request에서 userId를 직접 받지 않는다 (인증 컨텍스트에서 가져온다)
     * - PATCH 요청 — CSRF 검증 대상 (Cookie 기반 인증)
     * - 이미 읽은 알림을 다시 요청해도 성공 처리 (200 OK)
     * - 존재하지 않는 알림 또는 다른 사용자의 알림이면 404 반환
     *
     * @param userId         JWT 인증된 로그인 사용자 id (@CurrentUser — Controller 에서 주입)
     * @param notificationId 읽음 처리할 알림의 고유 ID
     * @return 성공 응답 (data: null)
     */
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<CommonResponse<Void>> markAsRead(
            @CurrentUser Long userId,
            @PathVariable Long notificationId) {

        notificationService.markAsRead(userId, notificationId);

        return GlobalResponseFactory.success(null, "알림이 읽음 처리되었습니다.");
    }

    /**
     * 전체 알림 읽음 처리 - 현재 로그인한 사용자의 읽지 않은 모든 알림을 읽음 상태로 변경한다
     *
     * - 로그인 사용자 전용 API: JWT 인증 + @CurrentUser 로 userId 주입
     * - Request에서 userId를 직접 받지 않는다 (인증 컨텍스트에서 가져온다)
     * - PATCH 요청 — CSRF 검증 대상 (Cookie 기반 인증)
     * - 읽지 않은 알림이 없는 경우에도 정상적인 200 OK 반환 (affected_rows=0)
     * - 다른 사용자의 알림은 변경되지 않음 (SQL에 user_id 조건 포함)
     *
     * @param userId JWT 인증된 로그인 사용자 id (@CurrentUser — Controller 에서 주입)
     * @return 성공 응답 (data: null)
     */
    @PatchMapping("/read-all")
    public ResponseEntity<CommonResponse<Void>> markAllAsRead(
            @CurrentUser Long userId) {

        notificationService.markAllAsRead(userId);

        return GlobalResponseFactory.success(null, "전체 알림이 읽음 처리되었습니다.");
    }
}
