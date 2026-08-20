package com.workit.domain.notification.controller;

import com.workit.domain.notification.dto.response.NotificationListResponseDTO;
import com.workit.domain.notification.service.NotificationService;
import com.workit.global.dto.CommonResponse;
import com.workit.global.response.GlobalResponseFactory;
import com.workit.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
}
