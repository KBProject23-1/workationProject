package com.workit.domain.category.service;

import com.workit.domain.category.dto.request.CategoryLabelRequestDTO;
import com.workit.domain.category.dto.response.CategoryLabelResponseDTO;
import com.workit.domain.category.dto.response.CategoryListResponseDTO;
import com.workit.domain.category.dto.response.CategoryResponseDTO;
import com.workit.domain.category.exception.CategoryErrorCode;
import com.workit.domain.category.mapper.CategoryMapper;
import com.workit.domain.category.vo.ExpenseCategoryVO;
import com.workit.domain.workation.vo.BudgetType;
import com.workit.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    // 기본 카테고리명 중 가장 긴 것이 7자("세탁·생활서비스")이므로 10자로 제한
    // DB 컬럼은 VARCHAR(50) 이지만 화면 레이아웃이 깨지지 않도록 애플리케이션에서 더 좁게 막음
    private static final int MAX_CUSTOM_NAME_LENGTH = 10;

    private final CategoryMapper categoryMapper;

    @Override
    @Transactional(readOnly = true)
    public CategoryListResponseDTO getCategoryList(Long userId, BudgetType budgetType) {

        List<CategoryResponseDTO> categories = categoryMapper
                .selectCategoryList(userId, budgetType)
                .stream()
                .map(CategoryResponseDTO::from)
                .collect(Collectors.toList());

        return CategoryListResponseDTO.of(budgetType, categories);
    }

    @Override
    @Transactional
    public CategoryLabelResponseDTO modifyCategoryLabel(Long userId, Long categoryId,
                                                        CategoryLabelRequestDTO dto) {

        // 1) 존재하는 카테고리인지 확인
        ExpenseCategoryVO category = categoryMapper.selectCategoryById(userId, categoryId);

        if (category == null) {
            throw new BusinessException(CategoryErrorCode.CATEGORY_NOT_FOUND);
        }

        // 2) 입력값 정리 - 공백만 입력한 경우도 별칭 삭제로 취급한다
        String customName = normalize(dto.getCustomName());

        // 3) null 이면 별칭을 지워 기본 이름으로 복원하고, 값이 있으면 등록·수정한다
        if (customName == null) {
            categoryMapper.deleteCategoryLabel(userId, categoryId);
            log.info("카테고리 별칭 삭제 - categoryId: {}, userId: {}", categoryId, userId);
        } else {
            validateCustomName(customName);
            categoryMapper.upsertCategoryLabel(userId, categoryId, customName);
            log.info("카테고리 별칭 저장 - categoryId: {}, userId: {}, name: {}",
                    categoryId, userId, customName);
        }

        // 조회 시점의 VO 에는 변경 전 별칭이 담겨 있으므로 변경 결과로 덮어쓴다
        category.setCustomName(customName);

        return CategoryLabelResponseDTO.of(category, LocalDateTime.now());
    }

    // 앞뒤 공백을 제거하고, 빈 문자열은 null 로 통일한다
    private String normalize(String customName) {

        if (customName == null) {
            return null;
        }

        String trimmed = customName.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void validateCustomName(String customName) {
        if (customName.length() > MAX_CUSTOM_NAME_LENGTH) {
            throw new BusinessException(CategoryErrorCode.CATEGORY_NAME_TOO_LONG);
        }
    }
}
