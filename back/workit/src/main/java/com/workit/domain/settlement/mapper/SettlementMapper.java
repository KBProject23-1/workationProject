package com.workit.domain.settlement.mapper;

import com.workit.domain.expense.vo.WorkationExpenseVO;
import com.workit.domain.settlement.vo.SettlementCategoryVO;
import com.workit.domain.settlement.vo.SettlementValidationVO;
import com.workit.domain.workation.vo.BudgetType;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SettlementMapper {

    // 카테고리별 배정 예산 + 실제 집행액
    // 예산은 있는데 지출이 없는 카테고리도 포함해야 미집행 항목이 화면에 보인다
    List<SettlementCategoryVO> selectSettlementCategories(@Param("workationId") Long workationId,
                                                          @Param("userId") Long userId,
                                                          @Param("budgetType") BudgetType budgetType);

    // 정산 전 보완 대상 건수
    SettlementValidationVO selectValidation(@Param("workationId") Long workationId,
                                            @Param("budgetType") BudgetType budgetType);

    // Excel / PDF 출력용 지출 상세. 조회 API 와 달리 페이징 없이 전부 가져온다
    List<WorkationExpenseVO> selectExpenseDetails(@Param("workationId") Long workationId,
                                                  @Param("userId") Long userId,
                                                  @Param("budgetType") BudgetType budgetType);

    int countExpenses(@Param("workationId") Long workationId,
                      @Param("budgetType") BudgetType budgetType);

    // 문서 상단 기본정보에 넣을 신청자 성명
    String selectUserName(@Param("userId") Long userId);

    // 해당 워케이션 지출에 실제로 사용된 법인카드 목록
    // 카드가 여러 장이면 문서에 모두 표기한다
    List<String> selectUsedCardLabels(@Param("workationId") Long workationId,
                                      @Param("budgetType") BudgetType budgetType);
}
