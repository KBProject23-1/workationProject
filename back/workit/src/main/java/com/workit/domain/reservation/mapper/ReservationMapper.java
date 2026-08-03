package com.workit.domain.reservation.mapper;

import com.workit.domain.reservation.vo.ReservationCategory;
import com.workit.domain.reservation.vo.ReservationListItemVO;
import com.workit.domain.reservation.vo.ReservationStatus;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ReservationMapper {

//    사용자·상태·카테고리 조건에 맞는 예약 목록 한 페이지를 조회
    List<ReservationListItemVO> selectReservationList(
            @Param("userId") Long userId,
            @Param("statuses") List<ReservationStatus> statuses,
            @Param("category") ReservationCategory category,
            @Param("offset") int offset,
            @Param("size") int size,
            @Param("canceledOnly") boolean canceledOnly
    );

//    목록 쿼리와 동일한 필터 조건으로 전체 예약 건수를 조회
    long countReservationList(
            @Param("userId") Long userId,
            @Param("statuses") List<ReservationStatus> statuses,
            @Param("category") ReservationCategory category
    );
}
