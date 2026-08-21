package com.workit.domain.workation.mapper;

import com.workit.domain.workation.vo.BudgetSpentVO;
import com.workit.domain.workation.vo.WorkationHistoryVO;
import com.workit.domain.workation.vo.WorkationReservationVO;
import com.workit.domain.workation.vo.WorkationVO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

public interface WorkationMapper {

    void insertWorkation(WorkationVO vo);

    WorkationVO selectWorkationById(@Param("id") Long id);

    // 진행 중(ACTIVE) 워케이션 1건. 없으면 null
    WorkationVO selectActiveWorkation(@Param("userId") Long userId);

    // 예산 유형별 지출 합계
    List<BudgetSpentVO> selectBudgetSpentList(@Param("workationId") Long workationId);

    // 자동분류 후 사용자가 확인하지 않은 지출 건수
    int countUncheckedExpenses(@Param("workationId") Long workationId);

    int countActiveWorkation(@Param("userId") Long userId);

    int countRegion(@Param("regionId") Long regionId);

    // 정산 완료된 워케이션 목록 (최신순, 페이징)
    List<WorkationHistoryVO> selectSettledWorkationList(@Param("userId") Long userId,
                                                        @Param("offset") int offset,
                                                        @Param("size") int size);

    // 정산 완료된 워케이션 총 건수
    long countSettledWorkation(@Param("userId") Long userId);

    // 워케이션 수정
    int updateWorkation(WorkationVO vo);

    // 변경할 기간 밖에 있는 지출 건수 (기간 축소 시 검증용)
    int countExpensesOutOfPeriod(@Param("workationId") Long workationId,
                                 @Param("startDate") LocalDate startDate,
                                 @Param("endDate") LocalDate endDate);

    // 기간을 벗어나는 지출 중 수기 등록 건수. 삭제하면 복구할 수 없어 따로 센다
    int countManualExpensesOutOfPeriod(@Param("workationId") Long workationId,
                                       @Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate);

    // 기간을 벗어나는 지출을 워케이션에서 분리. transactions 는 남으므로 앱 결제는 재유입된다
    int deleteExpensesOutOfPeriod(@Param("workationId") Long workationId,
                                  @Param("startDate") LocalDate startDate,
                                  @Param("endDate") LocalDate endDate);

    // 워케이션에 묶인 살아 있는 예약. 기간 변경 영향을 판단하는 데 쓴다
    List<WorkationReservationVO> selectActiveReservations(@Param("workationId") Long workationId);

    // 추천 이력 삭제. recommendation_results 는 CASCADE 로 따라 삭제된다
    int deleteRecommendationRequestsByWorkationId(@Param("workationId") Long workationId);

    // 예약 이력은 남기고 워케이션 연결만 끊는다
    int unlinkReservations(@Param("workationId") Long workationId);

    // 워케이션 삭제 (하위 데이터 정리 후 호출)
    int deleteWorkation(@Param("id") Long id);

    // 하위 데이터 삭제
    int deleteExpensesByWorkationId(@Param("workationId") Long workationId);
    int deleteBudgetsByWorkationId(@Param("workationId") Long workationId);

    // 결제 원본은 보존하고 워케이션 연결만 해제
    int unlinkTransactions(@Param("workationId") Long workationId);

    // 워케이션 종료 (ACTIVE → SETTLED)
    int settleWorkation(@Param("id") Long id);

    // 내일 시작하는 ACTIVE 워케이션 조회
    List<WorkationVO> selectWorkationsByStartDate(@Param("startDate") LocalDate startDate);

    // 내일 종료하는 ACTIVE 워케이션 조회
    List<WorkationVO> selectWorkationsByEndDate(@Param("endDate") LocalDate endDate);
}