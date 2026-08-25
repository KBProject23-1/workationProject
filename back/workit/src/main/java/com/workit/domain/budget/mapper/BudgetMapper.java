package com.workit.domain.budget.mapper;

import com.workit.domain.budget.dto.request.BudgetItemRequestDTO;
import com.workit.domain.budget.vo.BudgetItemVO;
import com.workit.domain.workation.vo.BudgetType;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

public interface BudgetMapper {

    // 카테고리별 예산 + 실제 지출 집계 조회
    // budgetType 이 null 이면 WORK / PERSONAL 전체를 조회한다
    List<BudgetItemVO> selectBudgetItemList(@Param("workationId") Long workationId,
                                            @Param("userId") Long userId,
                                            @Param("budgetType") BudgetType budgetType);

    // 예산 배정 단건 조회. 없으면 null
    BudgetItemVO selectBudgetItemById(@Param("budgetId") Long budgetId,
                                      @Param("userId") Long userId);

    // 예산 유형별 배정 건수. 이미 설정됐는지 확인용
    int countBudgetByType(@Param("workationId") Long workationId,
                          @Param("budgetType") BudgetType budgetType);

    // 특정 카테고리가 이미 예산에 추가되어 있는지 확인
    int countBudgetItem(@Param("workationId") Long workationId,
                        @Param("budgetType") BudgetType budgetType,
                        @Param("expenseCategoryId") Long expenseCategoryId);

    // 카테고리 배정 일괄 저장
    // 이미 있는 카테고리는 금액만 갱신하므로 budgetId 가 유지된다
    int upsertBudgetList(@Param("workationId") Long workationId,
                         @Param("budgetType") BudgetType budgetType,
                         @Param("items") List<BudgetItemRequestDTO> items);

    // 배정 단건 추가
    int insertBudgetItem(@Param("workationId") Long workationId,
                         @Param("budgetType") BudgetType budgetType,
                         @Param("expenseCategoryId") Long expenseCategoryId,
                         @Param("targetAmount") BigDecimal targetAmount);

    // 목록에서 빠진 카테고리의 배정을 삭제한다
    int deleteBudgetsNotIn(@Param("workationId") Long workationId,
                           @Param("budgetType") BudgetType budgetType,
                           @Param("categoryIds") List<Long> categoryIds);

    int deleteBudgetItem(@Param("budgetId") Long budgetId);

    // 목록에서 빠지는 카테고리 중 지출이 등록된 건수
    int countExpenseNotIn(@Param("workationId") Long workationId,
                          @Param("budgetType") BudgetType budgetType,
                          @Param("categoryIds") List<Long> categoryIds);

    // 특정 카테고리로 등록된 지출 건수
    int countExpenseByCategory(@Param("workationId") Long workationId,
                               @Param("budgetType") BudgetType budgetType,
                               @Param("expenseCategoryId") Long expenseCategoryId);

    // 요청한 카테고리 중 해당 예산 유형에 속하는 것의 개수
    // 요청 개수와 다르면 다른 유형 카테고리나 존재하지 않는 id 가 섞인 것이다
    int countValidCategories(@Param("budgetType") BudgetType budgetType,
                             @Param("userId") Long userId,
                             @Param("categoryIds") List<Long> categoryIds);

    // 기타(ETC) 카테고리 id 조회
    Long selectEtcCategoryId(@Param("budgetType") BudgetType budgetType);

    // 추천 계산에서 업무 예산(예: RENT) 금액을 조회한다
    BigDecimal selectWorkBudgetByCategoryCode(@Param("workationId") Long workationId,
                                             @Param("budgetType") BudgetType budgetType,
                                             @Param("categoryCode") String categoryCode);
}
