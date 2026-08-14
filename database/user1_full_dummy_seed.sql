-- 사용자 1 기준 핵심 시드(요청 데이터 우선 반영)
-- 재실행 가능: 중복 시 기존 값은 덮어쓰기/유지하여 필요한 데이터만 정합

USE workit;
SET NAMES utf8mb4;

SET @USER_ID := 1;
SET @WORKATION_ID := 10;

-- -----------------------------------------------------------------------------
-- region (요청 고정)
-- -----------------------------------------------------------------------------
INSERT INTO region (id, name, created_at)
VALUES
    (1, '부산', '2026-08-11 17:07:59'),
    (2, '강릉', '2026-08-11 17:07:59'),
    (3, '여수', '2026-08-11 17:07:59'),
    (4, '제주', '2026-08-11 17:07:59')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    created_at = VALUES(created_at);

-- -----------------------------------------------------------------------------
-- expense_categories (요청 고정)
-- -----------------------------------------------------------------------------
INSERT INTO expense_categories
    (id, user_id, budget_type, code, name, description, is_default, is_deletable, sort_order, created_at)
VALUES
    (1, NULL, 'WORK', 'ACCOMMODATION', '숙박비', '호텔·에어비앤비 등 숙소 요금', 1, 1, 1, '2026-08-11 17:07:59'),
    (2, NULL, 'WORK', 'TRANSPORTATION', '교통비', '항공·철도·버스·현지 이동', 1, 1, 2, '2026-08-11 17:07:59'),
    (3, NULL, 'WORK', 'RENT', '임차료', '공유오피스·회의실 대여 등', 1, 1, 3, '2026-08-11 17:07:59'),
    (4, NULL, 'WORK', 'MEETING', '회의비', '업무 미팅 중 식음료·다과', 1, 1, 4, '2026-08-11 17:07:59'),
    (5, NULL, 'WORK', 'FOOD', '식비', '근무일 식대', 1, 1, 5, '2026-08-11 17:07:59'),
    (6, NULL, 'WORK', 'ETC', '기타', '위 항목에 없는 지출', 1, 0, 6, '2026-08-11 17:07:59'),
    (7, NULL, 'WORK', 'COMMUNICATION', '통신비', '데이터·와이파이 등 업무 통신', 0, 1, 7, '2026-08-11 17:07:59'),
    (8, NULL, 'WORK', 'SUPPLIES', '소모품비', '업무용 소모품 구입', 0, 1, 8, '2026-08-11 17:07:59'),
    (9, NULL, 'WORK', 'ENTERTAINMENT', '접대비', '거래처 접대 비용', 0, 1, 9, '2026-08-11 17:07:59'),
    (10, NULL, 'WORK', 'VEHICLE', '차량유지비', '렌터카·주유·주차 등', 0, 1, 10, '2026-08-11 17:07:59'),
    (11, NULL, 'WORK', 'EDUCATION', '교육·도서비', '업무 관련 교육·도서 구입', 0, 1, 11, '2026-08-11 17:07:59'),
    (12, NULL, 'WORK', 'INSURANCE', '보험료', '여행자보험 등', 0, 1, 12, '2026-08-11 17:07:59'),
    (13, NULL, 'PERSONAL', 'ACCOMMODATION', '숙박비', '개인 부담 숙소 요금', 1, 1, 1, '2026-08-11 17:07:59'),
    (14, NULL, 'PERSONAL', 'TRANSPORTATION', '교통비', '개인 이동 비용', 1, 1, 2, '2026-08-11 17:07:59'),
    (15, NULL, 'PERSONAL', 'FOOD', '식비', '식사 비용', 1, 1, 3, '2026-08-11 17:07:59'),
    (16, NULL, 'PERSONAL', 'LEISURE', '여가비', '관광·액티비티·문화생활', 1, 1, 4, '2026-08-11 17:07:59'),
    (17, NULL, 'PERSONAL', 'SHOPPING', '쇼핑', '기념품·의류 등 구매', 1, 1, 5, '2026-08-11 17:07:59'),
    (18, NULL, 'PERSONAL', 'ETC', '기타', '위 항목에 없는 지출', 1, 0, 6, '2026-08-11 17:07:59'),
    (19, NULL, 'PERSONAL', 'CAFE', '카페·간식', '커피·디저트·간식', 0, 1, 7, '2026-08-11 17:07:59'),
    (20, NULL, 'PERSONAL', 'GATHERING', '모임비', '지인·동료와의 모임 비용', 0, 1, 8, '2026-08-11 17:07:59'),
    (21, NULL, 'PERSONAL', 'HEALTH', '건강·의료', '약국·병원·운동', 0, 1, 9, '2026-08-11 17:07:59'),
    (22, NULL, 'PERSONAL', 'LAUNDRY', '세탁·생활서비스', '세탁·생활 편의 서비스', 0, 1, 10, '2026-08-11 17:07:59'),
    (23, NULL, 'PERSONAL', 'COMMUNICATION', '통신비', '개인 데이터·로밍', 0, 1, 11, '2026-08-11 17:07:59')
ON DUPLICATE KEY UPDATE
    user_id = VALUES(user_id),
    budget_type = VALUES(budget_type),
    code = VALUES(code),
    name = VALUES(name),
    description = VALUES(description),
    is_default = VALUES(is_default),
    is_deletable = VALUES(is_deletable),
    sort_order = VALUES(sort_order),
    created_at = VALUES(created_at);

-- -----------------------------------------------------------------------------
-- merchant_category_mappings (요청 고정)
-- -----------------------------------------------------------------------------
INSERT INTO merchant_category_mappings (id, budget_type, merchant_category, expense_category_id, created_at)
VALUES
    (1, 'WORK', 'ACCOMMODATION', 1, '2026-08-11 17:07:59'),
    (2, 'WORK', 'TRANSPORT', 2, '2026-08-11 17:07:59'),
    (3, 'WORK', 'OFFICE', 3, '2026-08-11 17:07:59'),
    (4, 'WORK', 'RESTAURANT', 5, '2026-08-11 17:07:59'),
    (5, 'PERSONAL', 'ACCOMMODATION', 13, '2026-08-11 17:07:59'),
    (6, 'PERSONAL', 'TRANSPORT', 14, '2026-08-11 17:07:59'),
    (7, 'PERSONAL', 'RESTAURANT', 15, '2026-08-11 17:07:59'),
    (8, 'PERSONAL', 'CAFE', 19, '2026-08-11 17:07:59'),
    (9, 'PERSONAL', 'ACTIVITY', 16, '2026-08-11 17:07:59')
ON DUPLICATE KEY UPDATE
    budget_type = VALUES(budget_type),
    merchant_category = VALUES(merchant_category),
    expense_category_id = VALUES(expense_category_id),
    created_at = VALUES(created_at);

-- -----------------------------------------------------------------------------
-- users / user_auth / user_profile / terms / user_terms_agreements
-- -----------------------------------------------------------------------------
INSERT INTO users (
    id, email_hash, email_encrypt, name_encrypt, phone_number_hash, phone_number_encrypt,
    status, created_at, updated_at, deleted_at
)
VALUES (
    1,
    'f660ab912ec121d1b1e928a0bb4bc61b15f5ad44d5efdc4e1c92a25e99b8e44a',
    'LqaqE2cdZxR07urvReSz0u3S7gdruQ6uieZVCs++VykP/1xt5qOVgPE=',
    'ONE8P0r7NhJotulELqAJhTkwFFb6FU+HOCuyp/VCYmSetyyOzQ==',
    'fbdae8fe9734d0d2beeb9075af74eb95e97a4d61e45b78846cf872fae84a21d8',
    'qD/zyKwTh12fgbVebGCAgpRoybP+tigQoh5xHiXBgF15v6xRPEY/',
    'ACTIVE',
    '2026-08-11 17:11:54',
    '2026-08-11 17:11:54',
    '2026-08-11 17:11:54'
)
ON DUPLICATE KEY UPDATE
    email_hash = VALUES(email_hash),
    email_encrypt = VALUES(email_encrypt),
    name_encrypt = VALUES(name_encrypt),
    phone_number_hash = VALUES(phone_number_hash),
    phone_number_encrypt = VALUES(phone_number_encrypt),
    status = VALUES(status),
    updated_at = VALUES(updated_at),
    deleted_at = VALUES(deleted_at);

INSERT INTO user_auth (
    id, user_id, password_hash, identity_ci_hash, identity_ci_encrypt, created_at, updated_at
) VALUES (
    1,
    1,
    '$2a$10$LOTfFOXmoosja8P0cE5t0Ooq7zGQfx4gH/lq9xUHatSWDpPKEWIfu',
    '00bf70d5ab5eef1c07bcd1a41bd1cb89a716706eee7b80892bd87235e740eea4',
    'OrDTaxOJvP14TAStApRG7SpDu9oXiKUwQZ++BKDNHzKolJtig5ValE0=',
    '2026-08-11 17:11:54',
    '2026-08-11 17:11:54'
)
ON DUPLICATE KEY UPDATE
    password_hash = VALUES(password_hash),
    identity_ci_hash = VALUES(identity_ci_hash),
    identity_ci_encrypt = VALUES(identity_ci_encrypt),
    updated_at = VALUES(updated_at);

INSERT INTO user_profile (id, user_id, nickname, company_name, created_at, updated_at)
VALUES (1, 1, 'devtest1', NULL, '2026-08-11 17:11:54', '2026-08-11 17:11:54')
ON DUPLICATE KEY UPDATE
    nickname = VALUES(nickname),
    company_name = VALUES(company_name),
    updated_at = VALUES(updated_at);

