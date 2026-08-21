package com.workit.domain.budget.service;

import com.workit.domain.budget.mapper.BudgetMapper;
import com.workit.domain.budget.vo.BudgetItemVO;
import com.workit.domain.notification.dto.request.NotificationCreateRequestDTO;
import com.workit.domain.notification.enums.NotificationCategory;
import com.workit.domain.notification.service.NotificationCreateService;
import com.workit.domain.workation.vo.BudgetType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 예산 알림 판단 서비스 구현체
// - 지출이 추가·수정된 후 카테고리별 예산 사용률을 확인하여 80% / 초과 알림을 생성한다
// - NotificationCreateService를 통해 알림을 생성하며, 예산 도메인에서 직접 INSERT하지 않는다
// - 중복 알림 방지는 NotificationCreateServiceImpl.createNotification() 내부에서 원자적으로 처리한다
@Service
@RequiredArgsConstructor
@Slf4j
public class BudgetAlertServiceImpl implements BudgetAlertService {

    private static final String REFERENCE_TYPE = "BUDGET";

    /** 80% 소진 알림 기준 사용률 (정수) */
    private static final int THRESHOLD_80_PERCENT = 80;

    /** 예산 초과 기준 사용률 (정수) */
    private static final int THRESHOLD_EXCEEDED = 100;

    private final BudgetMapper budgetMapper;
    private final NotificationCreateService notificationCreateService;

    @Override
    public void checkAndNotifyBudgetAlert(Long userId, Long workationId,
                                           BudgetType budgetType, Long expenseCategoryId) {

        // 1) 해당 카테고리의 예산 배정 + 실제 지출 집계 조회
        List<BudgetItemVO> items = budgetMapper.selectBudgetItemList(
                workationId, userId, budgetType);

        BudgetItemVO targetItem = items.stream()
                .filter(item -> expenseCategoryId.equals(item.getExpenseCategoryId()))
                .findFirst()
                .orElse(null);

        // 예산 배정이 없으면 알림을 판단할 수 없다
        if (targetItem == null) {
            log.debug("예산 배정 미존재 - 알림 판단 스킵 workationId={}, budgetType={}, categoryId={}",
                    workationId, budgetType, expenseCategoryId);
            return;
        }

        // target_amount가 0이거나 null이면 알림 판단 불가
        BigDecimal targetAmount = targetItem.getTargetAmount();
        if (targetAmount == null || targetAmount.compareTo(BigDecimal.ZERO) <= 0) {
            log.debug("예산 배정 금액 0 - 알림 판단 스킵 budgetId={}", targetItem.getBudgetId());
            return;
        }

        BigDecimal spentAmount = targetItem.getSpentAmount();
        if (spentAmount == null) {
            spentAmount = BigDecimal.ZERO;
        }

        // 2) 사용률 계산 (정수 내림)
        //    예: 80.0% → 80, 79.9% → 79, 105.3% → 105
        int usageRate = spentAmount.multiply(BigDecimal.valueOf(100))
                .divide(targetAmount, 0, RoundingMode.FLOOR)
                .intValue();

        Long budgetId = targetItem.getBudgetId();
        String categoryCode = targetItem.getCategoryCode();

        log.debug("예산 사용률 확인 - budgetId={}, usageRate={}, spentAmount={}, targetAmount={}",
                budgetId, usageRate, spentAmount, targetAmount);

        // 3) 80% 소진 알림 (80% 이상이고, 아직 알림이 없으면)
        if (usageRate >= THRESHOLD_80_PERCENT) {
            createAndSendAlert(userId, budgetId, budgetType, categoryCode,
                    usageRate, spentAmount, true);
        }

        // 4) 예산 초과 알림 (100% 초과이고, 아직 알림이 없으면)
        if (usageRate > THRESHOLD_EXCEEDED) {
            createAndSendAlert(userId, budgetId, budgetType, categoryCode,
                    usageRate, spentAmount, false);
        }
    }

    /**
     * 예산 알림을 생성하고 NotificationCreateService를 통해 전달한다.
     * - 중복 알림 방지는 NotificationCreateServiceImpl 내부에서 처리한다.
     *
     * @param userId       사용자 ID
     * @param budgetId     예산 배정 ID (referenceId로 사용)
     * @param budgetType   예산 유형 (WORK / PERSONAL)
     * @param categoryCode 카테고리 코드 (expense_categories.code: FOOD, ACCOMMODATION 등)
     * @param usageRate    현재 사용률 (%)
     * @param spentAmount  현재 사용 금액
     * @param is80Percent  80% 알림 여부 (true: 80% 알림, false: 초과 알림)
     */
    private void createAndSendAlert(Long userId, Long budgetId, BudgetType budgetType,
                                     String categoryCode, int usageRate,
                                     BigDecimal spentAmount, boolean is80Percent) {

        String suffix = is80Percent ? "_80_PERCENT" : "_EXCEEDED";
        String notificationType = budgetType.name() + "_" + categoryCode + suffix;

        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put("usageRate", usageRate);
        placeholders.put("amount", spentAmount.longValue());

        NotificationCreateRequestDTO request = NotificationCreateRequestDTO.builder()
                .category(NotificationCategory.BUDGET_NOTIFY)
                .notificationType(notificationType)
                .important(!is80Percent)
                .placeholders(placeholders)
                .referenceType(REFERENCE_TYPE)
                .referenceId(budgetId)
                .build();

        notificationCreateService.createNotification(userId, request);

        log.info("예산 알림 생성 - userId={}, budgetId={}, notificationType={}, usageRate={}, amount={}",
                userId, budgetId, notificationType, usageRate, spentAmount);
    }
}
