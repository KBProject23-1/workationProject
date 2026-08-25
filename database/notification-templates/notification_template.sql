INSERT INTO `notification_templates`
(`category`, `notification_type`, `title_template`, `content_template`, `is_active`)
VALUES

-- =========================================================
-- WORK / 법인 예산
-- =========================================================

-- 숙박비
(
    'BUDGET_NOTIFY',
    'WORK_ACCOMMODATION_80_PERCENT',
    '법인 숙박비 예산 80% 소진',
    '법인 숙박비 예산의 {usageRate}%를 사용했어요. 현재 사용 금액은 {amount}원이에요.',
    1
),
(
    'BUDGET_NOTIFY',
    'WORK_ACCOMMODATION_EXCEEDED',
    '법인 숙박비 예산 초과',
    '법인 숙박비 예산을 {amount}원 초과했어요.',
    1
),

-- 교통비
(
    'BUDGET_NOTIFY',
    'WORK_TRANSPORTATION_80_PERCENT',
    '법인 교통비 예산 80% 소진',
    '법인 교통비 예산의 {usageRate}%를 사용했어요. 현재 사용 금액은 {amount}원이에요.',
    1
),
(
    'BUDGET_NOTIFY',
    'WORK_TRANSPORTATION_EXCEEDED',
    '법인 교통비 예산 초과',
    '법인 교통비 예산을 {amount}원 초과했어요.',
    1
),

-- 임차료
(
    'BUDGET_NOTIFY',
    'WORK_RENT_80_PERCENT',
    '법인 임차료 예산 80% 소진',
    '법인 임차료 예산의 {usageRate}%를 사용했어요. 현재 사용 금액은 {amount}원이에요.',
    1
),
(
    'BUDGET_NOTIFY',
    'WORK_RENT_EXCEEDED',
    '법인 임차료 예산 초과',
    '법인 임차료 예산을 {amount}원 초과했어요.',
    1
),

-- 회의비
(
    'BUDGET_NOTIFY',
    'WORK_MEETING_80_PERCENT',
    '법인 회의비 예산 80% 소진',
    '법인 회의비 예산의 {usageRate}%를 사용했어요. 현재 사용 금액은 {amount}원이에요.',
    1
),
(
    'BUDGET_NOTIFY',
    'WORK_MEETING_EXCEEDED',
    '법인 회의비 예산 초과',
    '법인 회의비 예산을 {amount}원 초과했어요.',
    1
),

-- 식비
(
    'BUDGET_NOTIFY',
    'WORK_FOOD_80_PERCENT',
    '법인 식비 예산 80% 소진',
    '법인 식비 예산의 {usageRate}%를 사용했어요. 현재 사용 금액은 {amount}원이에요.',
    1
),
(
    'BUDGET_NOTIFY',
    'WORK_FOOD_EXCEEDED',
    '법인 식비 예산 초과',
    '법인 식비 예산을 {amount}원 초과했어요.',
    1
),

-- 기타
(
    'BUDGET_NOTIFY',
    'WORK_ETC_80_PERCENT',
    '법인 기타 예산 80% 소진',
    '법인 기타 예산의 {usageRate}%를 사용했어요. 현재 사용 금액은 {amount}원이에요.',
    1
),
(
    'BUDGET_NOTIFY',
    'WORK_ETC_EXCEEDED',
    '법인 기타 예산 초과',
    '법인 기타 예산을 {amount}원 초과했어요.',
    1
),

-- 통신비
(
    'BUDGET_NOTIFY',
    'WORK_COMMUNICATION_80_PERCENT',
    '법인 통신비 예산 80% 소진',
    '법인 통신비 예산의 {usageRate}%를 사용했어요. 현재 사용 금액은 {amount}원이에요.',
    1
),
(
    'BUDGET_NOTIFY',
    'WORK_COMMUNICATION_EXCEEDED',
    '법인 통신비 예산 초과',
    '법인 통신비 예산을 {amount}원 초과했어요.',
    1
),

-- 소모품비
(
    'BUDGET_NOTIFY',
    'WORK_SUPPLIES_80_PERCENT',
    '법인 소모품비 예산 80% 소진',
    '법인 소모품비 예산의 {usageRate}%를 사용했어요. 현재 사용 금액은 {amount}원이에요.',
    1
),
(
    'BUDGET_NOTIFY',
    'WORK_SUPPLIES_EXCEEDED',
    '법인 소모품비 예산 초과',
    '법인 소모품비 예산을 {amount}원 초과했어요.',
    1
),

-- 접대비
(
    'BUDGET_NOTIFY',
    'WORK_ENTERTAINMENT_80_PERCENT',
    '법인 접대비 예산 80% 소진',
    '법인 접대비 예산의 {usageRate}%를 사용했어요. 현재 사용 금액은 {amount}원이에요.',
    1
),
(
    'BUDGET_NOTIFY',
    'WORK_ENTERTAINMENT_EXCEEDED',
    '법인 접대비 예산 초과',
    '법인 접대비 예산을 {amount}원 초과했어요.',
    1
),

