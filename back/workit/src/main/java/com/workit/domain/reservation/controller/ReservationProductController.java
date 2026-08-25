package com.workit.domain.reservation.controller;

import com.workit.domain.reservation.dto.request.ReservationProductAvailabilityRequestDTO;
import com.workit.domain.reservation.dto.response.ReservationProductAvailabilityResponseDTO;
import com.workit.domain.reservation.service.ReservationService;
import com.workit.global.dto.CommonResponse;
import com.workit.global.response.GlobalResponseFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

// 예약 상품의 날짜별 재고 조회 컨트롤러
@RestController
@RequestMapping("/api/v1/reservation-products")
@RequiredArgsConstructor
public class ReservationProductController {

    private final ReservationService reservationService;

    // 예약 상품의 날짜별 남은 재고와 예약 가능 여부 목록
    @GetMapping("/{productId}/availability")
    public ResponseEntity<CommonResponse<List<ReservationProductAvailabilityResponseDTO>>>
    reservationProductAvailabilityList(
            @PathVariable("productId") Long productId,
            @RequestParam("startDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        ReservationProductAvailabilityRequestDTO request =
                new ReservationProductAvailabilityRequestDTO();
        request.setStartDate(startDate);
        request.setEndDate(endDate);

        return GlobalResponseFactory.success(
                reservationService.findReservationProductAvailabilities(productId, request)
        );
    }
}
