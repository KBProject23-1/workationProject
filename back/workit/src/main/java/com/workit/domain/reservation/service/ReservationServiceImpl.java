package com.workit.domain.reservation.service;

import com.workit.domain.reservation.dto.response.ReservationCancellationDetailResponseDTO;
import com.workit.domain.reservation.dto.response.ReservationDetailResponseDTO;
import com.workit.domain.reservation.dto.response.ReservationListItemResponseDTO;
import com.workit.domain.reservation.exception.ReservationErrorCode;
import com.workit.domain.reservation.mapper.ReservationMapper;
import com.workit.domain.reservation.vo.ReservationCategory;
import com.workit.domain.reservation.vo.ReservationCancellationDetailVO;
import com.workit.domain.reservation.vo.ReservationDetailVO;
import com.workit.domain.reservation.vo.ReservationReviewAction;
import com.workit.domain.reservation.vo.ReservationStatus;
import com.workit.exception.BusinessException;
import com.workit.global.dto.PageResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;


// 예약 목록과 상세 조회에 필요한 검증 및 화면 상태 계산을 수행하는 서비스

@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {

    private static final int MAX_PAGE_SIZE = 50;

    private final ReservationMapper reservationMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<ReservationListItemResponseDTO> findReservationList(
            Long userId,
            List<ReservationStatus> statuses,
            ReservationCategory category,
            int page,
            int size) {

        validateRequest(userId, statuses, page, size);

        // 같은 상태가 여러 번 전달돼도 SQL IN 조건에는 한 번만 포함한다.
        List<ReservationStatus> distinctStatuses = new ArrayList<>(new LinkedHashSet<>(statuses));
        int offset = page * size;

        // 목록과 동일한 사용자·상태·카테고리 조건으로 전체 건수를 조회한다.
        long totalElements = reservationMapper.countReservationList(
                userId,
                distinctStatuses,
                category
        );

        if (totalElements == 0) {
            return PageResponseDTO.of(Collections.emptyList(), page, size, 0);
        }

        // CANCELED만 조회할 때는 취소 일시 기준, 그 외에는 이용 시작 일시 기준으로 정렬
        boolean canceledOnly = distinctStatuses.size() == 1
                && distinctStatuses.contains(ReservationStatus.CANCELED);

        List<ReservationListItemResponseDTO> content = reservationMapper
                .selectReservationList(
                        userId,
                        distinctStatuses,
                        category,
                        offset,
                        size,
                        canceledOnly
                )
                .stream()
                .map(ReservationListItemResponseDTO::from)
                .collect(Collectors.toList());

        return PageResponseDTO.of(content, page, size, totalElements);
    }

    @Override
    @Transactional(readOnly = true)
    public ReservationDetailResponseDTO findReservationDetails(Long userId, Long reservationId) {
        validateDetailRequest(userId, reservationId);

        ReservationDetailVO detail = reservationMapper.selectReservationDetails(userId, reservationId);
        if (detail == null) {
            throw new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND);
        }

        LocalDate today = LocalDate.now();
        LocalDateTime reviewDeadline = detail.getEndDate()
                .plusDays(30)
                .atTime(LocalTime.MAX);

//        예약이 확정된(CONFIRMED) 상태이고 이용 시작일 이전이면 취소 가능
        boolean cancelable = detail.getStatus() == ReservationStatus.CONFIRMED
                && today.isBefore(detail.getStartDate());

//        이용 종료 후부터 종료일 기준 30일 이내이면 후기 작성 가능
        boolean reviewPeriod = today.isAfter(detail.getEndDate())
                && !today.isAfter(detail.getEndDate().plusDays(30));

//        후기 작성 가능 여부와 기존 후기 존재 여부를 바탕으로 화면 동작을 결정
        boolean activeReview = detail.getReviewId() != null
                && "ACTIVE".equals(detail.getReviewStatus());

        ReservationReviewAction reviewAction = findReviewAction(
                detail,
                reviewPeriod,
                activeReview
        );
        Long activeReviewId = activeReview ? detail.getReviewId() : null;

        return ReservationDetailResponseDTO.from(
                detail,
                cancelable,
                activeReviewId,
                reviewAction,
                reviewDeadline
        );
    }

    @Override
    @Transactional(readOnly = true)
//    userId 사용자의 reservationId 기준 예약 취소 상세 조회
    public ReservationCancellationDetailResponseDTO findReservationCancellationDetails(
            Long userId,
            Long reservationId) {

        validateDetailRequest(userId, reservationId);

        ReservationCancellationDetailVO detail =
                reservationMapper.selectReservationCancellationDetails(userId, reservationId);
        if (detail == null) {
            throw new BusinessException(ReservationErrorCode.RESERVATION_CANCELLATION_NOT_FOUND);
        }

        return ReservationCancellationDetailResponseDTO.from(detail);
    }

    // 리뷰 작성 이력과 이용 종료 후 30일 기한을 기준으로 화면 동작을 결정한다.
    private ReservationReviewAction findReviewAction(
            ReservationDetailVO detail,
            boolean reviewPeriod,
            boolean activeReview) {

        if (!reviewPeriod) {
            return ReservationReviewAction.NONE;
        }

        if (activeReview) {
            return ReservationReviewAction.EDIT;
        }

        if (detail.getReviewId() == null) {
            return ReservationReviewAction.WRITE;
        }

        return ReservationReviewAction.NONE;
    }

    // 상세 조회에 필요한 사용자와 예약 식별자를 검증한다.
    private void validateDetailRequest(Long userId, Long reservationId) {
        if (userId == null || userId < 1) {
            throw new IllegalArgumentException("사용자 정보가 올바르지 않습니다.");
        }

        if (reservationId == null || reservationId < 1) {
            throw new BusinessException(ReservationErrorCode.INVALID_RESERVATION_ID);
        }
    }

    // 필수 조회 조건과 페이징 범위를 검증한다.
    private void validateRequest(
            Long userId,
            List<ReservationStatus> statuses,
            int page,
            int size) {

        if (userId == null || userId < 1) {
            throw new IllegalArgumentException("사용자 정보가 올바르지 않습니다.");
        }

        if (statuses == null || statuses.isEmpty() || statuses.contains(null)) {
            throw new BusinessException(ReservationErrorCode.RESERVATION_STATUS_REQUIRED);
        }

        if (page < 0) {
            throw new IllegalArgumentException("페이지 번호는 0 이상이어야 합니다.");
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("페이지 크기는 1 이상 50 이하여야 합니다.");
        }

        if (page > Integer.MAX_VALUE / size) {
            throw new IllegalArgumentException("요청한 페이지 범위가 너무 큽니다.");
        }
    }
}