-- 차량유지비
(
    'BUDGET_NOTIFY',
    'WORK_VEHICLE_80_PERCENT',
    '법인 차량유지비 예산 80% 소진',
    '법인 차량유지비 예산의 {usageRate}%를 사용했어요. 현재 사용 금액은 {amount}원이에요.',
    1
),
(
    'BUDGET_NOTIFY',
    'WORK_VEHICLE_EXCEEDED',
    '법인 차량유지비 예산 초과',
    '법인 차량유지비 예산을 {amount}원 초과했어요.',
    1
),

-- 교육·도서비
(
    'BUDGET_NOTIFY',
    'WORK_EDUCATION_80_PERCENT',
    '법인 교육·도서비 예산 80% 소진',
    '법인 교육·도서비 예산의 {usageRate}%를 사용했어요. 현재 사용 금액은 {amount}원이에요.',
    1
),
(
    'BUDGET_NOTIFY',
    'WORK_EDUCATION_EXCEEDED',
    '법인 교육·도서비 예산 초과',
    '법인 교육·도서비 예산을 {amount}원 초과했어요.',
    1
),

-- 보험료
(
    'BUDGET_NOTIFY',
    'WORK_INSURANCE_80_PERCENT',
    '법인 보험료 예산 80% 소진',
    '법인 보험료 예산의 {usageRate}%를 사용했어요. 현재 사용 금액은 {amount}원이에요.',
    1
),
(
    'BUDGET_NOTIFY',
    'WORK_INSURANCE_EXCEEDED',
    '법인 보험료 예산 초과',
    '법인 보험료 예산을 {amount}원 초과했어요.',
    1
),

-- =========================================================
-- PERSONAL / 개인 예산
-- =========================================================

-- 숙박비
(
    'BUDGET_NOTIFY',
    'PERSONAL_ACCOMMODATION_80_PERCENT',
    '개인 숙박비 예산 80% 소진',
    '개인 숙박비 예산의 {usageRate}%를 사용했어요. 현재 사용 금액은 {amount}원이에요.',
    1
),
(
    'BUDGET_NOTIFY',
    'PERSONAL_ACCOMMODATION_EXCEEDED',
    '개인 숙박비 예산 초과',
    '개인 숙박비 예산을 {amount}원 초과했어요.',
    1
),

-- 교통비
(
    'BUDGET_NOTIFY',
    'PERSONAL_TRANSPORTATION_80_PERCENT',
    '개인 교통비 예산 80% 소진',
    '개인 교통비 예산의 {usageRate}%를 사용했어요. 현재 사용 금액은 {amount}원이에요.',
    1
),
(
    'BUDGET_NOTIFY',
    'PERSONAL_TRANSPORTATION_EXCEEDED',
    '개인 교통비 예산 초과',
    '개인 교통비 예산을 {amount}원 초과했어요.',
    1
),

-- 식비
(
    'BUDGET_NOTIFY',
    'PERSONAL_FOOD_80_PERCENT',
    '개인 식비 예산 80% 소진',
    '개인 식비 예산의 {usageRate}%를 사용했어요. 현재 사용 금액은 {amount}원이에요.',
    1
),
(
    'BUDGET_NOTIFY',
    'PERSONAL_FOOD_EXCEEDED',
    '개인 식비 예산 초과',
    '개인 식비 예산을 {amount}원 초과했어요.',
    1
),

-- 여가비
(
    'BUDGET_NOTIFY',
    'PERSONAL_LEISURE_80_PERCENT',
    '개인 여가비 예산 80% 소진',
    '개인 여가비 예산의 {usageRate}%를 사용했어요. 현재 사용 금액은 {amount}원이에요.',
    1
),
(
    'BUDGET_NOTIFY',
    'PERSONAL_LEISURE_EXCEEDED',
    '개인 여가비 예산 초과',
    '개인 여가비 예산을 {amount}원 초과했어요.',
    1
),

-- 쇼핑
(
    'BUDGET_NOTIFY',
    'PERSONAL_SHOPPING_80_PERCENT',
    '개인 쇼핑 예산 80% 소진',
    '개인 쇼핑 예산의 {usageRate}%를 사용했어요. 현재 사용 금액은 {amount}원이에요.',
    1
),
(
    'BUDGET_NOTIFY',
    'PERSONAL_SHOPPING_EXCEEDED',
    '개인 쇼핑 예산 초과',
    '개인 쇼핑 예산을 {amount}원 초과했어요.',
    1
),

-- 기타
(
    'BUDGET_NOTIFY',
    'PERSONAL_ETC_80_PERCENT',
    '개인 기타 예산 80% 소진',
    '개인 기타 예산의 {usageRate}%를 사용했어요. 현재 사용 금액은 {amount}원이에요.',
    1
),
(
    'BUDGET_NOTIFY',
    'PERSONAL_ETC_EXCEEDED',
    '개인 기타 예산 초과',
    '개인 기타 예산을 {amount}원 초과했어요.',
    1
),

