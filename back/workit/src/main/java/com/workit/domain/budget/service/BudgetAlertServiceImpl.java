package com.workit.domain.budget.service;

import com.workit.domain.budget.mapper.BudgetAlertMapper;
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
// - 중복 알림 방지: notification_histories의 userId + referenceType + referenceId + notificationType을 활용한다
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
    private final BudgetAlertMapper budgetAlertMapper;
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
        String categoryName = resolveCategoryName(targetItem);

        log.debug("예산 사용률 확인 - budgetId={}, usageRate={}, spentAmount={}, targetAmount={}",
                budgetId, usageRate, spentAmount, targetAmount);

        // 3) 80% 소진 알림 (80% 이상이고, 아직 알림이 없으면)
        if (usageRate >= THRESHOLD_80_PERCENT) {
            createAlertIfNeeded(userId, budgetId, budgetType, categoryName,
                    usageRate, spentAmount, true);
        }

        // 4) 예산 초과 알림 (100% 초과이고, 아직 알림이 없으면)
        if (usageRate > THRESHOLD_EXCEEDED) {
            createAlertIfNeeded(userId, budgetId, budgetType, categoryName,
                    usageRate, spentAmount, false);
        }
    }

    /**
     * 알림 생성 여부를 판단하고, 필요시 NotificationCreateService를 통해 알림을 생성한다.
     *
     * @param userId        사용자 ID
     * @param budgetId      예산 배정 ID (referenceId로 사용)
     * @param budgetType    예산 유형
     * @param categoryName  카테고리 이름
     * @param usageRate     현재 사용률 (%)
     * @param spentAmount   현재 사용 금액
     * @param is80Percent   80% 알림 여부 (true: 80% 알림, false: 초과 알림)
     */
    private void createAlertIfNeeded(Long userId, Long budgetId, BudgetType budgetType,
                                      String categoryName, int usageRate,
                                      BigDecimal spentAmount, boolean is80Percent) {

        // notificationType 매핑: {budgetType}_{categoryCode}_{80_PERCENT|EXCEEDED}
        //notificationType을 먼저 계산하여 중복 판단 기준으로 사용한다
        String notificationType = resolveNotificationType(budgetType, categoryName, is80Percent);

        // 중복 알림 방지: 동일 예산(budgetId) + 동일 notificationType에 대해 이미 알림이 있으면 생성하지 않는다
        boolean alreadyExists = budgetAlertMapper.existsNotificationByReference(
                userId, REFERENCE_TYPE, budgetId, notificationType);

        if (alreadyExists) {
            log.debug("예산 알림 중복 - budgetId={}, notificationType={}", budgetId, notificationType);
            return;
        }

        // placeholder 구성
        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put("usageRate", usageRate);
        placeholders.put("amount", spentAmount.longValue());

        // 알림 생성 요청 구성
        NotificationCreateRequestDTO request = NotificationCreateRequestDTO.builder()
                .category(NotificationCategory.BUDGET_NOTIFY)
                .notificationType(notificationType)
                .important(is80Percent ? false : true)
                .placeholders(placeholders)
                .referenceType(REFERENCE_TYPE)
                .referenceId(budgetId)
                .build();

        // 공통 알림 생성 서비스 호출
        notificationCreateService.createNotification(userId, request);

        log.info("예산 알림 생성 - userId={}, budgetId={}, notificationType={}, usageRate={}, amount={}",
                userId, budgetId, notificationType, usageRate, spentAmount);
    }

    /**
     *预算类型和类别名称解析为通知类型字符串。
     * 映射逻辑基于expense_categories.code与notification_templates.notification_type的对应关系。
     *
     * WORK + 식비 → WORK_FOOD_80_PERCENT / WORK_FOOD_EXCEEDED
     * PERSONAL + 숙박비 → PERSONAL_ACCOMMODATION_80_PERCENT / PERSONAL_ACCOMMODATION_EXCEEDED
     *
     * @param budgetType    预算类型 (WORK / PERSONAL)
     * @param categoryName  类别名称 (DB中的名称)
     * @param is80Percent   是否80%告警 (true: 80%, false: 超额)
     * @return 通知类型字符串
     */
    private String resolveNotificationType(BudgetType budgetType, String categoryName,
                                            boolean is80Percent) {
        String suffix = is80Percent ? "_80_PERCENT" : "_EXCEEDED";
        String prefix = budgetType.name();
        String categoryCode = mapCategoryNameToCode(categoryName);
        return prefix + "_" + categoryCode + suffix;
    }

    /**
     *类别名称解析为DB中expense_categories.code。
     * 代码与名称的对应关系基于expense_categories种子数据。
     * 不直接使用code的原因是BudgetItemVO只提供categoryName。
     *
     * @param categoryName DB中的类别名称
     * @return expense_categories.code
     */
    private String mapCategoryNameToCode(String categoryName) {
        if (categoryName == null) {
            return "ETC";
        }

        //BudgetItemVO.categoryName是expense_categories.name的值
        // 根据种子数据: 숙박비→ACCOMMODATION, 교통비→TRANSPORTATION, 임차료→RENT,
        // 회의비→MEETING, 식비→FOOD, 기타→ETC, 통신비→COMMUNICATION, 소모품비→SUPPLIES,
        // 접대비→ENTERTAINMENT, 차량유지비→VEHICLE, 교육·도서비→EDUCATION, 보험료→INSURANCE,
        // 여가비→LEISURE, 쇼핑→SHOPPING, 카페·간식→CAFE, 모임비→GATHERING,
        // 건강·의료→HEALTH, 세탁·생활서비스→LAUNDRY
        switch (categoryName) {
            case "숙박비":     return "ACCOMMODATION";
            case "교통비":     return "TRANSPORTATION";
            case "임차료":     return "RENT";
            case "회의비":     return "MEETING";
            case "식비":       return "FOOD";
            case "기타":       return "ETC";
            case "통신비":     return "COMMUNICATION";
            case "소모품비":   return "SUPPLIES";
            case "접대비":     return "ENTERTAINMENT";
            case "차량유지비": return "VEHICLE";
            case "교육·도서비": return "EDUCATION";
            case "보험료":     return "INSURANCE";
            case "여가비":     return "LEISURE";
            case "쇼핑":       return "SHOPPING";
            case "카페·간식":  return "CAFE";
            case "모임비":     return "GATHERING";
            case "건강·의료":  return "HEALTH";
            case "세탁·생활서비스": return "LAUNDRY";
            default:           return "ETC";
        }
    }

    /**
     *BudgetItemVO에서 카테고리 이름을 추출한다.
     * customName(사용자 별칭)이 있으면 우선 사용하고, 없으면 categoryName(기본 이름)을 사용한다.
     *
     * @param item 예산 항목 VO
     * @return 표시할 카테고리 이름
     */
    private String resolveCategoryName(BudgetItemVO item) {
        if (item.getCustomName() != null && !item.getCustomName().isEmpty()) {
            return item.getCustomName();
        }
        return item.getCategoryName();
    }
}
