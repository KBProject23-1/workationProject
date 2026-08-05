package com.workit.domain.reservation.mapper;

import com.workit.domain.reservation.vo.ReservationCategory;
import com.workit.domain.reservation.vo.ReservationCancellationDetailVO;
import com.workit.domain.reservation.vo.ReservationCreateProductVO;
import com.workit.domain.reservation.vo.ReservationCreateVO;
import com.workit.domain.reservation.vo.ReservationCreateWorkationVO;
import com.workit.domain.reservation.vo.ReservationDailyInventoryVO;
import com.workit.domain.reservation.vo.ReservationDetailVO;
import com.workit.domain.reservation.vo.ReservationListItemVO;
import com.workit.domain.reservation.vo.ReservationStatus;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 예약 데이터 조회를 담당하는 MyBatis Mapper
 */
public interface ReservationMapper {

    // 로그인 사용자의 진행 중 워케이션을 잠금 조회
    ReservationCreateWorkationVO selectWorkationForUpdate(
            @Param("userId") Long userId,
            @Param("workationId") Long workationId
    );

    // 가격이 결제 중 변경되지 않도록 예약 상품을 잠금 조회
    ReservationCreateProductVO selectReservationProductForUpdate(
            @Param("productId") Long productId
    );

    // 동일 워케이션에서 같은 카테고리의 겹치는 확정 예약 수를 조회
    int countOverlappingReservation(
            @Param("userId") Long userId,
            @Param("workationId") Long workationId,
            @Param("category") ReservationCategory category,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("includeEndDate") boolean includeEndDate
    );

    // 상품의 예약 대상 일별 재고를 날짜 순서대로 잠금 조회
    List<ReservationDailyInventoryVO> selectDailyInventoriesForUpdate(
            @Param("productId") Long productId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("includeEndDate") boolean includeEndDate
    );

    // 결제와 재고 차감이 완료될 확정 예약을 등록
    int insertReservation(ReservationCreateVO reservation);

    // 생성된 PK를 사용한 최종 예약번호로 변경
    int updateReservationCode(
            @Param("reservationId") Long reservationId,
            @Param("reservationCode") String reservationCode
    );

    // 잠금 조회한 일별 재고를 조건부 차감
    int updateDailyInventory(
            @Param("dailyInventoryId") Long dailyInventoryId,
            @Param("reservedCount") int reservedCount
    );

    // 확정 예약과 차감한 일별 재고의 관계를 저장
    int insertReservationDailyInventory(
            @Param("reservationId") Long reservationId,
            @Param("dailyInventoryId") Long dailyInventoryId,
            @Param("reservedCount") int reservedCount
    );

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
