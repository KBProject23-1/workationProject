package com.workit.domain.reservation.controller;

import com.workit.domain.reservation.dto.request.ReservationCreateRequestDTO;
import com.workit.domain.reservation.dto.response.ReservationCancellationDetailResponseDTO;
import com.workit.domain.reservation.dto.response.ReservationCancelResponseDTO;
import com.workit.domain.reservation.dto.response.ReservationCreateResponseDTO;
import com.workit.domain.reservation.dto.response.ReservationDetailResponseDTO;
import com.workit.domain.reservation.dto.response.ReservationListItemResponseDTO;
import com.workit.domain.reservation.service.ReservationService;
import com.workit.domain.reservation.vo.ReservationCategory;
import com.workit.domain.reservation.vo.ReservationStatus;
import com.workit.global.dto.CommonResponse;
import com.workit.global.dto.PageResponseDTO;
import com.workit.global.response.GlobalResponseFactory;
import com.workit.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
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

    // 상품 가격과 재고를 다시 검증하고 지갑 결제가 완료된 예약을 생성한다.
    @PostMapping
    public ResponseEntity<CommonResponse<ReservationCreateResponseDTO>> reservationAdd(
            @CurrentUser Long userId,
            @RequestBody ReservationCreateRequestDTO request) {

        ReservationCreateResponseDTO response = reservationService.addReservation(userId, request);
        return GlobalResponseFactory.created(response);
    }

    // 로그인 사용자의 예약 취소와 결제 환불 처리
    @PostMapping("/{reservationId}/cancel")
    public ResponseEntity<CommonResponse<ReservationCancelResponseDTO>> reservationCancelSave(
            @CurrentUser Long userId,
            @PathVariable("reservationId") Long reservationId) {

        return GlobalResponseFactory.success(
                reservationService.saveReservationCancellation(userId, reservationId)
        );
    }

    /**
     * 로그인 사용자의 예약 목록을 상태·카테고리별로 조회
     * status는 같은 이름의 Query Parameter를 반복하여 여러 건 전달 가능
     */
    @GetMapping
    public ResponseEntity<CommonResponse<PageResponseDTO<ReservationListItemResponseDTO>>> reservationList(
            @CurrentUser Long userId,
            @RequestParam(value = "workationId", required = false) Long workationId,
            @RequestParam("status") List<ReservationStatus> statuses,
            @RequestParam(value = "category", required = false) ReservationCategory category,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        return GlobalResponseFactory.success(
                reservationService.findReservationList(
                        userId,
                        workationId,    // 워케이션 필터, null이면 전체
                        statuses,
                        category,
                        page,
                        size
                )
        );
    }


//    로그인 사용자의 예약 확정·이용 완료 상세를 조회
    @GetMapping("/{reservationId}")
    public ResponseEntity<CommonResponse<ReservationDetailResponseDTO>> reservationDetails(
            @CurrentUser Long userId,
            @PathVariable("reservationId") Long reservationId) {

        return GlobalResponseFactory.success(
                reservationService.findReservationDetails(userId, reservationId)
        );
    }

    // 로그인 사용자의 예약 취소 상세를 조회
    @GetMapping("/{reservationId}/cancellation")
    public ResponseEntity<CommonResponse<ReservationCancellationDetailResponseDTO>> reservationCancellationDetails(
            @CurrentUser Long userId,
            @PathVariable("reservationId") Long reservationId) {

        return GlobalResponseFactory.success(
                reservationService.findReservationCancellationDetails(userId, reservationId)
        );
    }
}
