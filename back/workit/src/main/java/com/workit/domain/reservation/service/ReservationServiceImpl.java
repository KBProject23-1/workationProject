package com.workit.domain.reservation.service;

import com.workit.domain.reservation.dto.response.ReservationListItemResponseDTO;
import com.workit.domain.reservation.mapper.ReservationMapper;
import com.workit.domain.reservation.vo.ReservationCategory;
import com.workit.domain.reservation.vo.ReservationStatus;
import com.workit.global.dto.PageResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

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

//    필수 조회 조건과 페이징 범위를 검증한다.
    private void validateRequest(
            Long userId,
            List<ReservationStatus> statuses,
            int page,
            int size) {

        if (userId == null || userId < 1) {
            throw new IllegalArgumentException("사용자 정보가 올바르지 않습니다.");
        }

        if (statuses == null || statuses.isEmpty() || statuses.contains(null)) {
            throw new IllegalArgumentException("예약 상태를 한 개 이상 선택해야 합니다.");
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