INSERT INTO terms (id, title, content, required, created_at)
VALUES
    (1, 'WorkIt 전자금융거래 이용약관', '"        제1조(목적)\n\n        본 약관은 WorkIt(이하 ""회사"")가 제공하는 전자지갑 및 전자금융거래 서비스의 이용과 관련하여 회사와 회원 간의 권리, 의무 및 책임사항을 규정함을 목적으로 합니다.\n\n        제2조(정의)\n\n        ① ""전자금융거래""란 회사가 제공하는 전자적 장치를 이용하여 회원이 금융상품 또는 지급수단을 이용하는 거래를 말합니다.\n\n        ② ""전자지갑""이란 회원이 계좌 또는 카드를 등록하고 포인트를 충전하여 사용할 수 있는 서비스를 말합니다.\n\n        ③ ""WorkIt 포인트""란 회원이 충전하거나 적립하여 결제에 사용할 수 있는 선불전자지급수단을 말합니다.\n\n        ④ ""결제수단""이란 등록된 계좌, 신용카드 또는 체크카드를 말합니다.\n\n        ⑤ ""가맹점""이란 회사와 계약을 체결하여 WorkIt 결제를 제공하는 사업자를 말합니다.\n\n        제3조(서비스의 종류)\n\n        회사는 다음 서비스를 제공합니다.\n\n        1. 계좌 등록\n\n        2. 카드 등록\n\n        3. WorkIt 포인트 충전\n\n        4. 포인트 결제\n\n        5. 카드 간편결제\n\n        6. 거래내역 조회\n\n        7. 거래 취소\n\n        8. 환불\n\n        9. 기타 회사가 제공하는 전자금융 서비스\n\n        제4조(전자금융거래 이용)\n\n        ① 회원은 본인 명의의 계좌와 카드만 등록할 수 있습니다.\n\n        ② 회사는 본인확인을 위해 인증 절차를 요구할 수 있습니다.\n\n        ③ 회사는 안전한 거래를 위해 추가 인증을 요청할 수 있습니다.\n\n        제5조(포인트 충전)\n\n        ① 회원은 회사가 지정한 계좌를 이용하여 포인트를 충전할 수 있습니다.\n\n        ② 충전된 포인트는 가맹점에서 사용할 수 있습니다.\n\n        ③ 충전 가능 금액 및 보유 한도는 회사 정책에 따릅니다.\n\n        제6조(결제)\n\n        ① 회원은 포인트 또는 등록된 카드로 결제할 수 있습니다.\n\n        ② 결제가 완료되면 거래내역이 생성됩니다.\n\n        ③ 결제 오류 발생 시 회사는 확인 후 조치합니다.\n\n        제7조(결제취소 및 환불)\n\n        ① 회원은 가맹점의 환불 정책에 따라 결제를 취소할 수 있습니다.\n\n        ② 결제가 취소되면 사용된 포인트 또는 카드 승인금액은 원래의 결제수단으로 반환됩니다.\n\n        ③ 환불 처리 기간은 금융기관의 정책에 따라 달라질 수 있습니다.\n\n        제8조(회원의 의무)\n\n        회원은 다음 사항을 준수하여야 합니다.\n\n        1. 본인 명의의 계좌만 등록할 것\n\n        2. 본인 명의의 카드만 등록할 것\n\n        3. 비밀번호 및 인증정보를 타인에게 제공하지 않을 것\n\n        4. 부정한 거래를 시도하지 않을 것\n\n        5. 타인의 결제수단을 무단으로 사용하지 않을 것\n\n        6. 사고 발생 시 즉시 회사에 신고할 것\n\n        제9조(회사의 의무)\n\n        ① 회사는 안정적인 전자금융서비스를 제공합니다.\n\n        ② 회사는 회원의 금융정보를 안전하게 보호합니다.\n\n        ③ 회사는 거래기록을 관련 법령에 따라 보관합니다.\n\n        ④ 회사는 장애 발생 시 신속하게 복구하도록 노력합니다.\n\n        제10조(이용 제한)\n\n        회사는 다음의 경우 서비스 이용을 제한할 수 있습니다.\n\n        1. 본 약관을 위반한 경우\n\n        2. 부정결제가 의심되는 경우\n\n        3. 명의도용이 의심되는 경우\n\n        4. 법령 위반이 확인된 경우\n\n        5. 시스템 점검이 필요한 경우\n\n        제11조(거래기록)\n\n        ① 회사는 전자금융거래 기록을 관련 법령에 따라 보관합니다.\n\n        ② 회원은 거래내역을 서비스에서 조회할 수 있습니다.\n\n        제12조(오류의 정정)\n\n        회원은 거래 오류를 발견한 경우 회사에 정정을 요청할 수 있으며 회사는 확인 후 필요한 조치를 합니다.\n\n        제13조(개인정보 보호)\n\n        회사는 전자금융거래 과정에서 수집된 개인정보를 관련 법령 및 개인정보처리방침에 따라 안전하게 관리합니다.\n\n        제14조(손해배상)\n\n        ① 회사의 고의 또는 과실로 회원에게 손해가 발생한 경우 관련 법령에 따라 배상합니다.\n\n        ② 회원의 고의 또는 과실로 발생한 손해는 회원이 부담합니다.\n\n        제15조(면책)\n\n        회사는 다음 각 호의 경우 책임을 지지 않습니다.\n\n        1. 회원의 인증정보 관리 소홀\n\n        2. 천재지변\n\n        3. 통신 장애\n\n        4. 금융기관 시스템 장애\n\n        5. 회원의 귀책사유\n\n        제16조(약관 변경)\n\n        회사는 관련 법령을 위반하지 않는 범위에서 본 약관을 변경할 수 있으며 변경사항은 서비스 내 공지합니다.\n\n        제17조(준거법)\n\n        본 약관은 대한민국 법률을 따르며 분쟁은 대한민국 법원의 관할로 합니다.\n\n        부칙\n\n        본 약관은 2026년 7월 30일부터 시행합니다.\n        '
, 1, '2026-08-11 17:11:24'),
    (2, '마케팅 정보 수신 동의', '제1조 (목적)\n\n회사는 이용자에게 워케이션 관련 혜택 및 서비스 정보를 제공하기 위해 마케팅 정보를 발송할 수 있습니다.\n\n\n제2조 (수집 및 이용 항목)\n\n회사는 다음 정보를 활용합니다.\n\n- 이름\n- 연락처\n- 이메일 주소\n- 서비스 이용 기록\n- 관심 지역 및 이용 선호 정보\n\n\n제3조 (마케팅 활용 목적)\n\n회사는 다음 목적을 위해 정보를 이용합니다.\n\n1. 신규 서비스 안내\n2. 할인 및 프로모션 제공\n3. 워케이션 상품 추천\n4. 숙소 및 공유오피스 추천\n5. 이벤트 및 혜택 안내\n\n\n제4조 (전송 방법)\n\n마케팅 정보는 다음 방법으로 제공될 수 있습니다.\n\n- 앱 푸시 알림\n- 문자 메시지\n- 이메일\n- 카카오 알림톡\n\n\n제5조 (동의 철회)\n\n이용자는 언제든지 마케팅 정보 수신 동의를 철회할 수 있습니다.\n\n동의 철회 이후에는 마케팅 목적의 정보 제공을 중단합니다.\n', 1, '2026-08-11 17:11:28'),
    (3, '개인정보 수집 및 이용 동의', '제1조 (목적)\n\n워크잇(이하 ""회사"")은 이용자의 개인정보를 보호하고 관련 법령을 준수하기 위해 개인정보 수집 및 이용에 관한 사항을 안내합니다.\n\n\n제2조 (수집하는 개인정보 항목)\n\n회사는 서비스 제공을 위해 다음 개인정보를 수집합니다.\n\n1. 회원가입 및 계정 관리\n\n- 이름\n- 이메일 주소\n- 휴대전화번호\n- 로그인 정보\n- 프로필 정보\n\n\n2. 전자지갑 및 결제 서비스 이용\n\n- 계좌 연결 정보\n- 카드 등록 정보\n- 결제 승인 정보\n- 충전 및 거래 기록\n- 포인트 이용 내역\n\n\n3. 워케이션 서비스 이용\n\n- 예약 정보\n- 숙박 및 공유오피스 이용 정보\n- 후기 작성 정보\n- 선호 지역 및 이용 패턴\n\n\n4. 자동 수집 정보\n\n- 서비스 이용 기록\n- 접속 로그\n- IP 주소\n- 기기 정보\n- 앱 이용 기록\n\n\n제3조 (개인정보 수집 및 이용 목적)\n\n회사는 수집한 개인정보를 다음 목적을 위해 이용합니다.\n\n1. 회원 관리 및 본인 확인\n2. 전자금융거래 서비스 제공\n3. 결제 및 포인트 충전 처리\n4. 숙소 및 공유오피스 예약 서비스 제공\n5. 워케이션 추천 서비스 제공\n6. 예산 관리 및 소비 분석 서비스 제공\n7. 고객 문의 대응 및 서비스 개선\n\n\n제4조 (개인정보 보유 및 이용 기간)\n\n회사는 개인정보 수집 및 이용 목적이 달성된 후에는 해당 정보를 지체 없이 파기합니다.\n\n단, 관계 법령에 따라 일정 기간 보관이 필요한 경우 해당 기간 동안 보관합니다.\n\n- 전자금융거래 기록: 5년\n- 계약 및 결제 관련 기록: 5년\n- 소비자 불만 및 분쟁 처리 기록: 3년\n\n\n제5조 (동의 거부 권리)\n\n이용자는 개인정보 수집 및 이용에 대한 동의를 거부할 권리가 있습니다.\n\n다만 필수 개인정보 수집 및 이용에 동의하지 않을 경우 서비스 이용이 제한될 수 있습니다.\n', 1, '2026-08-11 17:11:31'),
    (4, '개인정보 제3자 제공 동의', '제1조 (목적)\n\n회사로 이용자의 편리한 서비스 이용을 위해 필요한 경우 개인정보를 제3자에게 제공합니다.\n\n\n제2조 (개인정보 제공 대상)\n\n회사는 다음 업체에게 개인정보를 제공할 수 있습니다.\n\n1. 숙소 예약 서비스 제공 업체\n\n제공 항목:\n- 이름\n- 연락처\n- 예약 정보\n\n제공 목적:\n- 숙소 예약 처리\n- 예약 확인 및 고객 지원\n\n2. 공유오피스 운영 업체\n\n제공 항목:\n- 이름\n- 연락처\n- 예약 정보\n\n제공 목적:\n- 공간 예약 및 이용 관리\n\n3. 결제 및 금융 서비스 제공 업체\n\n제공 항목:\n- 결제 정보\n- 거래 정보\n\n제공 목적:\n- 결제 승인\n- 충전 및 환불 처리\n\n4. 워케이션 바우처 제공 기관\n\n제공 항목:\n- 이름\n- 이용 정보\n\n제공 목적:\n- 바우처 사용 확인 및 서비스 제공\n\n\n제3조 (개인정보 제공 기간)\n\n개인정보는 제공 목적 달성 후 즉시 파기하거나 해당 업체의 개인정보 처리방침에 따라 관리됩니다.\n\n\n제4조 (동의 거부)\n\n이용자는 개인정보 제3자 제공에 대한 동의를 거부할 수 있습니다.\n\n다만 해당 서비스 이용을 위해 필요한 경우 일부 기능 이용이 제한될 수 있습니다.\n', 0, '2026-08-11 17:11:34'),
    (5, 'WorkIt 서비스 이용약관', '제1조(목적)\n\n본 약관은 WorkIt(이하 ""회사"")가 제공하는 전자지갑 및 워케이션 플랫폼 서비스의 이용과 관련하여 회사와 회원의 권리, 의무 및 책임사항을 규정함을 목적으로 합니다.\n\n제2조(정의)\n\n① ""WorkIt 서비스""란 회사가 제공하는 모든 서비스를 말합니다.\n\n② ""회원""이란 본 약관에 동의하고 서비스를 이용하는 자를 말합니다.\n\n③ ""전자지갑""이란 회원이 계좌를 연결하거나 포인트를 충전하여 결제에 사용할 수 있는 서비스를 말합니다.\n\n④ ""WorkIt 포인트""란 회원이 충전하거나 적립하여 사용할 수 있는 전자적 지급수단을 말합니다.\n\n⑤ ""가맹점""이란 WorkIt와 계약을 체결하여 결제를 지원하는 사업자를 말합니다.\n\n⑥ ""워케이션 서비스""란 숙소 예약, 공유오피스 예약, 추천 서비스 등을 말합니다.\n\n⑦ ""바우처""란 정부 또는 제휴기관이 제공하는 워케이션 지원금을 말합니다.\n\n제3조(약관의 효력)\n\n① 본 약관은 회원가입 시 동의함으로써 효력이 발생합니다.\n\n② 회사는 관련 법령을 위반하지 않는 범위에서 본 약관을 변경할 수 있습니다.\n\n③ 변경 시 최소 7일 전에 공지하며 중요한 변경은 30일 전에 공지합니다.\n\n제4조(회원가입)\n\n① 회원은 회사가 정한 절차에 따라 가입할 수 있습니다.\n\n② 허위 정보를 입력한 경우 회사는 가입을 거부하거나 이용을 제한할 수 있습니다.\n\n제5조(서비스 내용)\n\n회사는 다음 서비스를 제공합니다.\n\n1. 전자지갑 서비스\n\n2. 계좌 충전\n\n3. 카드 등록 및 간편결제\n\n4. WorkIt 포인트 충전 및 사용\n\n5. 숙소 예약\n\n6. 공유오피스 예약\n\n7. 워케이션 바우처 사용\n\n8. AI 기반 숙소 추천\n\n9. AI 기반 공유오피스 추천\n\n10. 예산 관리 서비스\n\n11. 여행 후기 작성 및 조회\n\n12. 기타 회사가 제공하는 서비스\n\n제6조(포인트)\n\n① 회원은 계좌를 통해 포인트를 충전할 수 있습니다.\n\n② 포인트는 가맹점에서 결제에 사용할 수 있습니다.\n\n③ 회사 정책에 따라 환불이 가능합니다.\n\n④ 회사는 부정 이용이 확인될 경우 포인트 사용을 제한할 수 있습니다.\n\n제7조(결제)\n\n① 회원은 등록한 카드 또는 충전된 포인트로 결제할 수 있습니다.\n\n② 결제 오류가 발생한 경우 회사는 신속히 조치합니다.\n\n③ 환불은 가맹점 정책을 따릅니다.\n\n제8조(예약 서비스)\n\n① 회원은 숙소와 공유오피스를 예약할 수 있습니다.\n\n② 예약 취소 및 환불은 해당 업체의 정책을 따릅니다.\n\n③ 회사는 중개 플랫폼으로서 예약 업체의 운영에 대한 책임을 부담하지 않습니다.\n\n제9조(바우처)\n\n① 회사는 정부 또는 기관과 연계된 워케이션 바우처를 지원할 수 있습니다.\n\n② 부정 사용이 확인될 경우 바우처 사용을 제한할 수 있습니다.\n\n제10조(후기)\n\n① 회원은 실제 이용 후 후기를 작성할 수 있습니다.\n\n② 허위 사실이나 타인의 권리를 침해하는 후기는 삭제될 수 있습니다.\n\n제11조(회원의 의무)\n\n회원은 다음 행위를 하여서는 안 됩니다.\n\n1. 타인 명의 사용\n\n2. 허위 예약\n\n3. 부정 결제\n\n4. 시스템 해킹\n\n5. 불법 프로그램 사용\n\n6. 회사 업무 방해\n\n제12조(회사의 의무)\n\n① 회사는 안정적인 서비스를 제공합니다.\n\n② 개인정보를 안전하게 보호합니다.\n\n③ 회원의 문의를 신속히 처리합니다.\n\n제13조(서비스 제한)\n\n회사에는 다음의 경우 서비스 이용을 제한할 수 있습니다.\n\n1. 약관 위반\n\n2. 부정 결제\n\n3. 해킹 시도\n\n4. 법령 위반\n\n5. 시스템 점검\n\n제14조(면책)\n\n① 회사는 천재지변 등 불가항력으로 발생한 손해에 대해 책임지지 않습니다.\n\n② 숙소 및 공유오피스 운영상의 문제는 해당 업체의 책임입니다.\n\n③ 회원의 귀책사유로 발생한 손해는 회원이 부담합니다.\n\n제15조(계약 해지)\n\n회원은 언제든지 회원탈퇴를 신청할 수 있습니다.\n\n제16조(준거법)\n\n본 약관은 대한민국 법률을 따릅니다.\n\n부칙\n\n본 약관은 2026년 7월 30일부터 시행합니다.\n', 1, '2026-08-11 17:11:37')
