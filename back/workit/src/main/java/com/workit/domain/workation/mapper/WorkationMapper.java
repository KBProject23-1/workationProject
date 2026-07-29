package com.workit.domain.workation.mapper;

import com.workit.domain.workation.domain.BudgetSpentVO;
import com.workit.domain.workation.domain.WorkationHistoryVO;
import com.workit.domain.workation.domain.WorkationVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface WorkationMapper {

    void insertWorkation(WorkationVO vo);

    WorkationVO selectWorkationById(@Param("id") Long id);

    /** 진행 중(ACTIVE) 워케이션 1건. 없으면 null */
    WorkationVO selectActiveWorkation(@Param("userId") Long userId);

    /** 예산 유형별 지출 합계 */
    List<BudgetSpentVO> selectBudgetSpentList(@Param("workationId") Long workationId);

    /** 자동분류 후 사용자가 확인하지 않은 지출 건수 */
    int countUncheckedExpenses(@Param("workationId") Long workationId);

    int countActiveWorkation(@Param("userId") Long userId);

    int countRegion(@Param("regionId") Long regionId);

    /** 정산 완료된 워케이션 목록 (최신순, 페이징) */
    List<WorkationHistoryVO> selectSettledWorkationList(@Param("userId") Long userId,
                                                        @Param("offset") int offset,
                                                        @Param("size") int size);

    /** 정산 완료된 워케이션 총 건수 */
    long countSettledWorkation(@Param("userId") Long userId);
}