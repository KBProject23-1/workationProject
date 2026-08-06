package com.workit.domain.reservation.service;

import com.workit.domain.reservation.dto.request.ReservationCreateRequestDTO;
import com.workit.domain.reservation.dto.response.ReservationCancellationDetailResponseDTO;
import com.workit.domain.reservation.dto.response.ReservationCancelResponseDTO;
import com.workit.domain.reservation.dto.response.ReservationCreateResponseDTO;
import com.workit.domain.reservation.dto.response.ReservationDetailResponseDTO;
import com.workit.domain.reservation.dto.response.ReservationListItemResponseDTO;
import com.workit.domain.reservation.vo.ReservationCategory;
import com.workit.domain.reservation.vo.ReservationStatus;
import com.workit.global.dto.PageResponseDTO;

import java.time.LocalDate;
import java.util.List;

/**
 * 예약 조회 기능에서 제공하는 서비스 규격
 */
public interface ReservationService {

    // 이용 기간이 종료된 확정 예약의 완료 상태 변경
    int modifyCompletedReservationStatuses(LocalDate today);

    // 상품 가격과 재고를 다시 검증하고 지갑 결제가 완료된 예약을 생성
    ReservationCreateResponseDTO addReservation(
            Long userId,
            ReservationCreateRequestDTO request
    );

    // 예약 취소와 결제 환불 및 차감 재고 복구
    ReservationCancelResponseDTO saveReservationCancellation(Long userId, Long reservationId);

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

    /**
     * 로그인 사용자가 소유한 예약 취소 상세를 조회한다.
     */
    ReservationCancellationDetailResponseDTO findReservationCancellationDetails(
            Long userId,
            Long reservationId
    );
}