ON DUPLICATE KEY UPDATE
    title = VALUES(title),
    content = VALUES(content),
    required = VALUES(required),
    created_at = VALUES(created_at);

INSERT INTO user_terms_agreements (id, user_id, term_id, agreed_at)
VALUES
    (1, 1, 1, '2026-08-11 17:11:54'),
    (2, 1, 2, '2026-08-11 17:11:54'),
    (3, 1, 3, '2026-08-11 17:11:54'),
    (4, 1, 4, '2026-08-11 17:11:54'),
    (5, 1, 5, '2026-08-11 17:11:54')
ON DUPLICATE KEY UPDATE
    agreed_at = VALUES(agreed_at);

-- -----------------------------------------------------------------------------
-- workations
-- -----------------------------------------------------------------------------
INSERT INTO workations
    (id, user_id, region_id, title, start_date, end_date, business_budget_total, personal_budget_total, status, settled_at, created_at, updated_at)
VALUES
    (4, 1, 4, '제주', '2026-08-12', '2026-08-15', 600000.00, 600000.00, 'ACTIVE', NULL, '2026-08-12 05:28:23', '2026-08-12 05:28:23')
ON DUPLICATE KEY UPDATE
    user_id = VALUES(user_id),
    region_id = VALUES(region_id),
    title = VALUES(title),
    start_date = VALUES(start_date),
    end_date = VALUES(end_date),
    business_budget_total = VALUES(business_budget_total),
    personal_budget_total = VALUES(personal_budget_total),
    status = VALUES(status),
    settled_at = VALUES(settled_at),
    created_at = VALUES(created_at),
    updated_at = VALUES(updated_at);

-- -----------------------------------------------------------------------------
-- budgets
-- -----------------------------------------------------------------------------
INSERT INTO budgets
    (id, workation_id, expense_category_id, budget_type, target_amount, created_at, updated_at)
VALUES
    (25, 4, 1, 'WORK', 100000.00, '2026-08-12 05:28:52', '2026-08-12 05:28:52'),
    (26, 4, 2, 'WORK', 100000.00, '2026-08-12 05:28:52', '2026-08-12 05:28:52'),
    (27, 4, 3, 'WORK', 100000.00, '2026-08-12 05:28:52', '2026-08-12 05:28:52'),
    (28, 4, 4, 'WORK', 100000.00, '2026-08-12 05:28:52', '2026-08-12 05:28:52'),
    (29, 4, 5, 'WORK', 100000.00, '2026-08-12 05:28:52', '2026-08-12 05:28:52'),
    (30, 4, 6, 'WORK', 100000.00, '2026-08-12 05:28:52', '2026-08-12 05:28:52'),
    (31, 4, 13, 'PERSONAL', 100000.00, '2026-08-12 05:28:52', '2026-08-12 05:28:52'),
    (32, 4, 14, 'PERSONAL', 100000.00, '2026-08-12 05:28:52', '2026-08-12 05:28:52'),
    (33, 4, 15, 'PERSONAL', 100000.00, '2026-08-12 05:28:52', '2026-08-12 05:28:52'),
    (34, 4, 16, 'PERSONAL', 100000.00, '2026-08-12 05:28:52', '2026-08-12 05:28:52'),
    (35, 4, 17, 'PERSONAL', 100000.00, '2026-08-12 05:28:52', '2026-08-12 05:28:52'),
    (36, 4, 18, 'PERSONAL', 100000.00, '2026-08-12 05:28:52', '2026-08-12 05:28:52')
ON DUPLICATE KEY UPDATE
    target_amount = VALUES(target_amount),
    created_at = VALUES(created_at),
    updated_at = VALUES(updated_at);

-- -----------------------------------------------------------------------------
-- survey_questions / survey_options
-- -----------------------------------------------------------------------------
INSERT INTO survey_questions
    (id, question_code, question, category, question_type, min_select_count, max_select_count, created_at, updated_at)
