package com.workit.domain.reservation.service;

import com.workit.domain.reservation.dto.response.ReservationListItemResponseDTO;
import com.workit.domain.reservation.vo.ReservationCategory;
import com.workit.domain.reservation.vo.ReservationStatus;
import com.workit.global.dto.PageResponseDTO;

import java.util.List;

public interface ReservationService {

//  사용자 예약 목록을 조건에 따라 페이징 조회.
    PageResponseDTO<ReservationListItemResponseDTO> findReservationList(
            Long userId,
            List<ReservationStatus> statuses,
            ReservationCategory category,
            int page,
            int size
    );
}