-- 카페·간식
(
    'BUDGET_NOTIFY',
    'PERSONAL_CAFE_80_PERCENT',
    '개인 카페·간식 예산 80% 소진',
    '개인 카페·간식 예산의 {usageRate}%를 사용했어요. 현재 사용 금액은 {amount}원이에요.',
    1
),
(
    'BUDGET_NOTIFY',
    'PERSONAL_CAFE_EXCEEDED',
    '개인 카페·간식 예산 초과',
    '개인 카페·간식 예산을 {amount}원 초과했어요.',
    1
),

-- 모임비
(
    'BUDGET_NOTIFY',
    'PERSONAL_GATHERING_80_PERCENT',
    '개인 모임비 예산 80% 소진',
    '개인 모임비 예산의 {usageRate}%를 사용했어요. 현재 사용 금액은 {amount}원이에요.',
    1
),
(
    'BUDGET_NOTIFY',
    'PERSONAL_GATHERING_EXCEEDED',
    '개인 모임비 예산 초과',
    '개인 모임비 예산을 {amount}원 초과했어요.',
    1
),

-- 건강·의료
(
    'BUDGET_NOTIFY',
    'PERSONAL_HEALTH_80_PERCENT',
    '개인 건강·의료 예산 80% 소진',
    '개인 건강·의료 예산의 {usageRate}%를 사용했어요. 현재 사용 금액은 {amount}원이에요.',
    1
),
(
    'BUDGET_NOTIFY',
    'PERSONAL_HEALTH_EXCEEDED',
    '개인 건강·의료 예산 초과',
    '개인 건강·의료 예산을 {amount}원 초과했어요.',
    1
),

-- 세탁·생활서비스
(
    'BUDGET_NOTIFY',
    'PERSONAL_LAUNDRY_80_PERCENT',
    '개인 세탁·생활서비스 예산 80% 소진',
    '개인 세탁·생활서비스 예산의 {usageRate}%를 사용했어요. 현재 사용 금액은 {amount}원이에요.',
    1
),
(
    'BUDGET_NOTIFY',
    'PERSONAL_LAUNDRY_EXCEEDED',
    '개인 세탁·생활서비스 예산 초과',
    '개인 세탁·생활서비스 예산을 {amount}원 초과했어요.',
    1
),

-- 개인 통신비
(
    'BUDGET_NOTIFY',
    'PERSONAL_COMMUNICATION_80_PERCENT',
    '개인 통신비 예산 80% 소진',
    '개인 통신비 예산의 {usageRate}%를 사용했어요. 현재 사용 금액은 {amount}원이에요.',
    1
),
(
    'BUDGET_NOTIFY',
    'PERSONAL_COMMUNICATION_EXCEEDED',
    '개인 통신비 예산 초과',
    '개인 통신비 예산을 {amount}원 초과했어요.',
    1
),

-- =========================================================
-- 워케이션 알림
-- =========================================================
(
    'WORKATION_NOTIFY',
    'WORKATION_START_D_MINUS_1',
    '워케이션 시작 예정',
    '내일 {date}에 워케이션이 시작됩니다. 일정을 확인해 주세요.',
    1
),
(
    'WORKATION_NOTIFY',
    'WORKATION_END_D_MINUS_1',
    '워케이션 종료 예정',
    '내일 {date}에 워케이션이 종료됩니다. 정산 항목을 확인해 주세요.',
    1
),

-- =========================================================
-- 정산 알림
-- =========================================================
(
    'SETTLEMENT_NOTIFY',
    'SETTLEMENT_OVERDUE',
    '정산이 지연되고 있어요',
    '확인하지 않은 지출이 {unconfirmedCount}건 있어요. 확인 후 정산을 완료해 주세요.',
    1
),
(
    'SETTLEMENT_NOTIFY',
    'UNCONFIRMED_EXPENSE_OVER_3',
    '확인하지 않은 지출이 있어요',
    '확인하지 않은 자동 분류 지출이 {unconfirmedCount}건 있습니다. 지출 내역을 확인해 주세요.',
    1
),

-- =========================================================
-- 일정 알림
-- =========================================================
(
    'SCHEDULE_NOTIFY',
    'SCHEDULE_D_MINUS_1_HOUR',
    '일정이 1시간 후 시작돼요',
    '{scheduleTitle} 일정이 {date}에 시작됩니다.',
    1
),

-- =========================================================
-- 입출금 알림
-- =========================================================
(
    'TRANSFER_NOTIFY',
    'WALLET_CHARGE_SUCCESS',
    '지갑 충전 완료',
    '{amount}원이 지갑에 충전되었습니다.',
    1
),
(
    'TRANSFER_NOTIFY',
    'ACCOUNT_REFUND_SUCCESS',
    '계좌 환불 완료',
    '{amount}원이 계좌로 환불되었습니다.',
    1
),

-- =========================================================
-- 결제 알림
-- =========================================================
(
    'PAYMENT_NOTIFY',
    'PAYMENT_SUCCESS',
    '결제 완료',
    '{merchant}에서 {amount}원 결제가 완료되었습니다.',
    1
),
(
    'PAYMENT_NOTIFY',
    'REFUND_SUCCESS',
    '환불 완료',
    '{merchant} 결제 건이 환불되었습니다. 환불 금액은 {amount}원입니다.',
    1
);