VALUES
    (1, 'PRIORITY_FACTOR', '장소를 선택할 때 가장 중요하게 생각하는 요소는 무엇인가요?', 'COMMON', 'SINGLE_CHOICE', 1, 1, '2026-08-01 08:00:00', '2026-08-01 08:00:00'),
    (2, 'OFFICE_ENVIRONMENT', '어떤 공유오피스 환경을 선호하시나요?', 'OFFICE', 'SINGLE_CHOICE', 1, 1, '2026-08-01 08:00:00', '2026-08-01 08:00:00'),
    (3, 'ACTIVITY_PREFERENCE', '선호하는 여가 활동을 선택해주세요.', 'ACTIVITY', 'MULTIPLE_CHOICE', 1, 3, '2026-08-01 08:00:00', '2026-08-01 08:00:00'),
    (4, 'MEAL_STYLE', '식비를 어떤 방식으로 사용하고 싶으신가요?', 'RESTAURANT', 'SINGLE_CHOICE', 1, 1, '2026-08-01 08:00:00', '2026-08-01 08:00:00')
ON DUPLICATE KEY UPDATE
    question_code = VALUES(question_code),
    question = VALUES(question),
    category = VALUES(category),
    question_type = VALUES(question_type),
    min_select_count = VALUES(min_select_count),
    max_select_count = VALUES(max_select_count),
    created_at = VALUES(created_at),
    updated_at = VALUES(updated_at);

INSERT INTO survey_options
    (id, question_id, tag_id, option_code, option_name, weight)
VALUES
    (1, 1, NULL, 'BUDGET', '예산', 100),
    (2, 1, NULL, 'ACCESSIBILITY', '이동 편의', 100),
    (3, 1, NULL, 'RATING', '높은 평점', 100),
    (4, 1, NULL, 'BALANCED', '균형 있게', 100),
    (5, 2, NULL, 'QUIET', '조용하고 집중하기 좋은 공간', 100),
    (6, 2, NULL, 'OPEN', '자유롭고 개방적인 공간', 100),
    (7, 3, NULL, 'WATER_SPORTS', '수상 레포츠', 100),
    (8, 3, NULL, 'LAND_SPORTS', '육상 레포츠', 100),
    (9, 3, NULL, 'RURAL_EXPERIENCE', '농어촌 체험', 100),
    (10, 3, NULL, 'WELLNESS_TOURISM', '웰니스 관광', 100),
    (11, 3, NULL, 'CAFE_TEA_HOUSE', '카페/찻집', 100),
    (12, 3, NULL, 'NATURAL_PARK', '자연공원', 100),
    (13, 3, NULL, 'MOUNTAIN_SCENERY', '자연경관(산)', 100),
    (14, 3, NULL, 'WATER_SCENERY', '자연경관(하천/해양)', 100),
    (15, 3, NULL, 'NATURAL_ECOLOGY', '자연생태', 100),
    (16, 3, NULL, 'NONE', '특별히 없음', 100),
    (17, 4, NULL, 'BALANCED', '균형형', 100),
    (18, 4, NULL, 'DINNER_FOCUSED', '저녁 집중형', 100),
    (19, 4, NULL, 'SKIP_BREAKFAST', '아침 제외형', 100)
ON DUPLICATE KEY UPDATE
    question_id = VALUES(question_id),
    tag_id = VALUES(tag_id),
    option_code = VALUES(option_code),
    option_name = VALUES(option_name),
    weight = VALUES(weight);

-- -----------------------------------------------------------------------------
-- user_survey
-- -----------------------------------------------------------------------------
INSERT INTO user_surveys (id, user_id, workation_id, created_at, updated_at)
VALUES (3, 1, 4, '2026-08-12 05:28:35', '2026-08-12 05:28:35')
ON DUPLICATE KEY UPDATE
    user_id = VALUES(user_id),
    workation_id = VALUES(workation_id),
    created_at = VALUES(created_at),
    updated_at = VALUES(updated_at);

INSERT INTO user_survey_answers (id, survey_id, option_id, created_at)
VALUES
    (11, 3, 2, '2026-08-12 05:28:35'),
    (12, 3, 5, '2026-08-12 05:28:35'),
    (13, 3, 7, '2026-08-12 05:28:35'),
    (14, 3, 9, '2026-08-12 05:28:35'),
    (15, 3, 12, '2026-08-12 05:28:35'),
    (16, 3, 17, '2026-08-12 05:28:35')
ON DUPLICATE KEY UPDATE
    survey_id = VALUES(survey_id),
    option_id = VALUES(option_id),
    created_at = VALUES(created_at);

-- -----------------------------------------------------------------------------
-- 마이그레이션 구분: 누락 데이터 확인용
-- -----------------------------------------------------------------------------
-- 위에서 입력한 핵심 행(요청 데이터)은 전부 없으면 추가/있으면 갱신됩니다.

-- ============================================================================
-- 이하: 제외 항목 외 나머지 테이블 더미 데이터
-- 대상: 유저 알림, 기기, 결제/카드/계좌, 추천/예약/리뷰, 가맹점, 지출/추천 API 연동 외부
-- ============================================================================

-- -----------------------------------------------------------------------------
-- 사용자 장비 / 알림
-- -----------------------------------------------------------------------------
-- user_device
INSERT INTO user_device
    (user_id, device_id, device_name, pin_hash, last_login_at, created_at)
VALUES
    (@USER_ID, 'device-desktop-u1', 'Windows Chrome', '$2a$10$devicepin000000000000000000000000000', '2026-08-12 05:30:00', '2026-08-11 18:00:00')
ON DUPLICATE KEY UPDATE
    device_name = VALUES(device_name),
    pin_hash = VALUES(pin_hash),
    last_login_at = VALUES(last_login_at);

-- user_notification_settings
INSERT INTO user_notification_settings
    (user_id, system_notify, budget_warning, transfer_notify, payment_notify, event_notify, reservation_notify, review_notify)
VALUES
    (1, 1, 1, 1, 1, 0, 1, 1)
ON DUPLICATE KEY UPDATE
    system_notify = VALUES(system_notify),
    budget_warning = VALUES(budget_warning),
    transfer_notify = VALUES(transfer_notify),
    payment_notify = VALUES(payment_notify),
    event_notify = VALUES(event_notify),
    reservation_notify = VALUES(reservation_notify),
    review_notify = VALUES(review_notify);

-- notification_histories
INSERT INTO notification_histories
    (user_id, type, important, title, content, `read`, created_at)
VALUES
    (1, 'SYSTEM', 1, '환영합니다', '계정 테스트용 샘플 알림 메시지', 0, '2026-08-12 05:31:00'),
    (1, 'BUDGET', 0, '예산 알림', '주간 예산 사용량이 40%를 초과했습니다', 0, '2026-08-12 05:31:30'),
    (1, 'RESERVATION', 1, '예약 알림', '숙소 예약이 확정되었습니다', 1, '2026-08-12 05:32:00'),
    (1, 'REVIEW', 0, '리뷰 알림', '최근 이용 후기는 5점입니다', 0, '2026-08-12 05:32:30')
ON DUPLICATE KEY UPDATE
    important = VALUES(important),
    title = VALUES(title),
    content = VALUES(content),
    `read` = VALUES(`read`),
    created_at = VALUES(created_at);

-- user_category_labels
INSERT INTO user_category_labels (user_id, expense_category_id, custom_name)
SELECT 1, c.id, c.code
FROM expense_categories c
WHERE c.id IN (1, 13, 15)
  AND NOT EXISTS (
      SELECT 1
      FROM user_category_labels ucl
      WHERE ucl.user_id = 1
        AND ucl.expense_category_id = c.id
);

-- -----------------------------------------------------------------------------
-- 은행/카드/지갑/거래
-- -----------------------------------------------------------------------------
-- banks
INSERT INTO banks (code, name, logo_url)
VALUES
    ('KB', 'KB국민은행', 'https://example.com/bank/kb.png'),
    ('NH', '농협은행', 'https://example.com/bank/nh.png')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    logo_url = VALUES(logo_url);

-- card_companies
INSERT INTO card_companies (code, name, logo_url)
VALUES
    ('BC', '비씨카드', 'https://example.com/card/bc.png'),
    ('SS', '삼성카드', 'https://example.com/card/ss.png')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    logo_url = VALUES(logo_url);

-- wallets
INSERT INTO wallets (user_id, balance, updated_at)
VALUES (@USER_ID, 500000.00, '2026-08-12 06:10:00')
ON DUPLICATE KEY UPDATE
    balance = VALUES(balance),
    updated_at = VALUES(updated_at);

-- bank_accounts
INSERT INTO bank_accounts
    (user_id, bank_code, account_number, product_name, balance, is_primary, is_withdrawal_agreed, withdrawal_agreed_at, balance_updated_at, is_deleted)
VALUES
    (@USER_ID, 'KB', '110-123-456789', 'Main Salary', 1200000.00, 1, 1, '2026-08-12 06:11:00', '2026-08-12 06:11:00', 0)
ON DUPLICATE KEY UPDATE
    bank_code = VALUES(bank_code),
    account_number = VALUES(account_number),
    product_name = VALUES(product_name),
    balance = VALUES(balance),
    is_primary = VALUES(is_primary),
    is_withdrawal_agreed = VALUES(is_withdrawal_agreed),
    withdrawal_agreed_at = VALUES(withdrawal_agreed_at),
    balance_updated_at = VALUES(balance_updated_at),
    is_deleted = VALUES(is_deleted);

-- cards
INSERT INTO cards
    (user_id, card_company_code, card_name, card_number, card_classification, card_type, is_primary, is_agreed, created_at, is_deleted, updated_at, deleted_at)
VALUES
    (@USER_ID, 'BC', 'WorkIt Work Card', '4111111111111111', 'CREDIT', 'WORK', 1, 1, '2026-08-12 06:12:00', 0, '2026-08-12 06:12:00', NULL),
    (@USER_ID, 'SS', 'WorkIt Personal Card', '5111111111111111', 'DEBIT', 'PERSONAL', 0, 1, '2026-08-12 06:13:00', 0, '2026-08-12 06:13:00', NULL)
ON DUPLICATE KEY UPDATE
    card_company_code = VALUES(card_company_code),
    card_name = VALUES(card_name),
    card_number = VALUES(card_number),
    card_classification = VALUES(card_classification),
    card_type = VALUES(card_type),
    is_primary = VALUES(is_primary),
    is_agreed = VALUES(is_agreed),
    updated_at = VALUES(updated_at),
    is_deleted = VALUES(is_deleted),
    deleted_at = VALUES(deleted_at);

