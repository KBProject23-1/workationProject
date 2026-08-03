package com.workit.domain.reservation.service;

import com.workit.domain.reservation.dto.response.ReservationDetailResponseDTO;
import com.workit.domain.reservation.dto.response.ReservationListItemResponseDTO;
import com.workit.domain.reservation.vo.ReservationCategory;
import com.workit.domain.reservation.vo.ReservationStatus;
import com.workit.global.dto.PageResponseDTO;

import java.util.List;

/**
 * 예약 조회 기능에서 제공하는 서비스 규격
 */
public interface ReservationService {

    /**
     * 사용자 예약 목록을 상태와 카테고리 조건으로 페이징 조회한다.
     */
    PageResponseDTO<ReservationListItemResponseDTO> findReservationList(
            Long userId,
            List<ReservationStatus> statuses,
            ReservationCategory category,
            int page,
            int size
    );

    /**
     * 로그인 사용자가 소유한 예약 확정·이용 완료 상세를 조회한다.
     */
    ReservationDetailResponseDTO findReservationDetails(Long userId, Long reservationId);
}
