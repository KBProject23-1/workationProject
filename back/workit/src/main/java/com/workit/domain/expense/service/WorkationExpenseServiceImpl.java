package com.workit.domain.expense.service;

import com.workit.domain.category.mapper.CategoryMapper;
import com.workit.domain.category.vo.ExpenseCategoryVO;
import com.workit.domain.expense.dto.request.ExpenseBudgetTypeChangeRequestDTO;
import com.workit.domain.expense.dto.request.ExpenseCategoryChangeRequestDTO;
import com.workit.domain.expense.dto.request.ExpenseCreateRequestDTO;
import com.workit.domain.expense.dto.request.ExpenseUpdateRequestDTO;
import com.workit.domain.expense.dto.response.ExpenseBudgetTypeChangeResponseDTO;
import com.workit.domain.expense.dto.response.ExpenseCategoryChangeResponseDTO;
import com.workit.domain.expense.dto.response.ExpenseDetailResponseDTO;
import com.workit.domain.expense.dto.response.ExpenseItemResponseDTO;
import com.workit.domain.expense.dto.response.ExpenseListResponseDTO;
import com.workit.domain.expense.dto.response.ExpenseSummaryResponseDTO;
import com.workit.domain.expense.exception.ExpenseErrorCode;
import com.workit.domain.expense.mapper.WorkationExpenseMapper;
import com.workit.domain.expense.vo.WorkationExpenseVO;
import com.workit.domain.workation.service.WorkationOwnershipValidator;
import com.workit.domain.workation.vo.BudgetType;
import com.workit.domain.workation.vo.WorkationVO;
import com.workit.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkationExpenseServiceImpl implements WorkationExpenseService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_MERCHANT_NAME_LENGTH = 150;
    private static final int MAX_MEMO_LENGTH = 255;

    private final WorkationExpenseMapper expenseMapper;
    private final CategoryMapper categoryMapper;
    private final WorkationOwnershipValidator ownershipValidator;

    // =====================================================================================
    // 5.1 지출 목록 조회
    // =====================================================================================

    @Override
    @Transactional
    public ExpenseListResponseDTO getExpenseList(Long userId, Long workationId, BudgetType budgetType,
                                                Long expenseCategoryId, Boolean uncheckedOnly,
                                                int page, int size) {

        WorkationVO workation = ownershipValidator.getOwned(userId, workationId);

        // 1) 앱 내 결제를 지출로 유입시킨다
        //    조회 시점에 처리하므로 결제 파트가 워케이션을 알 필요가 없다
        importAppPayments(userId, workation);

        // 2) 잘못된 페이징 값이 들어와도 목록이 깨지지 않도록 보정
        int safePage = Math.max(page, 0);
        int safeSize = (size < 1 || size > MAX_PAGE_SIZE) ? DEFAULT_PAGE_SIZE : size;

        ExpenseSummaryResponseDTO summary = ExpenseSummaryResponseDTO
                .from(expenseMapper.selectExpenseSummary(workationId));

        long totalElements = expenseMapper.countExpenseList(
                workationId, budgetType, expenseCategoryId, uncheckedOnly);

        // 결과가 없으면 조인이 많은 목록 쿼리를 실행하지 않는다
        if (totalElements == 0) {
            return ExpenseListResponseDTO.of(workationId, summary,
                    Collections.emptyList(), safePage, safeSize, 0);
        }

        List<ExpenseItemResponseDTO> content = expenseMapper
                .selectExpenseList(workationId, userId, budgetType, expenseCategoryId,
                        uncheckedOnly, safePage * safeSize, safeSize)
                .stream()
                .map(ExpenseItemResponseDTO::from)
                .collect(Collectors.toList());

        return ExpenseListResponseDTO.of(workationId, summary, content,
                safePage, safeSize, totalElements);
    }

    // =====================================================================================
    // 5.2 외부 결제내역 추가
    // =====================================================================================

    @Override
    @Transactional
    public ExpenseItemResponseDTO addExpense(Long userId, Long workationId, ExpenseCreateRequestDTO dto) {

        WorkationVO workation = ownershipValidator.getOwnedActive(userId, workationId, "지출을 등록");

        BudgetType budgetType = requireBudgetType(dto.getBudgetType());

        validateMerchantName(dto.getMerchantName());
        validateAmount(dto.getAmount());
        validateMemo(dto.getMemo());
        validateSpentDate(dto.getSpentDate(), workation);
        validateCategory(userId, budgetType, dto.getExpenseCategoryId());
        validateCard(userId, budgetType, dto.getCardId());

        WorkationExpenseVO vo = new WorkationExpenseVO();
        vo.setWorkationId(workationId);
        vo.setTransactionId(null);              // 수기 입력 건은 연결할 결제가 없다
        vo.setExpenseCategoryId(dto.getExpenseCategoryId());
        vo.setCardId(dto.getCardId());
        vo.setBudgetType(budgetType);
        vo.setAmount(dto.getAmount());
        vo.setMerchantName(dto.getMerchantName().trim());
        vo.setSpentDate(dto.getSpentDate());
        vo.setMemo(normalize(dto.getMemo()));
        vo.setIsAutoCategorized(false);         // 사용자가 직접 고른 카테고리다

        expenseMapper.insertExpense(vo);
        log.info("지출 등록 완료 - expenseId: {}, workationId: {}, type: {}",
                vo.getId(), workationId, budgetType);

        // 카테고리명·카드명은 조인해야 얻을 수 있으므로 재조회해서 응답한다
        return ExpenseItemResponseDTO.from(getOwnedExpense(userId, vo.getId()));
    }

    // =====================================================================================
    // 5.3 지출 단건 상세조회
    // =====================================================================================

    @Override
    @Transactional(readOnly = true)
    public ExpenseDetailResponseDTO getExpenseDetail(Long userId, Long expenseId) {

        WorkationExpenseVO vo = getOwnedExpense(userId, expenseId);

        return ExpenseDetailResponseDTO.of(vo, availableCategories(userId, vo.getBudgetType()));
    }

    // =====================================================================================
    // 5.4 지출 수정
    // =====================================================================================

    @Override
    @Transactional
    public ExpenseDetailResponseDTO modifyExpense(Long userId, Long expenseId, ExpenseUpdateRequestDTO dto) {

        WorkationExpenseVO target = getOwnedExpense(userId, expenseId);

        ownershipValidator.getOwnedActive(userId, target.getWorkationId(), "지출을 수정");

        // 앱 내 결제는 카드사가 보낸 원본 값이므로 금액·일시·가맹점명을 바꿀 수 없다
        if (target.getTransactionId() != null) {
            throw new BusinessException(ExpenseErrorCode.APP_PAYMENT_NOT_EDITABLE);
        }

        validateMerchantName(dto.getMerchantName());
        validateAmount(dto.getAmount());
        validateMemo(dto.getMemo());

        WorkationVO workation = ownershipValidator.getOwned(userId, target.getWorkationId());
        validateSpentDate(dto.getSpentDate(), workation);

        // 예산 유형은 5.7 에서만 바꾸므로 기존 유형 기준으로 카드를 검증한다
        validateCard(userId, target.getBudgetType(), dto.getCardId());

        expenseMapper.updateExpense(expenseId, dto.getMerchantName().trim(), dto.getCardId(),
                dto.getAmount(), dto.getSpentDate(), normalize(dto.getMemo()));

        log.info("지출 수정 완료 - expenseId: {}", expenseId);

        WorkationExpenseVO updated = getOwnedExpense(userId, expenseId);
        return ExpenseDetailResponseDTO.of(updated, availableCategories(userId, updated.getBudgetType()));
    }

    // =====================================================================================
    // 5.5 지출 삭제
    // =====================================================================================

    @Override
    @Transactional
    public void removeExpense(Long userId, Long expenseId) {

        WorkationExpenseVO target = getOwnedExpense(userId, expenseId);

        ownershipValidator.getOwnedActive(userId, target.getWorkationId(), "지출을 삭제");

        // 앱 내 결제는 삭제해도 조회 시점에 다시 유입되므로 애초에 막는다
        // 워케이션 경비가 아니라면 5.7 로 개인소비로 옮기면 된다
        if (target.getTransactionId() != null) {
            throw new BusinessException(ExpenseErrorCode.APP_PAYMENT_NOT_DELETABLE);
        }

        expenseMapper.deleteExpense(expenseId);
        log.info("지출 삭제 완료 - expenseId: {}", expenseId);
    }

    // =====================================================================================
    // 5.6 지출 카테고리 수동 변경
    // =====================================================================================

    @Override
    @Transactional
    public ExpenseCategoryChangeResponseDTO modifyExpenseCategory(Long userId, Long expenseId,
                                                                ExpenseCategoryChangeRequestDTO dto) {

        WorkationExpenseVO target = getOwnedExpense(userId, expenseId);

        ownershipValidator.getOwnedActive(userId, target.getWorkationId(), "지출을 수정");

        Long categoryId = dto.getExpenseCategoryId();

        if (categoryId == null) {
            throw new IllegalArgumentException("카테고리를 선택해 주세요.");
        }
        validateCategory(userId, target.getBudgetType(), categoryId);

        expenseMapper.updateExpenseCategory(expenseId, categoryId);

        // 가맹점이 식별된 건만 규칙을 저장할 수 있다
        // 수기 입력 건은 transaction_id 가 없어 가맹점을 특정할 수 없다
        boolean ruleSaved = false;
        String ruleMessage = null;

        if (dto.isApplyToMerchant() && target.getMerchantId() != null) {

            expenseMapper.upsertCategoryRule(userId, target.getMerchantId(),
                    categoryId, target.getBudgetType());
            ruleSaved = true;
        }

        WorkationExpenseVO updated = getOwnedExpense(userId, expenseId);

        if (ruleSaved) {
            ruleMessage = String.format("다음부터 %s 결제는 %s로 분류됩니다.",
                    updated.getMerchantName(), updated.getDisplayCategoryName());
        }

        log.info("지출 카테고리 변경 - expenseId: {}, categoryId: {}, ruleSaved: {}",
                expenseId, categoryId, ruleSaved);

        return ExpenseCategoryChangeResponseDTO.builder()
                .expenseId(expenseId)
                .expenseCategoryId(categoryId)
                .categoryName(updated.getDisplayCategoryName())
                .isAutoCategorized(updated.getIsAutoCategorized())
                .ruleSaved(ruleSaved)
                .ruleMessage(ruleMessage)
                .build();
    }

    // =====================================================================================
    // 5.7 경비/개인소비 구분 변경
    // =====================================================================================

    @Override
    @Transactional
    public ExpenseBudgetTypeChangeResponseDTO modifyExpenseBudgetType(Long userId, Long expenseId,
                                                                    ExpenseBudgetTypeChangeRequestDTO dto) {

        WorkationExpenseVO target = getOwnedExpense(userId, expenseId);

        ownershipValidator.getOwnedActive(userId, target.getWorkationId(), "지출을 수정");

        BudgetType budgetType = requireBudgetType(dto.getBudgetType());
        Long categoryId = dto.getExpenseCategoryId();

        if (categoryId == null) {
            throw new IllegalArgumentException("변경할 예산 유형의 카테고리를 선택해 주세요.");
        }

        // 예산 유형이 바뀌면 카테고리 마스터도 달라지므로 새 유형 기준으로 검증한다
        if (expenseMapper.countValidCategory(budgetType, userId, categoryId) == 0) {
            throw new BusinessException(ExpenseErrorCode.CATEGORY_TYPE_MISMATCH,
                    "변경할 예산 유형에 해당 카테고리가 없습니다.");
        }

        // 법인으로 바꾸는 경우 기존 카드가 개인카드면 증빙이 성립하지 않는다
        if (budgetType == BudgetType.WORK && target.getTransactionId() == null) {
            validateCard(userId, budgetType, target.getCardId());
        }

        expenseMapper.updateExpenseBudgetType(expenseId, budgetType, categoryId);
        log.info("지출 예산유형 변경 - expenseId: {}, type: {} -> {}",
                expenseId, target.getBudgetType(), budgetType);

        WorkationExpenseVO updated = getOwnedExpense(userId, expenseId);

        return ExpenseBudgetTypeChangeResponseDTO.builder()
                .expenseId(expenseId)
                .budgetType(updated.getBudgetType())
                .expenseCategoryId(updated.getExpenseCategoryId())
                .categoryName(updated.getDisplayCategoryName())
                .build();
    }

    // =====================================================================================
    // 앱 내 결제 유입
    // =====================================================================================

    // 워케이션 기간에 발생한 앱 결제를 지출로 옮긴다
    // transactions 는 결제 파트의 원본이므로 읽기만 하고 수정하지 않는다
    private void importAppPayments(Long userId, WorkationVO workation) {

        List<WorkationExpenseVO> targets = expenseMapper.selectImportTargets(
                workation.getId(), userId, workation.getStartDate(), workation.getEndDate());

        if (targets.isEmpty()) {
            return;
        }

        expenseMapper.insertImportedExpenses(targets);
        log.info("앱 결제 유입 완료 - workationId: {}, {}건", workation.getId(), targets.size());
    }

    // =====================================================================================
    // 공통
    // =====================================================================================

    // 지출 존재 여부 + 워케이션 소유자 검증
    // 소유자 조건은 SQL 에 포함되어 있어 남의 지출은 조회되지 않는다
    private WorkationExpenseVO getOwnedExpense(Long userId, Long expenseId) {

        WorkationExpenseVO vo = expenseMapper.selectExpenseById(expenseId, userId);

        if (vo == null) {
            throw new BusinessException(ExpenseErrorCode.EXPENSE_NOT_FOUND);
        }
        return vo;
    }

    private List<ExpenseCategoryVO> availableCategories(Long userId, BudgetType budgetType) {
        return categoryMapper.selectCategoryList(userId, budgetType);
    }

    private BudgetType requireBudgetType(BudgetType budgetType) {
        if (budgetType == null) {
            throw new IllegalArgumentException("예산 유형을 지정해 주세요.");
        }
        return budgetType;
    }

    private void validateMerchantName(String merchantName) {

        if (merchantName == null || merchantName.trim().isEmpty()) {
            throw new IllegalArgumentException("가맹점명을 입력해 주세요.");
        }
        if (merchantName.length() > MAX_MERCHANT_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "가맹점명은 " + MAX_MERCHANT_NAME_LENGTH + "자를 넘을 수 없습니다.");
        }
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ExpenseErrorCode.AMOUNT_INVALID);
        }
    }

    private void validateMemo(String memo) {
        if (memo != null && memo.length() > MAX_MEMO_LENGTH) {
            throw new IllegalArgumentException("메모는 " + MAX_MEMO_LENGTH + "자를 넘을 수 없습니다.");
        }
    }

    // 지출은 워케이션 기간 안에서만 발생할 수 있다
    // 기간 밖 지출을 허용하면 워케이션 수정 시 기간 검증과 어긋난다
    private void validateSpentDate(LocalDate spentDate, WorkationVO workation) {

        if (spentDate == null) {
            throw new IllegalArgumentException("지출 일자를 입력해 주세요.");
        }
        if (spentDate.isBefore(workation.getStartDate()) || spentDate.isAfter(workation.getEndDate())) {
            throw new BusinessException(ExpenseErrorCode.SPENT_DATE_OUT_OF_PERIOD);
        }
    }

    private void validateCategory(Long userId, BudgetType budgetType, Long expenseCategoryId) {

        if (expenseCategoryId == null) {
            throw new IllegalArgumentException("카테고리를 선택해 주세요.");
        }
        if (expenseMapper.countValidCategory(budgetType, userId, expenseCategoryId) == 0) {
            throw new BusinessException(ExpenseErrorCode.CATEGORY_TYPE_MISMATCH);
        }
    }

    // 법인 지출은 사용 카드를 남겨야 증빙이 성립한다
    // 개인 지출은 카드를 지정해도 되고 현금이면 비워도 된다
    private void validateCard(Long userId, BudgetType budgetType, Long cardId) {

        if (budgetType == BudgetType.WORK && cardId == null) {
            throw new BusinessException(ExpenseErrorCode.CARD_REQUIRED);
        }
        if (cardId == null) {
            return;
        }

        String cardType = expenseMapper.selectOwnedCardType(cardId, userId);

        if (cardType == null) {
            throw new BusinessException(ExpenseErrorCode.CARD_NOT_FOUND);
        }
        if (budgetType == BudgetType.WORK && !BudgetType.WORK.name().equals(cardType)) {
            throw new BusinessException(ExpenseErrorCode.CARD_TYPE_MISMATCH);
        }
    }

    // 빈 문자열은 null 로 저장해 화면에서 값 없음 판정을 단순하게 한다
    private String normalize(String value) {
        return (value == null || value.trim().isEmpty()) ? null : value.trim();
    }
}