-- linkable_accounts
INSERT INTO linkable_accounts
    (user_id, bank_code, account_number, product_name, is_linked)
SELECT *
FROM (
    SELECT
        @USER_ID,
        'NH',
        '110-999-888777',
        '연동 계좌',
        1
    UNION ALL
    SELECT
        @USER_ID,
        'KB',
        '110-777-666555',
        '연동 테스트 계좌2',
        1
) AS src(user_id, bank_code, account_number, product_name, is_linked)
WHERE NOT EXISTS (
    SELECT 1
    FROM linkable_accounts la
    WHERE la.user_id = src.user_id
      AND la.bank_code = src.bank_code
      AND la.account_number = src.account_number
);
ON DUPLICATE KEY UPDATE
    product_name = VALUES(product_name),
    is_linked = VALUES(is_linked);

-- linkable_cards
INSERT INTO linkable_cards
    (user_id, card_company_code, card_number, card_name, card_classification, card_type, is_linked)
SELECT *
FROM (
    SELECT
        @USER_ID,
        'BC',
        '4111222233334444',
        '연동 BC 카드',
        'CREDIT',
        'WORK',
        1
    UNION ALL
    SELECT
        @USER_ID,
        'SS',
        '5111222233334444',
        '연동 삼성 카드',
        'DEBIT',
        'PERSONAL',
        1
) AS src(user_id, card_company_code, card_number, card_name, card_classification, card_type, is_linked)
WHERE NOT EXISTS (
    SELECT 1
    FROM linkable_cards lc
    WHERE lc.user_id = src.user_id
      AND lc.card_company_code = src.card_company_code
      AND lc.card_number = src.card_number
);
ON DUPLICATE KEY UPDATE
    card_name = VALUES(card_name),
    card_classification = VALUES(card_classification),
    card_type = VALUES(card_type),
    is_linked = VALUES(is_linked);

-- transactions
INSERT INTO transactions
    (user_id, wallet_id, bank_account_id, card_id, workation_id, reservation_id, merchant_id, idempotency_key, payment_source_type,
     merchant_name, amount, transaction_type, category_assigned, is_business_expense, approved_number, pg_transaction_id, transaction_number, status,
     approved_at, created_at, updated_at, cancelled_at)
VALUES
    (
        @USER_ID,
        (SELECT id FROM wallets WHERE user_id = @USER_ID ORDER BY id DESC LIMIT 1),
        (SELECT id FROM bank_accounts WHERE user_id = @USER_ID AND account_number = '110-123-456789' LIMIT 1),
        NULL,
        @WORKATION_ID,
        NULL,
        NULL,
        'idm-u1-deposit-001',
        'WALLET',
        '테스트 충전',
        500000.00,
        'DEPOSIT',
        '예치금 충전',
        1,
        NULL,
        NULL,
        'TX-U1-DEP-001',
        'PAID',
        '2026-08-12 06:20:00',
        '2026-08-12 06:20:00',
        '2026-08-12 06:20:00',
        NULL
    ),
    (
        @USER_ID,
        NULL,
        NULL,
        (SELECT id FROM cards WHERE user_id = @USER_ID AND card_number = '4111111111111111' LIMIT 1),
        @WORKATION_ID,
        NULL,
        (SELECT id FROM merchants WHERE name = '제주 오션 오피스' LIMIT 1),
        'idm-u1-work-pay-001',
        'CARD',
        '해변 오피스 공유',
        120000.00,
        'PAYMENT',
        '공유오피스',
        1,
        'APV-2026-1001',
        'PG-1001',
        'TX-U1-WORK-001',
        'PAID',
        '2026-08-12 06:21:00',
        '2026-08-12 06:21:00',
        '2026-08-12 06:21:00',
        NULL
    ),
    (
        @USER_ID,
        (SELECT id FROM wallets WHERE user_id = @USER_ID ORDER BY id DESC LIMIT 1),
        NULL,
        NULL,
        @WORKATION_ID,
        NULL,
        (SELECT id FROM merchants WHERE name = '제주 바람카페' LIMIT 1),
        'idm-u1-personal-pay-001',
        'WALLET',
        '휴양 카페',
        45000.00,
        'PAYMENT',
        '식비',
        0,
        NULL,
        NULL,
        'TX-U1-PERS-001',
        'PAID',
        '2026-08-12 06:22:00',
        '2026-08-12 06:22:00',
        '2026-08-12 06:22:00',
        NULL
    )
ON DUPLICATE KEY UPDATE
    amount = VALUES(amount),
    status = VALUES(status),
    updated_at = VALUES(updated_at),
    transaction_number = VALUES(transaction_number),
    cancelled_at = VALUES(cancelled_at);

SET @WALLET_ID := (SELECT id FROM wallets WHERE user_id = @USER_ID ORDER BY id DESC LIMIT 1);
SET @TX_U1_DEP_ID := (SELECT id FROM transactions WHERE user_id = @USER_ID AND transaction_number = 'TX-U1-DEP-001' ORDER BY id DESC LIMIT 1);
SET @TX_U1_WORK_ID := (SELECT id FROM transactions WHERE user_id = @USER_ID AND transaction_number = 'TX-U1-WORK-001' ORDER BY id DESC LIMIT 1);
SET @TX_U1_PERS_ID := (SELECT id FROM transactions WHERE user_id = @USER_ID AND transaction_number = 'TX-U1-PERS-001' ORDER BY id DESC LIMIT 1);

-- ledger_entries
INSERT INTO ledger_entries
    (transaction_id, entry_seq, account_type, account_ref_id, direction, amount, balance_after, created_at)
VALUES
    (@TX_U1_DEP_ID, 1, 'WALLET', @WALLET_ID, 'CREDIT', 500000.00, 500000.00, '2026-08-12 06:20:00'),
    (@TX_U1_WORK_ID, 1, 'CARD', (SELECT id FROM cards WHERE user_id = @USER_ID AND card_number = '4111111111111111' LIMIT 1), 'DEBIT', 120000.00, 120000.00, '2026-08-12 06:21:00'),
    (@TX_U1_WORK_ID, 2, 'MERCHANT', (SELECT id FROM merchants WHERE name = '제주 오션 오피스' LIMIT 1), 'CREDIT', 120000.00, NULL, '2026-08-12 06:21:00'),
    (@TX_U1_PERS_ID, 1, 'WALLET', @WALLET_ID, 'DEBIT', 45000.00, 455000.00, '2026-08-12 06:22:00'),
    (@TX_U1_PERS_ID, 2, 'MERCHANT', (SELECT id FROM merchants WHERE name = '제주 바람카페' LIMIT 1), 'CREDIT', 45000.00, NULL, '2026-08-12 06:22:00')
ON DUPLICATE KEY UPDATE
    account_type = VALUES(account_type),
    account_ref_id = VALUES(account_ref_id),
    direction = VALUES(direction),
    amount = VALUES(amount),
    balance_after = VALUES(balance_after),
    created_at = VALUES(created_at);

-- -----------------------------------------------------------------------------
-- 가맹점/태그/타입별 상세
-- -----------------------------------------------------------------------------
-- tags
INSERT INTO tags
    (name)
VALUES
    ('조용함'),
    ('해안'),
    ('미식'),
    ('액티비티'),
    ('워크숍'),
    ('바다뷰'),
    ('휴식');

-- merchants
INSERT INTO merchants
    (region_id, name, taxpayer_identification_number, address, category, latitude, longitude, phone_number, rating, thumbnail_url, price)
SELECT 4, '제주 오션 오피스', '4100010001', '제주시 노형동 21', 'OFFICE', 33.492300, 126.495100, '064-800-0001', 4.7, 'https://example.com/merchant/40001.jpg', 6500
WHERE NOT EXISTS (SELECT 1 FROM merchants WHERE name = '제주 오션 오피스');

INSERT INTO merchants
    (region_id, name, taxpayer_identification_number, address, category, latitude, longitude, phone_number, rating, thumbnail_url, price)
SELECT 4, '서귀포 힐링 리조트', '4100010002', '서귀포시 대정읍 11', 'ACCOMMODATION', 33.246100, 126.507700, '064-800-0002', 4.8, 'https://example.com/merchant/40002.jpg', 120000
WHERE NOT EXISTS (SELECT 1 FROM merchants WHERE name = '서귀포 힐링 리조트');

INSERT INTO merchants
    (region_id, name, taxpayer_identification_number, address, category, latitude, longitude, phone_number, rating, thumbnail_url, price)
SELECT 4, '제주 해돋이 레스토랑', '4100010003', '제주시 연동 78', 'RESTAURANT', 33.495200, 126.493600, '064-800-0003', 4.5, 'https://example.com/merchant/40003.jpg', 25000
WHERE NOT EXISTS (SELECT 1 FROM merchants WHERE name = '제주 해돋이 레스토랑');

INSERT INTO merchants
    (region_id, name, taxpayer_identification_number, address, category, latitude, longitude, phone_number, rating, thumbnail_url, price)
SELECT 4, '한라 액티브 스파크', '4100010004', '제주시 외도일동 14', 'ACTIVITY', 33.497900, 126.500300, '064-800-0004', 4.6, 'https://example.com/merchant/40004.jpg', 45000
WHERE NOT EXISTS (SELECT 1 FROM merchants WHERE name = '한라 액티브 스파크');

INSERT INTO merchants
    (region_id, name, taxpayer_identification_number, address, category, latitude, longitude, phone_number, rating, thumbnail_url, price)
SELECT 4, '제주 집중 코워킹', '4100010005', '제주시 애월읍 100', 'OFFICE', 33.478900, 126.337800, '064-800-0005', 4.9, 'https://example.com/merchant/40005.jpg', 18000
WHERE NOT EXISTS (SELECT 1 FROM merchants WHERE name = '제주 집중 코워킹');

INSERT INTO merchants
    (region_id, name, taxpayer_identification_number, address, category, latitude, longitude, phone_number, rating, thumbnail_url, price)
SELECT 4, '제주 바람카페', '4100010006', '제주시 노형동 102', 'RESTAURANT', 33.497100, 126.494900, '064-800-0006', 4.3, 'https://example.com/merchant/40006.jpg', 20000
WHERE NOT EXISTS (SELECT 1 FROM merchants WHERE name = '제주 바람카페');

