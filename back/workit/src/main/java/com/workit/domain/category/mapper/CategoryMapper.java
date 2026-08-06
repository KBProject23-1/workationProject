package com.workit.domain.category.mapper;

import com.workit.domain.category.vo.ExpenseCategoryVO;
import com.workit.domain.workation.vo.BudgetType;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CategoryMapper {

    // 예산 유형별 카테고리 마스터 + 사용자 별칭 조회
    List<ExpenseCategoryVO> selectCategoryList(@Param("userId") Long userId,
                                               @Param("budgetType") BudgetType budgetType);

    // 해당 워케이션 예산에 배정된 카테고리 + 사용자 별칭 조회
    List<ExpenseCategoryVO> selectBudgetedCategoryList(@Param("userId") Long userId,
                                                       @Param("workationId") Long workationId,
                                                       @Param("budgetType") BudgetType budgetType);

    // 카테고리 1건 + 해당 사용자의 별칭 조회. 없으면 null
    ExpenseCategoryVO selectCategoryById(@Param("userId") Long userId,
                                         @Param("categoryId") Long categoryId);

    // 별칭 등록 또는 수정 (없으면 INSERT, 있으면 UPDATE)
    int upsertCategoryLabel(@Param("userId") Long userId,
                            @Param("categoryId") Long categoryId,
                            @Param("customName") String customName);

    // 별칭 삭제 - 기본 이름으로 복원할 때 사용
    int deleteCategoryLabel(@Param("userId") Long userId,
                            @Param("categoryId") Long categoryId);
}
