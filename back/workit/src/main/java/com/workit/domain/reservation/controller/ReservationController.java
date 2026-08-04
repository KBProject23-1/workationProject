package com.workit.domain.reservation.controller;

import com.workit.domain.reservation.dto.response.ReservationCancellationDetailResponseDTO;
import com.workit.domain.reservation.dto.response.ReservationDetailResponseDTO;
import com.workit.domain.reservation.dto.response.ReservationListItemResponseDTO;
import com.workit.domain.reservation.service.ReservationService;
import com.workit.domain.reservation.vo.ReservationCategory;
import com.workit.domain.reservation.vo.ReservationStatus;
import com.workit.global.dto.CommonResponse;
import com.workit.global.dto.PageResponseDTO;
import com.workit.global.response.GlobalResponseFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 예약 목록과 예약 상세 조회 요청을 처리하는 컨트롤러
 */
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
    public ResponseEntity<CommonResponse<PageResponseDTO<ReservationListItemResponseDTO>>> reservationList(
            @RequestParam("status") List<ReservationStatus> statuses,
            @RequestParam(value = "category", required = false) ReservationCategory category,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        // 인증 기능 연결 전까지 1번 사용자를 사용하며, 이후 JWT 인증 객체에서 추출하도록 교체한다.
        Long userId = 1L;

        return GlobalResponseFactory.success(
                reservationService.findReservationList(userId, statuses, category, page, size)
        );
    }


//    로그인 사용자의 예약 확정·이용 완료 상세를 조회
    @GetMapping("/{reservationId}")
    public ResponseEntity<CommonResponse<ReservationDetailResponseDTO>> reservationDetails(
            @PathVariable("reservationId") Long reservationId) {

        // 인증 기능 연결 전까지 1번 사용자를 사용하며, 이후 JWT 인증 객체에서 추출하도록 교체한다.
        Long userId = 1L;

        return GlobalResponseFactory.success(
                reservationService.findReservationDetails(userId, reservationId)
        );
    }

    // 로그인 사용자의 예약 취소 상세를 조회
    @GetMapping("/{reservationId}/cancellation")
    public ResponseEntity<CommonResponse<ReservationCancellationDetailResponseDTO>> reservationCancellationDetails(
            @PathVariable("reservationId") Long reservationId) {

        // 인증 기능 연결 전까지 1번 사용자를 사용하며, 이후 JWT 인증 객체에서 추출하도록 교체한다.
        Long userId = 1L;

        return GlobalResponseFactory.success(
                reservationService.findReservationCancellationDetails(userId, reservationId)
        );
    }
}