-- merchant_tags
INSERT INTO merchant_tags
    (merchant_id, tag_id)
SELECT
    m.id,
    t.id
FROM (
    SELECT '제주 오션 오피스' AS merchant_name, '조용함' AS tag_name UNION ALL
    SELECT '제주 오션 오피스', '바다뷰' UNION ALL
    SELECT '서귀포 힐링 리조트', '휴식' UNION ALL
    SELECT '서귀포 힐링 리조트', '해안' UNION ALL
    SELECT '제주 해돋이 레스토랑', '미식' UNION ALL
    SELECT '한라 액티브 스파크', '액티비티' UNION ALL
    SELECT '제주 집중 코워킹', '조용함' UNION ALL
    SELECT '제주 집중 코워킹', '워크숍' UNION ALL
    SELECT '제주 바람카페', '미식'
) AS src(merchant_name, tag_name)
JOIN merchants m
    ON m.name = src.merchant_name
JOIN tags t
    ON t.name = src.tag_name
LEFT JOIN merchant_tags mt
    ON mt.merchant_id = m.id
   AND mt.tag_id = t.id
WHERE NOT EXISTS (
    SELECT 1
    FROM merchant_tags
    WHERE merchant_id = m.id
      AND tag_id = t.id
);

-- accommodations
INSERT INTO accommodations
    (merchant_id, accommodation_type, description, check_in_time, check_out_time)
SELECT
    id,
    'HOTEL',
    '오션뷰 객실과 스터디 라운드를 함께 이용 가능한 워케이션형 숙소',
    '15:00:00',
    '11:00:00'
FROM merchants
WHERE name = '서귀포 힐링 리조트'
ON DUPLICATE KEY UPDATE
    accommodation_type = VALUES(accommodation_type),
    description = VALUES(description),
    check_in_time = VALUES(check_in_time),
    check_out_time = VALUES(check_out_time);

-- offices
INSERT INTO offices
    (merchant_id, description, noise_level)
SELECT id, '조용한 개방형 좌석이 많은 집중형 오피스', 'QUIET'
FROM merchants
WHERE name = '제주 오션 오피스'
UNION ALL
SELECT id, '회의실·브레인스토밍 룸을 갖춘 협업형 오피스', 'COLLAB'
FROM merchants
WHERE name = '제주 집중 코워킹'
ON DUPLICATE KEY UPDATE
    description = VALUES(description),
    noise_level = VALUES(noise_level);

-- restaurants
INSERT INTO restaurants
    (merchant_id, food_type, price_level)
SELECT id, 'KOREAN', 3
FROM merchants
WHERE name = '제주 해돋이 레스토랑'
UNION ALL
SELECT id, 'CAFE', 2
FROM merchants
WHERE name = '제주 바람카페'
ON DUPLICATE KEY UPDATE
    food_type = VALUES(food_type),
    price_level = VALUES(price_level);

-- activities
INSERT INTO activities
    (merchant_id, activity_type)
SELECT id, 'SPORTS'
FROM merchants
WHERE name = '한라 액티브 스파크'
ON DUPLICATE KEY UPDATE
    activity_type = VALUES(activity_type);

-- schedules
-- 음식점·여가 방문 계획. 같은 시드를 다시 실행해도 동일 일정은 중복 생성하지 않는다.
INSERT INTO schedules
    (workation_id, merchant_id, scheduled_at, created_at, updated_at)
SELECT
    @WORKATION_ID,
    m.id,
    '2026-08-12 12:30:00',
    '2026-08-12 06:10:00',
    '2026-08-12 06:10:00'
FROM merchants m
WHERE m.name = '제주 해돋이 레스토랑'
  AND m.category = 'RESTAURANT'
  AND NOT EXISTS (
      SELECT 1
        FROM schedules s
       WHERE s.workation_id = @WORKATION_ID
         AND s.merchant_id = m.id
         AND s.scheduled_at = '2026-08-12 12:30:00'
  );

INSERT INTO schedules
    (workation_id, merchant_id, scheduled_at, created_at, updated_at)
SELECT
    @WORKATION_ID,
    m.id,
    '2026-08-13 18:00:00',
    '2026-08-12 06:11:00',
    '2026-08-12 06:11:00'
FROM merchants m
WHERE m.name = '한라 액티브 스파크'
  AND m.category = 'ACTIVITY'
  AND NOT EXISTS (
      SELECT 1
        FROM schedules s
       WHERE s.workation_id = @WORKATION_ID
         AND s.merchant_id = m.id
         AND s.scheduled_at = '2026-08-13 18:00:00'
  );

INSERT INTO schedules
    (workation_id, merchant_id, scheduled_at, created_at, updated_at)
SELECT
    @WORKATION_ID,
    m.id,
    '2026-08-14 15:00:00',
    '2026-08-12 06:12:00',
    '2026-08-12 06:12:00'
FROM merchants m
WHERE m.name = '제주 바람카페'
  AND m.category = 'RESTAURANT'
  AND NOT EXISTS (
      SELECT 1
        FROM schedules s
       WHERE s.workation_id = @WORKATION_ID
         AND s.merchant_id = m.id
         AND s.scheduled_at = '2026-08-14 15:00:00'
  );

-- merchant_id 기반 규칙은 가맹점 seed 입력 후 반영
-- user_category_rules
INSERT INTO user_category_rules
    (user_id, merchant_id, expense_category_id, budget_type, created_at, updated_at)
SELECT
    @USER_ID,
    m.id,
    ec.id,
    'WORK',
    '2026-08-12 06:00:00',
    '2026-08-12 06:00:00'
FROM merchants m
    JOIN expense_categories ec
        ON ec.budget_type = 'WORK'
       AND ec.code = 'COMMUNICATION'
WHERE m.name = '제주 해돋이 레스토랑'
  AND NOT EXISTS (
    SELECT 1
    FROM user_category_rules ucr
    WHERE ucr.user_id = @USER_ID
      AND ucr.merchant_id = m.id
      AND ucr.budget_type = 'WORK'
      AND ucr.expense_category_id = ec.id
);

INSERT INTO user_category_rules
    (user_id, merchant_id, expense_category_id, budget_type, created_at, updated_at)
SELECT
    @USER_ID,
    m.id,
    ec.id,
    'WORK',
    '2026-08-12 06:01:00',
    '2026-08-12 06:01:00'
FROM merchants m
    JOIN expense_categories ec
        ON ec.budget_type = 'WORK'
       AND ec.code = 'ACTIVITY'
WHERE m.name = '한라 액티브 스파크'
  AND NOT EXISTS (
    SELECT 1
    FROM user_category_rules ucr
    WHERE ucr.user_id = @USER_ID
      AND ucr.merchant_id = m.id
      AND ucr.budget_type = 'WORK'
      AND ucr.expense_category_id = ec.id
);

INSERT INTO user_category_rules
    (user_id, merchant_id, expense_category_id, budget_type, created_at, updated_at)
SELECT
    @USER_ID,
    m.id,
    ec.id,
    'PERSONAL',
    '2026-08-12 06:02:00',
    '2026-08-12 06:02:00'
FROM merchants m
    JOIN expense_categories ec
        ON ec.budget_type = 'PERSONAL'
       AND ec.code = 'FOOD'
WHERE m.name = '제주 바람카페'
  AND NOT EXISTS (
    SELECT 1
    FROM user_category_rules ucr
    WHERE ucr.user_id = @USER_ID
      AND ucr.merchant_id = m.id
      AND ucr.budget_type = 'PERSONAL'
      AND ucr.expense_category_id = ec.id
);

-- -----------------------------------------------------------------------------
-- 지출/예약/연결
-- -----------------------------------------------------------------------------
-- workation_expenses
INSERT INTO workation_expenses
    (workation_id, transaction_id, expense_category_id, card_id, budget_type, amount, merchant_name, spent_at, memo, is_auto_categorized, created_at, updated_at)
VALUES
    (@WORKATION_ID, @TX_U1_WORK_ID, (SELECT id FROM expense_categories WHERE budget_type = 'WORK' AND code = 'OFFICE' LIMIT 1), (SELECT id FROM cards WHERE user_id = @USER_ID AND card_number = '4111111111111111' LIMIT 1), 'WORK', 120000.00, '해변 오피스 공유', '2026-08-12', '법인 공유오피스 좌석권 결제', 1, '2026-08-12 06:25:00', '2026-08-12 06:25:00'),
    (@WORKATION_ID, @TX_U1_PERS_ID, (SELECT id FROM expense_categories WHERE budget_type = 'PERSONAL' AND code = 'FOOD' LIMIT 1), NULL, 'PERSONAL', 45000.00, '제주 바람카페', '2026-08-13', '카페 이용(개인)', 1, '2026-08-12 06:26:00', '2026-08-12 06:26:00')
ON DUPLICATE KEY UPDATE
    amount = VALUES(amount),
    memo = VALUES(memo),
    spent_at = VALUES(spent_at),
    updated_at = VALUES(updated_at);

-- reservation_products
INSERT INTO reservation_products
    (product_name, description, product_detail_type, max_headcount, unit_price, merchant_id, thumbnail_url)
SELECT '오션 오피스 데일리 데스크', '1인 집중 데스크 좌석', 'OFFICE_SEAT', 1, 18000.00, m.id, 'https://example.com/product/30001.jpg'
FROM merchants m
WHERE m.name = '제주 오션 오피스'
  AND NOT EXISTS (SELECT 1 FROM reservation_products rp WHERE rp.product_name = '오션 오피스 데일리 데스크')
UNION ALL
SELECT '제주 라운지룸 미팅룸', '2인 미팅룸, 화이트보드 포함', 'MEETING_ROOM', 4, 75000.00, m.id, 'https://example.com/product/30002.jpg'
FROM merchants m
WHERE m.name = '제주 집중 코워킹'
  AND NOT EXISTS (SELECT 1 FROM reservation_products rp WHERE rp.product_name = '제주 라운지룸 미팅룸')
UNION ALL
SELECT '워케이션 스위트 스탠다드', '넓은 침실·책상·와이파이', 'ROOM', 2, 120000.00, m.id, 'https://example.com/product/30003.jpg'
FROM merchants m
WHERE m.name = '서귀포 힐링 리조트'
  AND NOT EXISTS (SELECT 1 FROM reservation_products rp WHERE rp.product_name = '워케이션 스위트 스탠다드')
