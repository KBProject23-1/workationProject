package com.workit.domain.reservation.controller;

import com.workit.domain.reservation.dto.response.ReservationListItemResponseDTO;
import com.workit.domain.reservation.service.ReservationService;
import com.workit.domain.reservation.vo.ReservationCategory;
import com.workit.domain.reservation.vo.ReservationStatus;
import com.workit.global.dto.PageResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    /**
     * 로그인 사용자의 예약 목록을 상태·카테고리별로 조회
     * status는 같은 이름의 Query Parameter를 반복하여 여러 건 전달 가능
     */
    @GetMapping
    public ResponseEntity<PageResponseDTO<ReservationListItemResponseDTO>> reservationList(
            @RequestParam("status") List<ReservationStatus> statuses,
            @RequestParam(value = "category", required = false) ReservationCategory category,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        // 인증 기능 연결 전까지 1번 사용자를 사용하며, 이후 JWT 인증 객체에서 추출하도록 교체한다.
        Long userId = 1L;

        return ResponseEntity.ok(
                reservationService.findReservationList(userId, statuses, category, page, size)
        );
    }
}
