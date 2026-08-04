package com.workit.domain.reservation.mapper;

import com.workit.domain.reservation.vo.ReservationCategory;
import com.workit.domain.reservation.vo.ReservationCancellationDetailVO;
import com.workit.domain.reservation.vo.ReservationDetailVO;
import com.workit.domain.reservation.vo.ReservationListItemVO;
import com.workit.domain.reservation.vo.ReservationStatus;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 예약 데이터 조회를 담당하는 MyBatis Mapper
 */
public interface ReservationMapper {

    /**
     * 사용자·상태·카테고리 조건에 맞는 예약 목록 한 페이지를 조회한다.
     */
    List<ReservationListItemVO> selectReservationList(
            @Param("userId") Long userId,
            @Param("statuses") List<ReservationStatus> statuses,
            @Param("category") ReservationCategory category,
            @Param("offset") int offset,
            @Param("size") int size,
            @Param("canceledOnly") boolean canceledOnly
    );

    /**
     * 목록 쿼리와 동일한 필터 조건으로 전체 예약 건수를 조회한다.
     */
    long countReservationList(
            @Param("userId") Long userId,
            @Param("statuses") List<ReservationStatus> statuses,
            @Param("category") ReservationCategory category
    );

//  로그인 사용자가 소유한 예약 확정·이용 완료 상세를 조회한다.
    ReservationDetailVO selectReservationDetails(
            @Param("userId") Long userId,
            @Param("reservationId") Long reservationId
    );

    /**
     * 로그인 사용자가 소유한 예약 취소 상세를 조회
     */
    ReservationCancellationDetailVO selectReservationCancellationDetails(
            @Param("userId") Long userId,
            @Param("reservationId") Long reservationId
    );
}