UNION ALL
SELECT '액티비티 패키지 - 스킨십 해변 트랙', '짧은 체험형 액티비티', 'OFFICE_SEAT', 1, 32000.00, m.id, 'https://example.com/product/30004.jpg'
FROM merchants m
WHERE m.name = '한라 액티브 스파크'
  AND NOT EXISTS (SELECT 1 FROM reservation_products rp WHERE rp.product_name = '액티비티 패키지 - 스킨십 해변 트랙')
UNION ALL
SELECT '제주식 뷔페', '점심식사권', 'OFFICE_SEAT', 1, 25000.00, m.id, 'https://example.com/product/30005.jpg'
FROM merchants m
WHERE m.name = '제주 바람카페'
  AND NOT EXISTS (SELECT 1 FROM reservation_products rp WHERE rp.product_name = '제주식 뷔페')
ON DUPLICATE KEY UPDATE
    description = VALUES(description),
    product_detail_type = VALUES(product_detail_type),
    max_headcount = VALUES(max_headcount),
    unit_price = VALUES(unit_price),
    merchant_id = VALUES(merchant_id),
    thumbnail_url = VALUES(thumbnail_url);

-- product_daily_inventories
INSERT INTO product_daily_inventories
    (product_id, inventory_date, total_capacity, remaining_capacity, is_available)
SELECT rp.id, '2026-08-12', 10, 8, 1 FROM reservation_products rp WHERE rp.product_name = '오션 오피스 데일리 데스크'
UNION ALL SELECT rp.id, '2026-08-13', 10, 9, 1 FROM reservation_products rp WHERE rp.product_name = '오션 오피스 데일리 데스크'
UNION ALL SELECT rp.id, '2026-08-14', 10, 10, 1 FROM reservation_products rp WHERE rp.product_name = '오션 오피스 데일리 데스크'
UNION ALL SELECT rp.id, '2026-08-12', 4, 3, 1 FROM reservation_products rp WHERE rp.product_name = '제주 라운지룸 미팅룸'
UNION ALL SELECT rp.id, '2026-08-13', 4, 2, 1 FROM reservation_products rp WHERE rp.product_name = '제주 라운지룸 미팅룸'
UNION ALL SELECT rp.id, '2026-08-12', 3, 2, 1 FROM reservation_products rp WHERE rp.product_name = '워케이션 스위트 스탠다드'
UNION ALL SELECT rp.id, '2026-08-13', 3, 3, 1 FROM reservation_products rp WHERE rp.product_name = '워케이션 스위트 스탠다드'
UNION ALL SELECT rp.id, '2026-08-14', 3, 3, 1 FROM reservation_products rp WHERE rp.product_name = '워케이션 스위트 스탠다드'
UNION ALL SELECT rp.id, '2026-08-12', 8, 7, 1 FROM reservation_products rp WHERE rp.product_name = '액티비티 패키지 - 스킨십 해변 트랙'
UNION ALL SELECT rp.id, '2026-08-12', 15, 12, 1 FROM reservation_products rp WHERE rp.product_name = '제주식 뷔페'
ON DUPLICATE KEY UPDATE
    total_capacity = VALUES(total_capacity),
    remaining_capacity = VALUES(remaining_capacity),
    is_available = VALUES(is_available);

-- reservations
INSERT INTO reservations
    (user_id, workation_id, product_id, reservation_code, start_date, end_date, headcount, quantity, total_amount, status, created_at)
SELECT
    @USER_ID,
    @WORKATION_ID,
    rp.id,
    'RES-U1-WK-30003',
    '2026-08-12',
    '2026-08-15',
    1,
    1,
    360000.00,
    'CONFIRMED',
    '2026-08-12 06:35:00'
FROM reservation_products rp
WHERE rp.product_name = '워케이션 스위트 스탠다드'
  AND NOT EXISTS (SELECT 1 FROM reservations r WHERE r.reservation_code = 'RES-U1-WK-30003')
UNION ALL
SELECT
    @USER_ID,
    @WORKATION_ID,
    rp.id,
    'RES-U1-OF-30001',
    '2026-08-12',
    '2026-08-13',
    1,
    1,
    36000.00,
    'CONFIRMED',
    '2026-08-12 06:36:00'
FROM reservation_products rp
WHERE rp.product_name = '오션 오피스 데일리 데스크'
  AND NOT EXISTS (SELECT 1 FROM reservations r WHERE r.reservation_code = 'RES-U1-OF-30001')
UNION ALL
SELECT
    @USER_ID,
    @WORKATION_ID,
    rp.id,
    'RES-U1-MR-30002',
    '2026-08-13',
    '2026-08-13',
    4,
    1,
    75000.00,
    'CONFIRMED',
    '2026-08-12 06:37:00'
FROM reservation_products rp
WHERE rp.product_name = '제주 라운지룸 미팅룸'
  AND NOT EXISTS (SELECT 1 FROM reservations r WHERE r.reservation_code = 'RES-U1-MR-30002')
UNION ALL
SELECT
    @USER_ID,
    @WORKATION_ID,
    rp.id,
    'RES-U1-ACT-30004',
    '2026-08-14',
    '2026-08-14',
    1,
    2,
    64000.00,
    'CONFIRMED',
    '2026-08-12 06:38:00'
FROM reservation_products rp
WHERE rp.product_name = '액티비티 패키지 - 스킨십 해변 트랙'
  AND NOT EXISTS (SELECT 1 FROM reservations r WHERE r.reservation_code = 'RES-U1-ACT-30004')
UNION ALL
SELECT
    @USER_ID,
    @WORKATION_ID,
    rp.id,
    'RES-U1-FOOD-30005',
    '2026-08-13',
    '2026-08-13',
    1,
    2,
    50000.00,
    'CANCELED',
    '2026-08-12 06:39:00'
FROM reservation_products rp
WHERE rp.product_name = '제주식 뷔페'
  AND NOT EXISTS (SELECT 1 FROM reservations r WHERE r.reservation_code = 'RES-U1-FOOD-30005')
ON DUPLICATE KEY UPDATE
    status = VALUES(status),
    total_amount = VALUES(total_amount),
    headcount = VALUES(headcount),
    quantity = VALUES(quantity),
    user_id = VALUES(user_id),
    workation_id = VALUES(workation_id),
    product_id = VALUES(product_id),
    reservation_code = VALUES(reservation_code),
    start_date = VALUES(start_date),
    end_date = VALUES(end_date);

-- reservation_daily_inventories
INSERT INTO reservation_daily_inventories
    (reservation_id, daily_inventory_id, reserved_count)
SELECT
    r.id,
    pdi.id,
    1
FROM reservations r
JOIN reservation_products rp
    ON rp.id = r.product_id
JOIN product_daily_inventories pdi
    ON pdi.product_id = rp.id
WHERE r.reservation_code = 'RES-U1-WK-30003'
  AND pdi.inventory_date BETWEEN '2026-08-12' AND '2026-08-14'
  AND NOT EXISTS (
    SELECT 1
    FROM reservation_daily_inventories rdi
    WHERE rdi.reservation_id = r.id
      AND rdi.daily_inventory_id = pdi.id
)
UNION ALL
SELECT
    r.id,
    pdi.id,
    1
FROM reservations r
JOIN reservation_products rp
    ON rp.id = r.product_id
JOIN product_daily_inventories pdi
    ON pdi.product_id = rp.id
WHERE r.reservation_code IN ('RES-U1-OF-30001', 'RES-U1-MR-30002', 'RES-U1-ACT-30004', 'RES-U1-FOOD-30005')
  AND pdi.inventory_date IN (
      CASE
          WHEN r.reservation_code = 'RES-U1-OF-30001' THEN '2026-08-13'
          WHEN r.reservation_code = 'RES-U1-MR-30002' THEN '2026-08-12'
          WHEN r.reservation_code = 'RES-U1-ACT-30004' THEN '2026-08-12'
          WHEN r.reservation_code = 'RES-U1-FOOD-30005' THEN '2026-08-12'
          ELSE NULL
      END
  )
  AND NOT EXISTS (
    SELECT 1
    FROM reservation_daily_inventories rdi
    WHERE rdi.reservation_id = r.id
      AND rdi.daily_inventory_id = pdi.id
);
-- 위 쿼리는 MySQL에서 UNION + CASE 기반으로 작성, reservation_code 매칭되는 inventory_date를 정확히 매핑
ON DUPLICATE KEY UPDATE
    reserved_count = VALUES(reserved_count);

-- reservation_cancels
INSERT INTO reservation_cancels
    (reservation_id, cancel_fee, refund_amount, canceled_at, refunded_at)
SELECT
    r.id,
    5000.00,
    45000.00,
    '2026-08-13 09:00:00',
    '2026-08-13 10:00:00'
FROM reservations r
WHERE r.reservation_code = 'RES-U1-FOOD-30005'
  AND NOT EXISTS (
    SELECT 1
    FROM reservation_cancels rc
    WHERE rc.reservation_id = r.id
)
ON DUPLICATE KEY UPDATE
    cancel_fee = VALUES(cancel_fee),
    refund_amount = VALUES(refund_amount),
    canceled_at = VALUES(canceled_at),
    refunded_at = VALUES(refunded_at);

-- reviews
INSERT INTO reviews
    (user_id, reservation_id, merchant_id, transaction_id, rating, content, status, atmosphere, created_at, updated_at, image_url)
SELECT
    @USER_ID,
    r.id,
    (SELECT id FROM merchants WHERE name = '서귀포 힐링 리조트' LIMIT 1),
    @TX_U1_WORK_ID,
    5,
    '숙소 위치와 시설이 워케이션에 최적화되어 있습니다.',
    'ACTIVE',
    NULL,
    '2026-08-16 10:00:00',
    '2026-08-16 10:00:00',
    NULL
FROM reservations r
WHERE r.reservation_code = 'RES-U1-WK-30003'
  AND NOT EXISTS (
    SELECT 1
    FROM reviews rv
    WHERE rv.user_id = @USER_ID
      AND rv.reservation_id = r.id
)
UNION ALL
SELECT
    @USER_ID,
    r.id,
    (SELECT id FROM merchants WHERE name = '제주 오션 오피스' LIMIT 1),
    NULL,
    4,
    '좌석 간격이 괜찮고 조용해요.',
    'ACTIVE',
    'QUIET',
    '2026-08-13 09:00:00',
    '2026-08-13 09:00:00',
    NULL
