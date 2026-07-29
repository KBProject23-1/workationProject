CREATE DATABASE workit;

USE workit;

CREATE TABLE `users` (
                         `id`            BIGINT          NOT NULL AUTO_INCREMENT COMMENT '회원 고유 번호(PK)',
                         `email`         VARCHAR(100)    NOT NULL                COMMENT '유저 이메일 (로그인 ID)',
                         `password`      VARCHAR(255)    NOT NULL                COMMENT '유저 비밀번호 (BCrypt 암호화)',
                         `name`          VARCHAR(50)     NOT NULL                COMMENT '유저 이름 (PASS 실명 암호화 가능)',
                         `phone_number`  VARCHAR(255)    NOT NULL                COMMENT '유저 핸드폰 번호 (양방향 암호화)',
                         `pass_ci`       VARCHAR(255)    NOT NULL                COMMENT '유저 PASS 인증 식별값 (1인1계정 검증)',
                         `birth_date`    DATE            NULL                    COMMENT '유저 생년월일 (YYYY-MM-DD)',
                         `status`        VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE, PENDING, BLOCKED, WITHDRAWN',
                         `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '유저 계정 생성 시간',

    -- 제약 조건 설정
                         PRIMARY KEY (`id`),
                         UNIQUE KEY `ux_users_email` (`email`),          -- 로그인 ID 중복 방지
                         UNIQUE KEY `ux_users_phone` (`phone_number`),   -- 휴대폰 번호 중복 가입 방지
                         UNIQUE KEY `ux_users_pass_ci` (`pass_ci`)       -- PASS CI값 중복 가입 방지 (원천 차단)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='회원 기본 계정 정보 테이블';

CREATE TABLE `user_profile` (
                                `id`            BIGINT          NOT NULL AUTO_INCREMENT COMMENT '프로필 고유 번호(PK)',
                                `user_id`       BIGINT          NOT NULL                COMMENT '회원 고유 번호 (FK, users.id 참조)',
                                `nickname`      VARCHAR(50)     NOT NULL                COMMENT '유저 닉네임',
                                `company_name`  VARCHAR(100)    NULL                    COMMENT '소속 회사명 (선택 입력 가능)',
                                `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '프로필 최초 생성 일시',
                                `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '프로필 최종 수정 일시',

    -- 제약 조건 설정
                                PRIMARY KEY (`id`),
                                UNIQUE KEY `ux_user_profile_user_id` (`user_id`),   -- 1:1 관계 강제 (한 유저당 프로필은 단 하나)
                                UNIQUE KEY `ux_user_profile_nickname` (`nickname`), -- 닉네임 중복 원천 차단
                                CONSTRAINT `fk_user_profile_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='회원 부가 프로필 정보 테이블 (비식별 관계)';

CREATE TABLE `user_device` (
                               `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '기기 등록 고유 번호(PK)',
                               `user_id`        BIGINT        NOT NULL                COMMENT '회원 고유 번호 (FK, users.id 참조)',
                               `device_id`      VARCHAR(100)  NOT NULL                COMMENT '브라우저 고유 식별 UUID',
                               `device_name`    VARCHAR(100)  NOT NULL                COMMENT '사용자 기기 정보 (예: Chrome / Windows)',
                               `pin_number`     CHAR(60)      NOT NULL                COMMENT '자산 거래용 6자리 핀번호 (BCrypt 암호화문)',
                               `fail_count`     INT           NOT NULL DEFAULT 0      COMMENT '핀번호 연속 실패 횟수 (5회 도달 시 잠금)',
                               `last_login_at`  DATETIME      NULL                    COMMENT '해당 기기 최종 로그인 일시',
                               `created_at`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '기기 최초 인증 등록 일시',

    -- 제약 조건 설정 (금융권 표준 자물쇠)
                               PRIMARY KEY (`id`),
                               UNIQUE KEY `ux_user_device_id` (`user_id`, `device_id`), --q 한 유저가 동일 기기를 중복 등록하는 것 방지
                               CONSTRAINT `fk_user_device_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='회원별 보안 핀번호 및 로그인 기기 관리 테이블 (비식별 관계)';

CREATE TABLE `terms` (
                         `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '약관 고유 번호(PK)',
                         `title`       VARCHAR(100)  NOT NULL                COMMENT '약관 제목',
                         `content`     TEXT          NOT NULL                COMMENT '약관 본문 상세 내용',
                         `required`    TINYINT(1)    NOT NULL                COMMENT '필수 여부 (0:선택, 1:필수)',
                         `created_at`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '약관 등록 일시',

                         PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='서비스 약관 종류 마스터 테이블';

CREATE TABLE `user_terms_agreements` (
                                         `id`         BIGINT    NOT NULL AUTO_INCREMENT COMMENT '동의 내역 고유 번호(PK)',
                                         `user_id`    BIGINT    NOT NULL                COMMENT '회원 고유 번호 (FK, users.id 참조)',
                                         `term_id`    BIGINT    NOT NULL                COMMENT '약관 고유 번호 (FK, terms.id 참조)',
                                         `agreed_at`  DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '약관 동의 일시',

    -- 제약 조건 설정
                                         PRIMARY KEY (`id`),
                                         CONSTRAINT `fk_user_terms_agreements_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
                                         CONSTRAINT `fk_user_terms_agreements_term_id` FOREIGN KEY (`term_id`) REFERENCES `terms` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='회원별 약관 동의 이력 매핑 테이블 (비식별 관계)';

CREATE TABLE `user_notification_settings` (
                                              `user_id`             BIGINT        NOT NULL                COMMENT '회원 고유번호 (PK 겸 FK, users.id 참조)',
                                              `system_notify`       TINYINT(1)    NOT NULL DEFAULT 1      COMMENT '시스템 알림 ON(1) / OFF(0)',
                                              `budget_warning`      TINYINT(1)    NOT NULL DEFAULT 1      COMMENT '예산 경고 알림 ON(1) / OFF(0)',
                                              `transfer_notify`     TINYINT(1)    NOT NULL DEFAULT 1      COMMENT '송금(입출금) 알림 ON(1) / OFF(0)',
                                              `payment_notify`      TINYINT(1)    NOT NULL DEFAULT 1      COMMENT '결제 알림 ON(1) / OFF(0)',
                                              `event_notify`        TINYINT(1)    NOT NULL DEFAULT 0      COMMENT '이벤트/광고 알림 ON(1) / OFF(0)',
                                              `reservation_notify`  TINYINT(1)    NOT NULL DEFAULT 1      COMMENT '예약 알림 ON(1) / OFF(0)',
                                              `review_notify`       TINYINT(1)    NOT NULL DEFAULT 1      COMMENT '리뷰 작성 요청 알림 ON(1) / OFF(0)',

    -- 제약 조건 설정
                                              PRIMARY KEY (`user_id`),
                                              CONSTRAINT `fk_user_notification_settings_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='회원별 카테고리별 알림 ON/OFF 설정 테이블 (식별 관계)';

CREATE TABLE `notification_histories` (
                                          `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '알림 이력 고유 번호(PK)',
                                          `user_id`     BIGINT        NOT NULL                COMMENT '회원 고유 번호 (FK, users.id 참조)',
                                          `type`        ENUM('SYSTEM', 'BUDGET', 'TRANSFER', 'PAYMENT', 'EVENT', 'RESERVATION', 'REVIEW') NOT NULL COMMENT '알림 카테고리 타입',
                                          `important`   TINYINT(1)    NOT NULL DEFAULT 0      COMMENT '중요 알림 여부 (0:일반, 1:중요)',
                                          `title`       VARCHAR(100)  NOT NULL                COMMENT '알림 제목',
                                          `content`     TEXT          NOT NULL                COMMENT '알림 본문 내용',
                                          `read`        TINYINT(1)    NOT NULL DEFAULT 0      COMMENT '읽음 여부 상태 (0:안읽음, 1:읽음)',
                                          `created_at`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '알림 수신 일시',

    -- 제약 조건 설정
                                          PRIMARY KEY (`id`),
                                          CONSTRAINT `fk_notification_histories_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자별 수신 알림 목록 이력 테이블 (비식별 관계)';

CREATE TABLE `restaurants` (
                               `id`	BIGINT	NOT NULL,
                               `id2`	BIGINT	NOT NULL,
                               `food_type`	ENUM( 'KOREAN', 'JAPANESE', 'CHINESE', 'WESTERN', 'CAFE', 'DESSERT', 'BAR' )	NOT NULL,
                               `price_level`	TINYINT	NOT NULL
);

CREATE TABLE `merchant_tags` (
                                 `merchant_id`	BIGINT	NOT NULL,
                                 `tag_id`	BIGINT	NOT NULL
);

CREATE TABLE `survey_questions` (
                                    `id`	BIGINT	NOT NULL,
                                    `question`	VARCHAR(200)	NOT NULL,
                                    `category`	ENUM( 'COMMON', 'ACCOMMODATION', 'RESTAURANT', 'OFFICE', 'ACTIVITY' )	NOT NULL
);

CREATE TABLE `expense_categories` (
                                      `id`	BIGINT	NOT NULL,
                                      `budget_type`	ENUM('WORK', 'PERSONAL')	NOT NULL	COMMENT '법인용/개인용 구분',
                                      `code`	VARCHAR(30)	NOT NULL	COMMENT '카테고리 코드',
                                      `name`	VARCHAR(30)	NOT NULL	COMMENT '화면 표시명',
                                      `description`	VARCHAR(100)	NULL	COMMENT '화면에 노출할 설명문',
                                      `is_default`	TINYINT(1)	NOT NULL	COMMENT '1=처음부터 표출, 0=＋ 목록에만',
                                      `is_deletable`	TINYINT(1)	NOT NULL	COMMENT '0=삭제 불가(기타)',
                                      `sort_order`	INT	NOT NULL	COMMENT '노출 순서',
                                      `user_id`	BIGINT	NULL	COMMENT 'NULL=시스템 기본, 값=사용자가 만든 것',
                                      `created_at`	TIMESTAMP	NULL	COMMENT '생성 일시'
);

CREATE TABLE `workations` (
                              `id`	BIGINT	NOT NULL	COMMENT '워케이션 고유 번호(PK)',
                              `user_id`	BIGINT	NOT NULL	COMMENT '회원 고유 번호(FK)',
                              `region_id`	BIGINT	NOT NULL	COMMENT '지역 코드',
                              `title`	VARCHAR(100)	NOT NULL	COMMENT '워케이션 등록 제목',
                              `start_date`	DATE	NOT NULL	COMMENT '워케이션 시작일',
                              `end_date`	DATE	NOT NULL	COMMENT '워케이션 종료일',
                              `business_budget_total`	DECIMAL(15,2)	NULL	COMMENT '법인 총예산',
                              `personal_budget_total`	DECIMAL(15,2)	NULL	COMMENT '개인 총예산',
                              `status`	ENUM('ACTIVE', 'SETTLED')	NOT NULL	DEFAULT 'ACTIVE'	COMMENT '최종페이지에서 워케이션 완료 누르면 첫페이지 기록에 쌓임',
                              `settled_at`	TIMESTAMP	NULL,
                              `created_at`	TIMESTAMP	NULL	DEFAULT CURRENT_TIMESTAMP	COMMENT '생성일',
                              `updated_at`	TIMESTAMP	NULL
);

CREATE TABLE `reviews` (
                           `review_id`	BIGINT	NOT NULL	COMMENT '리뷰 고유 번호(PK)',
                           `reservation_id`	BIGINT	NOT NULL	COMMENT '리뷰 작성한 예약 번호(FK)',
                           `rating`	TINYINT	NOT NULL	COMMENT '별점(1~5점)',
                           `content`	TEXT	NULL	COMMENT '리뷰 내용',
                           `status`	ENUM( 'ACTIVE', 'DELETED' )	NOT NULL	DEFAULT 'ACTIVE'	COMMENT '리뷰 상태
ACTIVE: 정상 등록된 리뷰
DELETED: 사용자가 삭제한 리뷰',
                           `created_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP	COMMENT '리뷰 작성 일시',
                           `updated_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP	COMMENT '리뷰 수정 일시',
                           `image_url`	VARCHAR(500)	NULL	COMMENT '리뷰 이미지'
);

CREATE TABLE `offices` (
                           `id`	BIGINT	NOT NULL,
                           `merchant_id`	BIGINT	NOT NULL,
                           `description`	TEXT	NULL,
                           `noise_level`	ENUM( 'QUIET', 'NORMAL', 'BUSY' )	NULL	DEFAULT 'NORMAL'
);

CREATE TABLE `bank_accounts` (
                                 `id`	BIGINT	PRIMARY KEY AUTO_INCREMENT,
                                 `user_id`	BIGINT	NOT NULL,
                                 `bank_code`	VARCHAR(10)	NOT NULL	COMMENT '은행 코드',
                                 `account_number`	VARCHAR(50)	NOT NULL	COMMENT '계좌번호',
                                 `product_name`	VARCHAR(100)	NOT NULL	COMMENT '계좌 이름',
                                 `balance`	DECIMAL(15, 2)	NULL	DEFAULT 0.00	COMMENT '계좌 잔액',
                                 `is_primary`	TINYINT(1)	NULL	DEFAULT 0	COMMENT '주거래 계좌 여부',
                                 `is_withdrawal_agreed`	TINYINT(1)	NULL	COMMENT '계좌 입출금 동의 여부',
                                 `withdrawal_agreed_at`	TIMESTAMP	NULL	COMMENT '오픈 뱅킹 약관 동의 시간',
                                 `balance_updated_at`	TIMESTAMP	NULL	DEFAULT CURRENT_TIMESTAMP	COMMENT '계좌 잔액 업데이트 시간',
                                 `is_deleted`	TINYINT(1)	NULL	DEFAULT 0	COMMENT '등록 계좌 삭제 여부'
);

CREATE TABLE `user_survey_answers` (
                                       `id`	BIGINT	NOT NULL,
                                       `survey_id`	BIGINT	NOT NULL,
                                       `option_id`	BIGINT	NOT NULL
);

CREATE TABLE `tags` (
                        `id`	BIGINT	NOT NULL,
                        `name`	VARCHAR(50)	NOT NULL
);

CREATE TABLE `reservation_daily_inventories` (
                                                 `id`	BIGINT	NOT NULL	COMMENT '예약 재고 연결 고유번호(PK)',
                                                 `reservation_id`	BIGINT	NOT NULL	COMMENT '예약 고유번호(FK)',
                                                 `daily_inventory_id`	BIGINT	NOT NULL	COMMENT '일별 재고 고유번호(FK)',
                                                 `reserved_count`	INT	NOT NULL	DEFAULT 1	COMMENT '예약한 객실 또는 좌석 수'
);

CREATE TABLE `transactions` (
                                `id`	BIGINT	PRIMARY KEY AUTO_INCREMENT	COMMENT '거래내역 고유 번호(PK)',
                                `user_id`	BIGINT	NOT NULL	COMMENT '회원 고유 번호(FK)',
                                `wallet_id`	BIGINT	NOT NULL	COMMENT '전자지갑 계좌 잔액',
                                `bank_account_id`	BIGINT	NULL	COMMENT '주거래 계좌 ID
충전 / 환불 기록용',
                                `card_id`	BIGINT	NULL	COMMENT '카드 고유 번호(FK)',
                                `workation_id`	BIGINT	NULL	COMMENT '워케이션 고유 번호(FK)',
                                `reservation_id`	BIGINT	NULL	COMMENT '예약과 관련된 거래인 경우 연결되는 예약 고유번호(FK)',
                                `merchant_id` BIGINT NULL COMMENT '결제한 가맹점 고유 번호(FK)',
                                `payment_source_type`	ENUM('CARD', 'WALLET')	NOT NULL	COMMENT '결제 수단',
                                `merchant_name`	VARCHAR(150)	NOT NULL	COMMENT '가맹점명',
                                `amount`	DECIMAL(15, 2)	NOT NULL	COMMENT '거래 금액',
                                `transaction_type`	ENUM('DEPOSIT', 'WITHDRAWAL', 'PAYMENT')	NOT NULL	COMMENT '입금/출금 유형',
                                `category_assigned`	VARCHAR(150)	NULL	DEFAULT '기타'	COMMENT '자동 분류된 지출 카테고리',
                                `is_business_expense`	TINYINT(1)	NULL	DEFAULT 1	COMMENT '업무 경비 여부',
                                `approved_number`	VARCHAR(50)	NULL	COMMENT '카드 승인 번호',
                                `transaction_number`	VARCHAR(50)	NULL	COMMENT '우리 서비스 거래번호',
                                `status`	ENUM('PAID', 'FAILED', 'CANCELED')	NULL	COMMENT '결제 상태',
                                `approved_at`	TIMESTAMP	NOT NULL	COMMENT '승인 시각',
                                `created_at`	TIMESTAMP	NULL	COMMENT '생성일시',
                                `updated_at`	TIMESTAMP	NULL	COMMENT '수정 일시',
                                `cancelled_at`	TIMESTAMP	NULL	COMMENT '결제 취소 일시'
);

CREATE TABLE `region` (
                          `id`	BIGINT	NOT NULL	COMMENT '지역 코드 번호(PK)',
                          `name`	VARCHAR(5)	NULL COMMENT '지역 이름'
);

CREATE TABLE `wallets` (
                           `id`	BIGINT	PRIMARY KEY AUTO_INCREMENT,
                           `user_id`	BIGINT	NOT NULL,
                           `balance`	DECIMAL(15, 2)	NULL	DEFAULT 0.00	COMMENT '시스템 계좌 충전금',
                           `updated_at`	TIMESTAMP	NULL	DEFAULT CURRENT_TIMESTAMP	COMMENT '지갑 업데이트 시간'
);

CREATE TABLE `reservations` (
                                `id`	BIGINT	NOT NULL	COMMENT '예약 고유번호(PK)',
                                `product_id`	BIGINT	NOT NULL	COMMENT '예약 상품 고유번호(FK)',
                                `reservation_code`	VARCHAR(50)	NOT NULL	COMMENT '예약번호',
                                `start_at`	DATETIME	NOT NULL	COMMENT '사용자가 실제로 예약한 시작 일시',
                                `end_at`	DATETIME	NULL	COMMENT '사용자가 실제로 예약한 종료 일시',
                                `headcount`	INT	NOT NULL	DEFAULT 1	COMMENT '예약 인원',
                                `total_amount`	DECIMAL(15, 2)	NOT NULL	COMMENT '예약 생성 시 계산되어 확정된 예약 총액',
                                `status`	ENUM( 'CONFIRMED', 'CANCELED', 'COMPLETED')	NOT NULL	DEFAULT 'CONFIRMED'	COMMENT '예약 상태',
                                `created_at`	TIMESTAMP	NULL	DEFAULT CURRENT_TIMESTAMP	COMMENT '예약 생성 시각',
                                `user_id`	BIGINT	NOT NULL	COMMENT '회원 고유 번호(FK)',
                                `quantity`	INT	NOT NULL	COMMENT '사용자가 예약한 상품 수량
(숙소 객실 수, 공유 오피스 좌석/공간 수)'
);

CREATE TABLE `merchants` (
                             `id`	BIGINT	NOT NULL,
                             `id2`	BIGINT	NOT NULL,
                             `name`	VARCHAR(150)	NOT NULL,
                             `category`	ENUM( 'ACCOMMODATION', 'RESTAURANT', 'OFFICE', 'ACTIVITY' )	NOT NULL,
                             `latitude`	DOUBLE	NOT NULL,
                             `longitude`	DOUBLE	NOT NULL,
                             `phone_number`	VARCHAR(20)	NULL,
                             `rating`	DECIMAL(2,1)	NULL	DEFAULT 0.0,
                             `thumbnail_url`	VARCHAR(255)	NULL
);

CREATE TABLE `product_daily_inventories` (
                                             `id`	BIGINT	NOT NULL	COMMENT '일별 재고 고유번호(PK)',
                                             `product_id`	BIGINT	NOT NULL	COMMENT '예약 상품 고유번호(FK)',
                                             `inventory_date`	DATE	NOT NULL	COMMENT '예약 가능 날짜',
                                             `total_capacity`	INT	NOT NULL	COMMENT '전체 재고 수',
                                             `remaining_capacity`	INT	NOT NULL	COMMENT '남은 재고 수',
                                             `is_available`	TINYINT(1)	NOT NULL	DEFAULT 1	COMMENT '해당 날짜 예약 접수 여부'
);

CREATE TABLE `cards` (
                         `id`	BIGINT	PRIMARY KEY AUTO_INCREMENT,
                         `user_id`	BIGINT	NOT NULL,
                         `card_company_code`	VARCHAR(10)	NOT NULL	COMMENT '카드 회사 코드(카드사 구분)',
                         `card_name`	VARCHAR(100)	NOT NULL	COMMENT '카드 이름(사용자가 지정한 카드 별칭 - 없을때 기본 카드 이름)',
                         `card_number`	VARCHAR(16)	NULL	COMMENT '카드 번호',
                         `card_classification`	ENUM('CREDIT', 'DEBIT')	NULL	COMMENT '신용 / 체크 구분용',
                         `card_type`	ENUM('WORK', 'PERSONAL')	NOT NULL	COMMENT '법인 / 개인 구분용',
                         `is_primary`	TINYINT(1)	NULL	DEFAULT 0	COMMENT '주 거래 카드 여부',
                         `is_agreed`	TINYINT(1)	NULL	COMMENT '카드 이용 동의 여부',
                         `created_at`	TIMESTAMP	NULL	DEFAULT CURRENT_TIMESTAMP	COMMENT '카드 등록 시간(카드 이용 동의 시간)',
                         `is_deleted`	TINYINT(1)	NULL	DEFAULT 0	COMMENT '등록 카드 삭제 여부',
                         `updated_at`	TIMESTAMP	NULL	COMMENT '수정 일시',
                         `deleted_at`	TIMESTAMP	NULL	COMMENT '삭제 일시'
);

CREATE TABLE `survey_options` (
                                  `id`	BIGINT	NOT NULL,
                                  `question_id`	BIGINT	NOT NULL,
                                  `tag_id`	BIGINT	NOT NULL,
                                  `option_name`	VARCHAR(20)	NULL,
                                  `weight`	TINYINT	NOT NULL
);

CREATE TABLE `user_surveys` (
                                `id`	BIGINT	NOT NULL,
                                `workation_id`	BIGINT	NOT NULL	COMMENT '워케이션 고유 번호(PK)',
                                `created_at`	TIMESTAMP	NULL
);

CREATE TABLE `accommodations` (
                                  `id`	BIGINT	NOT NULL,
                                  `id2`	BIGINT	NOT NULL,
                                  `accommodation_type`	ENUM( 'HOTEL', 'PENSION', 'RESORT', 'GUESTHOUSE', 'POOL_VILLA' )	NOT NULL,
                                  `description`	TEXT	NULL,
                                  `check_in_time`	TIME	NULL,
                                  `check_out_time`	TIME	NULL,
                                  `noise_level`	ENUM( 'QUIET', 'NORMAL', 'BUSY' )	NULL	DEFAULT 'NORMAL'
);

CREATE TABLE `user_category_rules` (
                                       `id`	BIGINT	NOT NULL,
                                       `user_id`	BIGINT	NOT NULL	COMMENT '회원 고유 번호(FK)',
                                       `merchant_id`	BIGINT	NOT NULL	COMMENT '가맹점 고유 번호(FK)',
                                       `expense_category_id`	BIGINT	NOT NULL	COMMENT '사용자가 지정한 카테고리(FK)',
                                       `budget_type`	ENUM('WORK','PERSONAL')	NOT NULL	COMMENT '법인용/개인용 구분',
                                       `created_at`	TIMESTAMP	NULL	COMMENT '최초 정정 일시',
                                       `updated_at`	TIMESTAMP	NULL	COMMENT '최종 정정 일시'
);

CREATE TABLE `activities` (
                              `id`	BIGINT	NOT NULL,
                              `merchant_id`	BIGINT	NOT NULL,
                              `activity_type`	ENUM( 'MARINE', 'SPORTS', 'HEALING', 'CULTURE', 'FESTIVAL', 'SHOPPING', 'ETC' )	NOT NULL,
                              `difficulty`	ENUM( 'EASY', 'NORMAL', 'HARD' )	NULL	DEFAULT 'NORMAL',
                              `duration_minutes`	INT	NULL
);

CREATE TABLE `user_device` (
                               `user_id`	BIGINT	NOT NULL	COMMENT '회원 고유 번호(FK)',
                               `device_id`	VARCHAR(100)	NULL	COMMENT '사용자 기기 id',
                               `pin_number`	VARCHAR(100)	NULL	COMMENT '사용자 핀번호',
                               `device_name`	VARCHAR(100)	NULL	COMMENT '사용자 기기 이름',
                               `Field`	TIMESTAMP	NULL	COMMENT '마지막 로그인 일자',
                               `fail_count`	INT	NULL	COMMENT '비밀번호 틀린 횟수'
);

CREATE TABLE `merchant_category_mappings` (
                                              `id`	BIGINT	NOT NULL,
                                              `budget_type`	ENUM('WORK','PERSONAL')	NOT NULL	COMMENT '법인용/개인용 구분',
                                              `merchant_category`	VARCHAR(30)	NOT NULL	COMMENT 'merchants.category 값',
                                              `expense_category_id`	BIGINT	NOT NULL	COMMENT '지출 카테고리 고유 번호(FK)'
);

CREATE TABLE `budgets` (
                           `id`	BIGINT	NOT NULL,
                           `workation_id`	BIGINT	NOT NULL	COMMENT '워케이션 고유 번호(FK)',
                           `expense_category_id`	BIGINT	NOT NULL,
                           `budget_type`	ENUM('PERSONAL', 'WORK')	NOT NULL	COMMENT '예산 유형(법인/개인)',
                           `target_amount`	DECIMAL(15, 2)	NOT NULL	COMMENT '카테고리 배정 예산',
                           `created_at`	TIMESTAMP	NULL,
                           `updated_at`	TIMESTAMP	NULL
);

CREATE TABLE `reservation_products` (
                                        `id`	BIGINT	NOT NULL	COMMENT '예약 상품 고유번호(PK)',
                                        `product_name`	VARCHAR(150)	NOT NULL	COMMENT '예약 상품명',
                                        `description`	TEXT	NULL	COMMENT '예약 상품 설명',
                                        `product_detail_type`	ENUM('ROOM', 'OFFICE_SEAT', 'MEETING_ROOM')	NOT NULL	COMMENT '상품 세부 유형
ROOM: 숙소(객실)
SEAT, MEETING_ROOM(공유오피스)',
                                        `max_headcount`	INT	NOT NULL	DEFAULT 1	COMMENT '상품 최대 수용 인원',
                                        `bed_type`	VARCHAR(50)	NULL	COMMENT '침대 유형',
                                        `bed_count`	INT	NULL	COMMENT '객실 내 침대 개수',
                                        `price_per_unit`	DECIMAL(15, 2)	NOT NULL	COMMENT '상품 기준 단가',
                                        `price_unit`	ENUM('PER_DAY',    'PER_PERSON')	NOT NULL	COMMENT '가격 계산 단위
일별, 인원수별',
                                        `merchant_id`	BIGINT	NOT NULL
);

CREATE TABLE `card_companies` (
                                  `code`	VARCHAR(10)	NOT NULL	COMMENT '카드사 코드 (PK)',
                                  `name`	VARCHAR(50)	NOT NULL	COMMENT '카드사명',
                                  `logo_url`	VARCHAR(255)	NULL	COMMENT '카드사 로고 이미지 경로'
);

CREATE TABLE `workation_expenses` (
                                      `id`	BIGINT	NOT NULL,
                                      `workation_id`	BIGINT	NOT NULL	COMMENT '워케이션 고유 번호(FK)',
                                      `transaction_id`	BIGINT	NULL	COMMENT '거래내역 고유 번호(FK)
앱 내 결제만 값 존재',
                                      `merchant_id`	BIGINT	NULL	COMMENT '가맹점 고유 번호(FK)
자동분류 근거',
                                      `expense_category_id`	BIGINT	NOT NULL	COMMENT '지출 카테고리 고유 번호(FK)',
                                      `card_id`	BIGINT	NULL	COMMENT 'card_id FK',
                                      `budget_type`	ENUM('WORK','PERSONAL')	NOT NULL	COMMENT '업무/개인 경비 구분. 정산 필터 기준',
                                      `amount`	DECIMAL(15,2)	NOT NULL	COMMENT '지출 금액',
                                      `merchant_name`	VARCHAR(150)	NULL	COMMENT '가맹점명 (외부결제·수기 입력용)',
                                      `spent_at`	TIMESTAMP	NOT NULL	COMMENT '지출 일시',
                                      `memo`	VARCHAR(255)	NULL	COMMENT '지출 간단 메모',
                                      `is_auto_categorized`	TINYINT(1)	NOT NULL	COMMENT '1=자동분류 유지
0=사용자 확정',
                                      `created_at`	TIMESTAMP	NOT NULL	DEFAULT CURRENT_TIMESTAMP	COMMENT '생성 일시',
                                      `updated_at`	TIMESTAMP	NULL	COMMENT '수정 일시'
);

CREATE TABLE `reservation_cancels` (
                                       `id`	BIGINT	NOT NULL	COMMENT '예약 취소 고유번호(PK)',
                                       `reservation_id`	BIGINT	NOT NULL	COMMENT '예약 고유번호(FK)',
                                       `cancel_fee`	DECIMAL(15, 2)	NOT NULL	DEFAULT 0	COMMENT '취소 수수료',
                                       `refund_amount`	DECIMAL(15, 2)	NOT NULL	DEFAULT 0	COMMENT '환불 금액',
                                       `canceled_at`	TIMESTAMP	NULL	DEFAULT CURRENT_TIMESTAMP	COMMENT '취소 시각',
                                       `refunded_at`	TIMESTAMP	NULL	COMMENT '환불 완료 시각'
);

CREATE TABLE `user_category_labels` (
                                        `user_id`	BIGINT	NOT NULL	COMMENT '회원 고유 번호(PK, FK)',
                                        `expense_category_id`	BIGINT	NOT NULL	COMMENT '지출 카테고리 고유 번호(PK, FK)',
                                        `custom_name`	VARCHAR(50)	NOT NULL	COMMENT '사용자가 지정한 표시명',
                                        `updated_at`	TIMESTAMP	NULL	COMMENT '수정 일시'
);

CREATE TABLE `banks` (
                         `code`	VARCHAR(10)	NOT NULL	COMMENT '은행 코드 (PK)',
                         `name`	VARCHAR(50)	NOT NULL	COMMENT '은행명',
                         `logo_url`	VARCHAR(255)	NULL	COMMENT '은행 로고 이미지 경로'
);

CREATE TABLE `linkable_accounts` (
                                     `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '연동 가능 계좌 고유 번호(PK)',
                                     `user_id` BIGINT NOT NULL COMMENT '회원 고유 번호(FK)',
                                     `bank_code` VARCHAR(10) NOT NULL COMMENT '은행 코드(FK)',
                                     `account_number` VARCHAR(50) NOT NULL COMMENT '계좌번호(Mock)',
                                     `product_name` VARCHAR(100) NOT NULL COMMENT '계좌 상품명',
                                     `is_linked` TINYINT(1) NULL DEFAULT 0 COMMENT '이미 연동(등록) 완료했는지 여부'
);

CREATE TABLE `linkable_cards` (
                                  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '연동 가능 카드 고유 번호(PK)',
                                  `user_id` BIGINT NOT NULL COMMENT '회원 고유 번호(FK)',
                                  `card_company_code` VARCHAR(10) NOT NULL COMMENT '카드사 코드(FK)',
                                  `card_number` VARCHAR(16) NOT NULL COMMENT '카드번호(Mock)',
                                  `card_name` VARCHAR(100) NOT NULL COMMENT '카드 기본 이름(카드사 제공명)',
                                  `card_classification` ENUM('CREDIT', 'DEBIT') NULL COMMENT '신용/체크 구분',
                                  `card_type` ENUM('WORK', 'PERSONAL') NOT NULL COMMENT '법인/개인 구분',
                                  `is_linked` TINYINT(1) NULL DEFAULT 0 COMMENT '이미 연동 완료했는지 여부'
);


ALTER TABLE `restaurants` ADD CONSTRAINT `PK_RESTAURANTS` PRIMARY KEY (
                                                                       `id`,
                                                                       `id2`
    );

ALTER TABLE `merchant_tags` ADD CONSTRAINT `PK_MERCHANT_TAGS` PRIMARY KEY (
                                                                           `merchant_id`,
                                                                           `tag_id`
    );

ALTER TABLE `survey_questions` ADD CONSTRAINT `PK_SURVEY_QUESTIONS` PRIMARY KEY (
                                                                                 `id`
    );

ALTER TABLE `expense_categories` ADD CONSTRAINT `PK_EXPENSE_CATEGORIES` PRIMARY KEY (
                                                                                     `id`
    );

ALTER TABLE `workations` ADD CONSTRAINT `PK_WORKATIONS` PRIMARY KEY (
                                                                     `id`
    );

ALTER TABLE `reviews` ADD CONSTRAINT `PK_REVIEWS` PRIMARY KEY (
                                                               `review_id`
    );

ALTER TABLE `offices` ADD CONSTRAINT `PK_OFFICES` PRIMARY KEY (
                                                               `id`,
                                                               `merchant_id`
    );

ALTER TABLE `user_survey_answers` ADD CONSTRAINT `PK_USER_SURVEY_ANSWERS` PRIMARY KEY (
                                                                                       `id`
    );

ALTER TABLE `tags` ADD CONSTRAINT `PK_TAGS` PRIMARY KEY (
                                                         `id`
    );

ALTER TABLE `reservation_daily_inventories` ADD CONSTRAINT `PK_RESERVATION_DAILY_INVENTORIES` PRIMARY KEY (
                                                                                                           `id`
    );

ALTER TABLE `region` ADD CONSTRAINT `PK_REGION` PRIMARY KEY (
                                                             `id`
    );


ALTER TABLE `reservations` ADD CONSTRAINT `PK_RESERVATIONS` PRIMARY KEY (
                                                                         `id`
    );

ALTER TABLE `merchants` ADD CONSTRAINT `PK_MERCHANTS` PRIMARY KEY (
                                                                   `id`
    );

ALTER TABLE `product_daily_inventories` ADD CONSTRAINT `PK_PRODUCT_DAILY_INVENTORIES` PRIMARY KEY (
                                                                                                   `id`
    );

ALTER TABLE `survey_options` ADD CONSTRAINT `PK_SURVEY_OPTIONS` PRIMARY KEY (
                                                                             `id`
    );

ALTER TABLE `user_surveys` ADD CONSTRAINT `PK_USER_SURVEYS` PRIMARY KEY (
                                                                         `id`
    );

ALTER TABLE `accommodations` ADD CONSTRAINT `PK_ACCOMMODATIONS` PRIMARY KEY (
                                                                             `id`,
                                                                             `id2`
    );

ALTER TABLE `user_category_rules` ADD CONSTRAINT `PK_USER_CATEGORY_RULES` PRIMARY KEY (
                                                                                       `id`
    );

ALTER TABLE `activities` ADD CONSTRAINT `PK_ACTIVITIES` PRIMARY KEY (
                                                                     `id`,
                                                                     `merchant_id`
    );

ALTER TABLE `merchant_category_mappings` ADD CONSTRAINT `PK_MERCHANT_CATEGORY_MAPPINGS` PRIMARY KEY (
                                                                                                     `id`
    );

ALTER TABLE `budgets` ADD CONSTRAINT `PK_BUDGETS` PRIMARY KEY (
                                                               `id`
    );

ALTER TABLE `reservation_products` ADD CONSTRAINT `PK_RESERVATION_PRODUCTS` PRIMARY KEY (
                                                                                         `id`
    );

ALTER TABLE `workation_expenses` ADD CONSTRAINT `PK_WORKATION_EXPENSES` PRIMARY KEY (
                                                                                     `id`
    );

ALTER TABLE `reservation_cancels` ADD CONSTRAINT `PK_RESERVATION_CANCELS` PRIMARY KEY (
                                                                                       `id`
    );

ALTER TABLE `user_category_labels` ADD CONSTRAINT `PK_USER_CATEGORY_LABELS` PRIMARY KEY (
                                                                                         `user_id`,
                                                                                         `expense_category_id`
    );

ALTER TABLE `restaurants` ADD CONSTRAINT `FK_merchants_TO_restaurants_1` FOREIGN KEY (
                                                                                      `id2`
    )
    REFERENCES `merchants` (
                            `id`
        );

ALTER TABLE `merchant_tags` ADD CONSTRAINT `FK_merchants_TO_merchant_tags_1` FOREIGN KEY (
                                                                                          `merchant_id`
    )
    REFERENCES `merchants` (
                            `id`
        );

ALTER TABLE `merchant_tags` ADD CONSTRAINT `FK_tags_TO_merchant_tags_1` FOREIGN KEY (
                                                                                     `tag_id`
    )
    REFERENCES `tags` (
                       `id`
        );

ALTER TABLE `offices` ADD CONSTRAINT `FK_merchants_TO_offices_1` FOREIGN KEY (
                                                                              `merchant_id`
    )
    REFERENCES `merchants` (
                            `id`
        );

ALTER TABLE `accommodations` ADD CONSTRAINT `FK_merchants_TO_accommodations_1` FOREIGN KEY (
                                                                                            `id2`
    )
    REFERENCES `merchants` (
                            `id`
        );

ALTER TABLE `activities` ADD CONSTRAINT `FK_merchants_TO_activities_1` FOREIGN KEY (
                                                                                    `merchant_id`
    )
    REFERENCES `merchants` (
                            `id`
        );

ALTER TABLE `user_category_labels` ADD CONSTRAINT `FK_users_TO_user_category_labels_1` FOREIGN KEY (
                                                                                                    `user_id`
    )
    REFERENCES `users` (
                        `id`
        );

ALTER TABLE `user_category_labels` ADD CONSTRAINT `FK_expense_categories_TO_user_category_labels_1` FOREIGN KEY (
                                                                                                                 `expense_category_id`
    )
    REFERENCES `expense_categories` (
                                     `id`
        );

ALTER TABLE `transactions` ADD CONSTRAINT `FK_wallets_TO_transactions` FOREIGN KEY (`wallet_id`) REFERENCES wallets(`id`);
ALTER TABLE `transactions` ADD CONSTRAINT `FK_cards_TO_transactions` FOREIGN KEY (`card_id`) REFERENCES cards(`id`);
ALTER TABLE `transactions` ADD CONSTRAINT `FK_bank_accounts_TO_transactions` FOREIGN KEY (`bank_account_id`) REFERENCES bank_accounts(`id`);
ALTER TABLE `transactions` ADD CONSTRAINT `FK_merchants_TO_transactions` FOREIGN KEY (`merchant_id`) REFERENCES merchants(`id`);

-- 1. 참조 대상 테이블들에 기본키(PK) 추가
ALTER TABLE `card_companies` ADD CONSTRAINT `PK_CARD_COMPANIES` PRIMARY KEY (`code`);
ALTER TABLE `banks` ADD CONSTRAINT `PK_BANKS` PRIMARY KEY (`code`);

-- 2. 기존 외래키(FK) 설정 재실행
ALTER TABLE `cards` ADD CONSTRAINT `FK_card_companies_TO_cards` FOREIGN KEY (`card_company_code`) REFERENCES card_companies(`code`);
