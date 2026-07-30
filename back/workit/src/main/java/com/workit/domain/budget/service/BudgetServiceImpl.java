package com.workit.domain.budget.service;

import com.workit.domain.budget.dto.request.BudgetItemAddRequestDTO;
import com.workit.domain.budget.dto.request.BudgetItemRequestDTO;
import com.workit.domain.budget.dto.request.BudgetSetupRequestDTO;
import com.workit.domain.budget.dto.response.BudgetItemResponseDTO;
import com.workit.domain.budget.dto.response.BudgetStatusResponseDTO;
import com.workit.domain.budget.dto.response.BudgetSummaryResponseDTO;
import com.workit.domain.budget.exception.BudgetErrorCode;
import com.workit.domain.budget.mapper.BudgetMapper;
import com.workit.domain.budget.vo.BudgetItemVO;
import com.workit.domain.workation.vo.BudgetType;
import com.workit.domain.workation.vo.WorkationVO;
import com.workit.exception.BusinessException;
import com.workit.domain.workation.service.WorkationOwnershipValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BudgetServiceImpl implements BudgetService {

    private final BudgetMapper budgetMapper;

    // 예산은 워케이션에 딸린 데이터이므로 접근 전에 소유자를 확인한다
    private final WorkationOwnershipValidator ownershipValidator;

    // =====================================================================================
    // 3.1 예산 사용현황 조회
    // =====================================================================================

    @Override
    @Transactional(readOnly = true)
    public BudgetStatusResponseDTO getBudgetStatus(Long userId, Long workationId, BudgetType budgetType) {

        // 1) 워케이션 존재 여부 + 소유자 검증
        WorkationVO workation = ownershipValidator.getOwned(userId, workationId);

        // 2) 카테고리별 예산 + 지출 집계 조회
        List<BudgetItemVO> items = budgetMapper.selectBudgetItemList(workationId, userId, budgetType);

        // 3) 예산 유형별로 묶는다
        //    SQL 에서 budget_type 순으로 정렬했으므로 LinkedHashMap 으로 순서를 유지한다
        Map<BudgetType, List<BudgetItemVO>> grouped = items.stream()
                .collect(Collectors.groupingBy(BudgetItemVO::getBudgetType,
                        LinkedHashMap::new, Collectors.toList()));

        // 4) 요청받은 유형만, 미지정이면 두 유형 모두 응답에 담는다
        //    예산을 아직 배분하지 않은 유형은 items 가 빈 배열로 내려간다
        List<BudgetSummaryResponseDTO> budgets = new ArrayList<>();

        for (BudgetType type : resolveTypes(budgetType)) {

            List<BudgetItemResponseDTO> itemDTOs = grouped
                    .getOrDefault(type, Collections.emptyList())
                    .stream()
                    .map(BudgetItemResponseDTO::from)
                    .collect(Collectors.toList());

            budgets.add(BudgetSummaryResponseDTO.of(type, getBudgetTotal(workation, type), itemDTOs));
        }

        return BudgetStatusResponseDTO.of(workationId, budgets);
    }

    // =====================================================================================
    // 3.2 예산 카테고리별 세부 금액 설정
    // =====================================================================================

    @Override
    @Transactional
    public BudgetSummaryResponseDTO setupBudget(Long userId, Long workationId, BudgetSetupRequestDTO dto) {

        WorkationVO workation = ownershipValidator.getOwned(userId, workationId);

        BudgetType budgetType = requireBudgetType(dto.getBudgetType());
        List<BudgetItemRequestDTO> items = dto.getItems();

        // 이미 설정된 예산이 있으면 수정 API 로 안내한다
        // 가장 먼저 확인해 불필요한 검증을 생략한다
        if (budgetMapper.countBudgetByType(workationId, budgetType) > 0) {
            throw new BusinessException(BudgetErrorCode.BUDGET_ALREADY_EXISTS);
        }

        validateBudgetItems(userId, budgetType, items, getBudgetTotal(workation, budgetType));

        budgetMapper.upsertBudgetList(workationId, budgetType, items);
        log.info("예산 설정 완료 - workationId: {}, type: {}, 카테고리 {}건",
                workationId, budgetType, items.size());

        // 저장 결과를 조회 응답과 같은 형태로 반환한다
        // 프론트가 설정 직후 추가 호출 없이 사용현황 화면을 그릴 수 있다
        return buildSummary(workationId, userId, budgetType, workation);
    }

    // =====================================================================================
    // 3.3 예산 세부 금액 수정
    // =====================================================================================

    @Override
    @Transactional
    public BudgetSummaryResponseDTO modifyBudget(Long userId, Long workationId, BudgetSetupRequestDTO dto) {

        WorkationVO workation = ownershipValidator.getOwned(userId, workationId);

        BudgetType budgetType = requireBudgetType(dto.getBudgetType());
        List<BudgetItemRequestDTO> items = dto.getItems();

        // 1) 설정된 예산이 없으면 등록 API 로 안내한다 (설정과 반대 조건)
        if (budgetMapper.countBudgetByType(workationId, budgetType) == 0) {
            throw new BusinessException(BudgetErrorCode.BUDGET_NOT_SET);
        }

        // 2) 설정과 동일한 입력값 검증
        validateBudgetItems(userId, budgetType, items, getBudgetTotal(workation, budgetType));

        List<Long> categoryIds = extractCategoryIds(items);

        // 3) 목록에서 빠지는 카테고리에 이미 지출이 있으면 제외할 수 없다
        //    그대로 삭제하면 예산 없는 카테고리에 지출이 남아 정산 합계가 어긋난다
        int orphanExpenseCount = budgetMapper.countExpenseNotIn(workationId, budgetType, categoryIds);

        if (orphanExpenseCount > 0) {
            throw new BusinessException(BudgetErrorCode.BUDGET_CATEGORY_IN_USE,
                    String.format("이미 지출 %d건이 등록된 카테고리를 목록에서 제외할 수 없습니다.", orphanExpenseCount));
        }

        // 4) 요청에 포함된 카테고리는 금액을 갱신하고, 빠진 카테고리는 배정을 삭제한다
        //    UPSERT 를 사용하므로 유지되는 카테고리의 budgetId 는 그대로다
        budgetMapper.upsertBudgetList(workationId, budgetType, items);
        budgetMapper.deleteBudgetsNotIn(workationId, budgetType, categoryIds);

        log.info("예산 수정 완료 - workationId: {}, type: {}, 카테고리 {}건",
                workationId, budgetType, items.size());

        return buildSummary(workationId, userId, budgetType, workation);
    }

    // =====================================================================================
    // 3.4 예산 카테고리 추가 (＋ 버튼)
    // =====================================================================================

    @Override
    @Transactional
    public BudgetItemResponseDTO addBudgetItem(Long userId, Long workationId, BudgetItemAddRequestDTO dto) {

        ownershipValidator.getOwned(userId, workationId);

        BudgetType budgetType = requireBudgetType(dto.getBudgetType());
        Long categoryId = dto.getExpenseCategoryId();
        BigDecimal targetAmount = dto.getTargetAmount();

        if (categoryId == null) {
            throw new IllegalArgumentException("카테고리를 선택해 주세요.");
        }
        if (targetAmount == null || targetAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(BudgetErrorCode.BUDGET_AMOUNT_NEGATIVE);
        }

        // 1) 해당 예산 유형에 속한 카테고리인지 확인
        List<Long> categoryIds = Collections.singletonList(categoryId);

        if (budgetMapper.countValidCategories(budgetType, userId, categoryIds) != 1) {
            throw new BusinessException(BudgetErrorCode.BUDGET_TYPE_MISMATCH);
        }

        // 2) 이미 추가된 카테고리면 UNIQUE 제약 위반으로 500 이 되므로 미리 막는다
        if (budgetMapper.countBudgetItem(workationId, budgetType, categoryId) > 0) {
            throw new BusinessException(BudgetErrorCode.BUDGET_ITEM_ALREADY_EXISTS);
        }

        budgetMapper.insertBudgetItem(workationId, budgetType, categoryId, targetAmount);
        log.info("예산 카테고리 추가 - workationId: {}, type: {}, categoryId: {}",
                workationId, budgetType, categoryId);

        // 추가 직후에는 지출이 없으므로 목록 조회에서 해당 카테고리만 골라 반환한다
        return budgetMapper.selectBudgetItemList(workationId, userId, budgetType)
                .stream()
                .filter(vo -> categoryId.equals(vo.getExpenseCategoryId()))
                .findFirst()
                .map(BudgetItemResponseDTO::from)
                .orElseThrow(() -> new BusinessException(BudgetErrorCode.BUDGET_NOT_FOUND));
    }

    // =====================================================================================
    // 3.5 예산 카테고리 삭제 (− 버튼)
    // =====================================================================================

    @Override
    @Transactional
    public void removeBudgetItem(Long userId, Long workationId, Long budgetId, boolean force) {

        ownershipValidator.getOwned(userId, workationId);

        // 1) 예산 배정 존재 여부 확인
        BudgetItemVO budget = budgetMapper.selectBudgetItemById(budgetId, userId);

        if (budget == null) {
            throw new BusinessException(BudgetErrorCode.BUDGET_NOT_FOUND);
        }

        // 2) 경로의 워케이션과 실제 소유 워케이션이 다르면 잘못된 요청이다
        //    다른 워케이션의 예산 id 를 넣어 삭제하는 것을 막는다
        if (!workationId.equals(budget.getWorkationId())) {
            throw new BusinessException(BudgetErrorCode.BUDGET_NOT_FOUND);
        }

        // 3) 기타 카테고리는 분류되지 않은 지출이 모이는 곳이라 삭제할 수 없다
        if (Boolean.FALSE.equals(budget.getIsDeletable())) {
            throw new BusinessException(BudgetErrorCode.BUDGET_ETC_DELETE_DENIED);
        }

        // 4) 지출이 있으면 기본적으로 막고, force 가 true 면 예산 배정만 삭제한다
        //    지출 내역 자체는 남겨야 정산 근거가 사라지지 않는다
        int expenseCount = budgetMapper.countExpenseByCategory(
                workationId, budget.getBudgetType(), budget.getExpenseCategoryId());

        if (expenseCount > 0 && !force) {
            throw new BusinessException(BudgetErrorCode.BUDGET_ITEM_HAS_EXPENSE,
                    String.format("이 카테고리로 등록된 지출이 %d건 있습니다. "
                            + "삭제하면 예산 배정만 사라지고 지출 내역은 유지됩니다.", expenseCount));
        }

        budgetMapper.deleteBudgetItem(budgetId);
        log.info("예산 카테고리 삭제 - workationId: {}, budgetId: {}, 지출 {}건, force: {}",
                workationId, budgetId, expenseCount, force);
    }

    // 공통 예산 배정 입력값 검증
    // DB 조회가 필요 없는 검증을 먼저 수행해, 잘못된 요청에서 쿼리를 아예 실행하지 않는다
    private void validateBudgetItems(Long userId, BudgetType budgetType,
                                     List<BudgetItemRequestDTO> items, BigDecimal budgetTotal) {

        if (items == null || items.isEmpty()) {
            throw new BusinessException(BudgetErrorCode.BUDGET_ITEMS_REQUIRED);
        }

        // 1) 카테고리와 금액이 유효한 값인지 확인
        for (BudgetItemRequestDTO item : items) {

            if (item.getExpenseCategoryId() == null) {
                throw new IllegalArgumentException("카테고리를 선택해 주세요.");
            }
            if (item.getTargetAmount() == null
                    || item.getTargetAmount().compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException(BudgetErrorCode.BUDGET_AMOUNT_NEGATIVE);
            }
        }

        List<Long> categoryIds = extractCategoryIds(items);

        // 2) 같은 카테고리를 두 번 배정하면 UNIQUE 제약 위반으로 500 이 되므로 미리 막는다
        if (new HashSet<>(categoryIds).size() != categoryIds.size()) {
            throw new BusinessException(BudgetErrorCode.BUDGET_CATEGORY_DUPLICATED);
        }

        // 3) 요청한 카테고리가 모두 해당 예산 유형에 속하는지 확인
        //    개수가 다르면 다른 유형 카테고리나 존재하지 않는 id 가 섞여 있다
        if (budgetMapper.countValidCategories(budgetType, userId, categoryIds) != categoryIds.size()) {
            throw new BusinessException(BudgetErrorCode.BUDGET_TYPE_MISMATCH);
        }

        // 4) 분류되지 않은 지출이 갈 곳이 필요하므로 기타 카테고리는 필수
        Long etcCategoryId = budgetMapper.selectEtcCategoryId(budgetType);

        if (etcCategoryId != null && !categoryIds.contains(etcCategoryId)) {
            throw new BusinessException(BudgetErrorCode.BUDGET_ETC_REQUIRED);
        }

        // 5) 배정 합계가 총예산과 정확히 일치해야 한다
        //    BigDecimal 은 equals 가 소수 자릿수까지 비교하므로 compareTo 를 사용한다
        //    (1250000 과 1250000.00 을 equals 로 비교하면 false)
        BigDecimal targetSum = items.stream()
                .map(BudgetItemRequestDTO::getTargetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal total = BudgetItemResponseDTO.nvl(budgetTotal);

        if (targetSum.compareTo(total) != 0) {
            throw new BusinessException(BudgetErrorCode.BUDGET_SUM_MISMATCH,
                    String.format("카테고리 배정 합계가 총예산과 일치하지 않습니다. (배정 %,.0f원 / 총예산 %,.0f원)",
                            targetSum, total));
        }
    }

    private List<Long> extractCategoryIds(List<BudgetItemRequestDTO> items) {
        return items.stream()
                .map(BudgetItemRequestDTO::getExpenseCategoryId)
                .collect(Collectors.toList());
    }

    private BudgetType requireBudgetType(BudgetType budgetType) {
        if (budgetType == null) {
            throw new IllegalArgumentException("예산 유형을 지정해 주세요.");
        }
        return budgetType;
    }

    // 예산 유형 1건의 사용현황을 조회해 응답 형태로 만든다
    private BudgetSummaryResponseDTO buildSummary(Long workationId, Long userId,
                                                  BudgetType budgetType, WorkationVO workation) {

        List<BudgetItemResponseDTO> itemDTOs = budgetMapper
                .selectBudgetItemList(workationId, userId, budgetType)
                .stream()
                .map(BudgetItemResponseDTO::from)
                .collect(Collectors.toList());

        return BudgetSummaryResponseDTO.of(budgetType, getBudgetTotal(workation, budgetType), itemDTOs);
    }

    // 파라미터가 없으면 법인·개인 두 유형을 모두 조회한다
    private List<BudgetType> resolveTypes(BudgetType budgetType) {
        return (budgetType != null)
                ? Collections.singletonList(budgetType)
                : Arrays.asList(BudgetType.WORK, BudgetType.PERSONAL);
    }

    // 예산 유형에 맞는 총예산을 워케이션에서 꺼낸다
    private BigDecimal getBudgetTotal(WorkationVO workation, BudgetType type) {
        return (type == BudgetType.WORK)
                ? workation.getBusinessBudgetTotal()
                : workation.getPersonalBudgetTotal();
    }
}