FROM reservations r
WHERE r.reservation_code = 'RES-U1-OF-30001'
  AND NOT EXISTS (
    SELECT 1
    FROM reviews rv
    WHERE rv.user_id = @USER_ID
      AND rv.reservation_id = r.id
)
ON DUPLICATE KEY UPDATE
    rating = VALUES(rating),
    content = VALUES(content),
    status = VALUES(status),
    atmosphere = VALUES(atmosphere),
    updated_at = VALUES(updated_at),
    image_url = VALUES(image_url);

-- bookmarks
INSERT INTO bookmarks
    (users_id, merchants_id, created_at, updated_at, is_deleted)
SELECT
    @USER_ID,
    (SELECT id FROM merchants WHERE name = '서귀포 힐링 리조트' LIMIT 1),
    '2026-08-12 06:40:00',
    '2026-08-12 06:40:00',
    NULL
UNION ALL
SELECT
    @USER_ID,
    (SELECT id FROM merchants WHERE name = '제주 오션 오피스' LIMIT 1),
    '2026-08-12 06:41:00',
    '2026-08-12 06:41:00',
    NULL
UNION ALL
SELECT
    @USER_ID,
    (SELECT id FROM merchants WHERE name = '제주 집중 코워킹' LIMIT 1),
    '2026-08-12 06:42:00',
    '2026-08-12 06:42:00',
    NULL
ON DUPLICATE KEY UPDATE
    users_id = VALUES(users_id),
    merchants_id = VALUES(merchants_id),
    updated_at = VALUES(updated_at),
    is_deleted = VALUES(is_deleted);

-- recommendation_requests
INSERT INTO recommendation_requests
    (user_id, workation_id, reference_merchant_id, secondary_reference_merchant_id, recommendation_type, reference_type, meal_type, created_at, reference_latitude, reference_longitude)
SELECT
    @USER_ID,
    @WORKATION_ID,
    (SELECT id FROM merchants WHERE name = '서귀포 힐링 리조트' LIMIT 1),
    NULL,
    'ACCOMMODATION',
    'REGION_ONLY',
    NULL,
    '2026-08-12 06:45:00',
    33.4900,
    126.5000
WHERE NOT EXISTS (SELECT 1 FROM recommendation_requests rr WHERE rr.user_id = @USER_ID AND rr.reference_merchant_id = (SELECT id FROM merchants WHERE name = '서귀포 힐링 리조트' LIMIT 1) AND rr.recommendation_type = 'ACCOMMODATION')
UNION ALL
SELECT
    @USER_ID,
    @WORKATION_ID,
    (SELECT id FROM merchants WHERE name = '제주 오션 오피스' LIMIT 1),
    NULL,
    'OFFICE',
    'AUTO_MERCHANT',
    NULL,
    '2026-08-12 06:45:10',
    33.4900,
    126.5000
WHERE NOT EXISTS (SELECT 1 FROM recommendation_requests rr WHERE rr.user_id = @USER_ID AND rr.reference_merchant_id = (SELECT id FROM merchants WHERE name = '제주 오션 오피스' LIMIT 1) AND rr.recommendation_type = 'OFFICE')
UNION ALL
SELECT
    @USER_ID,
    @WORKATION_ID,
    (SELECT id FROM merchants WHERE name = '제주 바람카페' LIMIT 1),
    (SELECT id FROM merchants WHERE name = '제주 집중 코워킹' LIMIT 1),
    'RESTAURANT',
    'AUTO_MIDPOINT',
    'DINNER',
    '2026-08-12 06:45:20',
    33.4930,
    126.4950
WHERE NOT EXISTS (SELECT 1 FROM recommendation_requests rr WHERE rr.user_id = @USER_ID AND rr.reference_merchant_id = (SELECT id FROM merchants WHERE name = '제주 바람카페' LIMIT 1) AND rr.recommendation_type = 'RESTAURANT')
UNION ALL
SELECT
    @USER_ID,
    @WORKATION_ID,
    (SELECT id FROM merchants WHERE name = '한라 액티브 스파크' LIMIT 1),
    NULL,
    'ACTIVITY',
    'AUTO_MERCHANT',
    NULL,
    '2026-08-12 06:45:30',
    33.4950,
    126.4960
WHERE NOT EXISTS (SELECT 1 FROM recommendation_requests rr WHERE rr.user_id = @USER_ID AND rr.reference_merchant_id = (SELECT id FROM merchants WHERE name = '한라 액티브 스파크' LIMIT 1) AND rr.recommendation_type = 'ACTIVITY')
ON DUPLICATE KEY UPDATE
    reference_merchant_id = VALUES(reference_merchant_id),
    secondary_reference_merchant_id = VALUES(secondary_reference_merchant_id),
    recommendation_type = VALUES(recommendation_type),
    reference_type = VALUES(reference_type),
    meal_type = VALUES(meal_type),
    created_at = VALUES(created_at),
    reference_latitude = VALUES(reference_latitude),
    reference_longitude = VALUES(reference_longitude);

-- recommendation_results
INSERT INTO recommendation_results
    (recommendation_request_id, merchant_id, price_score, preference_score, accessibility_score, rating_score, total_score, ranking, distance, calculated_at)
SELECT
    rr.id,
    (SELECT id FROM merchants WHERE name = '서귀포 힐링 리조트' LIMIT 1),
    93.00,
    92.00,
    85.00,
    90.00,
    90.00,
    1,
    0.00,
    '2026-08-12 06:46:00'
FROM recommendation_requests rr
WHERE rr.user_id = @USER_ID
  AND rr.recommendation_type = 'ACCOMMODATION'
  AND rr.reference_merchant_id = (SELECT id FROM merchants WHERE name = '서귀포 힐링 리조트' LIMIT 1)
  AND NOT EXISTS (
    SELECT 1
    FROM recommendation_results rrs
    WHERE rrs.recommendation_request_id = rr.id
      AND rrs.ranking = 1
)
UNION ALL
SELECT
    rr.id,
    (SELECT id FROM merchants WHERE name = '제주 오션 오피스' LIMIT 1),
    88.00,
    70.00,
    95.00,
    80.00,
    84.50,
    2,
    1.50,
    '2026-08-12 06:46:00'
FROM recommendation_requests rr
WHERE rr.user_id = @USER_ID
  AND rr.recommendation_type = 'ACCOMMODATION'
  AND rr.reference_merchant_id = (SELECT id FROM merchants WHERE name = '서귀포 힐링 리조트' LIMIT 1)
  AND NOT EXISTS (
    SELECT 1
    FROM recommendation_results rrs
    WHERE rrs.recommendation_request_id = rr.id
      AND rrs.ranking = 2
)
UNION ALL
SELECT
    rr.id,
    (SELECT id FROM merchants WHERE name = '제주 오션 오피스' LIMIT 1),
    95.00,
    98.00,
    90.00,
    92.00,
    95.00,
    1,
    0.10,
    '2026-08-12 06:46:20'
FROM recommendation_requests rr
WHERE rr.user_id = @USER_ID
  AND rr.recommendation_type = 'OFFICE'
  AND rr.reference_merchant_id = (SELECT id FROM merchants WHERE name = '제주 오션 오피스' LIMIT 1)
  AND NOT EXISTS (
    SELECT 1
    FROM recommendation_results rrs
    WHERE rrs.recommendation_request_id = rr.id
      AND rrs.ranking = 1
)
UNION ALL
SELECT
    rr.id,
    (SELECT id FROM merchants WHERE name = '제주 집중 코워킹' LIMIT 1),
    87.00,
    85.00,
    75.00,
    94.00,
    87.20,
    2,
    1.20,
    '2026-08-12 06:46:20'
FROM recommendation_requests rr
WHERE rr.user_id = @USER_ID
  AND rr.recommendation_type = 'OFFICE'
  AND rr.reference_merchant_id = (SELECT id FROM merchants WHERE name = '제주 오션 오피스' LIMIT 1)
  AND NOT EXISTS (
    SELECT 1
    FROM recommendation_results rrs
    WHERE rrs.recommendation_request_id = rr.id
      AND rrs.ranking = 2
)
UNION ALL
SELECT
    rr.id,
    (SELECT id FROM merchants WHERE name = '제주 바람카페' LIMIT 1),
    99.00,
    90.00,
    88.00,
    92.00,
    93.00,
    1,
    0.40,
    '2026-08-12 06:46:40'
FROM recommendation_requests rr
WHERE rr.user_id = @USER_ID
  AND rr.recommendation_type = 'RESTAURANT'
  AND rr.reference_merchant_id = (SELECT id FROM merchants WHERE name = '제주 바람카페' LIMIT 1)
  AND NOT EXISTS (
    SELECT 1
    FROM recommendation_results rrs
    WHERE rrs.recommendation_request_id = rr.id
      AND rrs.ranking = 1
)
UNION ALL
SELECT
    rr.id,
    (SELECT id FROM merchants WHERE name = '한라 액티브 스파크' LIMIT 1),
    91.00,
    80.00,
    96.00,
    89.00,
    89.00,
    1,
    2.10,
    '2026-08-12 06:47:00'
FROM recommendation_requests rr
WHERE rr.user_id = @USER_ID
  AND rr.recommendation_type = 'ACTIVITY'
  AND rr.reference_merchant_id = (SELECT id FROM merchants WHERE name = '한라 액티브 스파크' LIMIT 1)
  AND NOT EXISTS (
    SELECT 1
    FROM recommendation_results rrs
    WHERE rrs.recommendation_request_id = rr.id
      AND rrs.ranking = 1
)
ON DUPLICATE KEY UPDATE
    merchant_id = VALUES(merchant_id),
    price_score = VALUES(price_score),
    preference_score = VALUES(preference_score),
    accessibility_score = VALUES(accessibility_score),
    rating_score = VALUES(rating_score),
    total_score = VALUES(total_score),
    ranking = VALUES(ranking),
    distance = VALUES(distance),
    calculated_at = VALUES(calculated_at);
