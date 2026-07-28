DROP DATABASE IF EXISTS `workit`;
CREATE DATABASE `workit` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `workit`;

SET FOREIGN_KEY_CHECKS = 0;


-- =====================================================================
-- 1. 공통 코드
-- =====================================================================

CREATE TABLE `banks` (
    `code`     VARCHAR(10)  NOT NULL COMMENT '은행 코드(PK)',
    `name`     VARCHAR(50)  NOT NULL COMMENT '은행명',
    `logo_url` VARCHAR(255)     NULL COMMENT '은행 로고 이미지 경로',
    PRIMARY KEY (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='은행 코드';

CREATE TABLE `card_companies` (
    `code`     VARCHAR(10)  NOT NULL COMMENT '카드사 코드(PK)',
    `name`     VARCHAR(50)  NOT NULL COMMENT '카드사명',
    `logo_url` VARCHAR(255)     NULL COMMENT '카드사 로고 이미지 경로',
    PRIMARY KEY (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='카드사 코드';

CREATE TABLE `region` (
    `id`   BIGINT      NOT NULL AUTO_INCREMENT COMMENT '지역 코드 번호(PK)',
    `name` VARCHAR(50) NOT NULL                COMMENT '지역 이름',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_region_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='워케이션 거점 지역';

CREATE TABLE `tags` (
    `id`   BIGINT      NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(50) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tags_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='성향 태그';

CREATE TABLE `terms` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `title`       VARCHAR(150)     NULL,
    `content`     TEXT             NULL,
    `is_required` TINYINT(1)       NULL COMMENT '필수 약관 여부',
    `created_at`  TIMESTAMP        NULL COMMENT '약관 등록일',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='약관';


-- =====================================================================
-- 2. 회원 · 인증
-- =====================================================================

CREATE TABLE `users` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '회원 고유 번호(PK)',
    `email`        VARCHAR(100) NOT NULL COMMENT '유저 이메일',
    `password`     VARCHAR(255) NOT NULL COMMENT '유저 비밀번호',
    `name`         VARCHAR(50)  NOT NULL COMMENT '유저 이름',
    `phone_number` VARCHAR(255) NOT NULL COMMENT '유저 핸드폰 번호',
    `pass_ci`      VARCHAR(255) NOT NULL COMMENT '유저 PASS 인증 식별값',
    `birth_date`   DATE             NULL COMMENT '유저 생년월일',
    `nickname`     VARCHAR(100)     NULL COMMENT '유저 닉네임',
    `company_info` VARCHAR(100)     NULL COMMENT '유저 회사 정보',
    `created_at`   TIMESTAMP        NULL DEFAULT CURRENT_TIMESTAMP COMMENT '계정 생성 시간',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_users_email` (`email`),
    UNIQUE KEY `uk_users_pass_ci` (`pass_ci`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='회원';

CREATE TABLE `user_device` (
    `user_id`       BIGINT       NOT NULL COMMENT '회원 고유 번호(FK)',
    `device_id`     VARCHAR(100)     NULL COMMENT '사용자 기기 id',
    `pin_number`    VARCHAR(100)     NULL COMMENT '사용자 핀번호',
    `device_name`   VARCHAR(100)     NULL COMMENT '사용자 기기 이름',
    `last_login_at` TIMESTAMP        NULL COMMENT '마지막 로그인 일자',
    `fail_count`    INT              NULL COMMENT '비밀번호 틀린 횟수',
    PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='사용자 기기';

CREATE TABLE `user_terms_agreements` (
    `id`        BIGINT    NOT NULL AUTO_INCREMENT,
    `user_id`   BIGINT    NOT NULL COMMENT '회원 고유 번호(FK)',
    `term_id`   BIGINT    NOT NULL,
    `agreed_at` TIMESTAMP     NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_uta_user_term` (`user_id`, `term_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='약관 동의 이력';

CREATE TABLE `user_notification_settings` (
    `user_id`               BIGINT     NOT NULL COMMENT '회원 고유번호(PK, FK)',
    `is_system_notify`      TINYINT(1)     NULL DEFAULT 1 COMMENT '시스템 알림',
    `is_budget_warning`     TINYINT(1)     NULL DEFAULT 1 COMMENT '예산 경고 알림',
    `is_transfer_notify`    TINYINT(1)     NULL DEFAULT 1 COMMENT '입출금 알림',
    `is_payment_notify`     TINYINT(1)     NULL DEFAULT 1 COMMENT '결제 알림',
    `is_event_notify`       TINYINT(1)     NULL DEFAULT 0 COMMENT '이벤트 알림',
    `is_reservation_notify` TINYINT(1)     NULL DEFAULT 1 COMMENT '예약 알림',
    `is_review_notify`      TINYINT(1)     NULL COMMENT '리뷰 작성 요청 알림',
    PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='알림 설정';

CREATE TABLE `notification_histories` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`      BIGINT       NOT NULL,
    `type`         ENUM('SYSTEM','BUDGET','WALLET','PAYMENT','EVENT','RESERVATION','REVIEW') NOT NULL COMMENT '알림 타입',
    `is_important` TINYINT(1)       NULL DEFAULT 0 COMMENT '중요 알림 탭 분류용',
    `title`        VARCHAR(150) NOT NULL COMMENT '알림 제목',
    `content`      TEXT         NOT NULL COMMENT '알림 내용',
    `is_read`      TINYINT(1)       NULL DEFAULT 0 COMMENT '읽음 처리 분류용',
    `created_at`   TIMESTAMP        NULL DEFAULT CURRENT_TIMESTAMP COMMENT '알림 생성 시간',
    PRIMARY KEY (`id`),
    KEY `idx_nh_user_created` (`user_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='알림 이력';


-- =====================================================================
-- 3. 지갑 · 결제
-- =====================================================================

CREATE TABLE `wallets` (
    `id`         BIGINT        NOT NULL AUTO_INCREMENT,
    `user_id`    BIGINT        NOT NULL,
    `balance`    DECIMAL(15,2)     NULL DEFAULT 0.00 COMMENT '시스템 계좌 충전금',
    `updated_at` TIMESTAMP         NULL DEFAULT CURRENT_TIMESTAMP COMMENT '지갑 업데이트 시간',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_wallets_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='전자지갑';

CREATE TABLE `bank_accounts` (
    `id`                   BIGINT        NOT NULL AUTO_INCREMENT,
    `user_id`              BIGINT        NOT NULL,
    `bank_code`            VARCHAR(10)   NOT NULL COMMENT '은행 코드',
    `account_number`       VARCHAR(50)   NOT NULL COMMENT '계좌번호',
    `product_name`         VARCHAR(100)  NOT NULL COMMENT '계좌 이름',
    `balance`              DECIMAL(15,2)     NULL DEFAULT 0.00 COMMENT '계좌 잔액',
    `is_primary`           TINYINT(1)        NULL DEFAULT 0 COMMENT '주거래 계좌 여부',
    `is_withdrawl_agreed`  TINYINT(1)        NULL COMMENT '계좌 입출금 동의 여부',
    `withdrawl_agreed_at`  TIMESTAMP         NULL COMMENT '오픈뱅킹 약관 동의 시간',
    `balance_updated_at`   TIMESTAMP         NULL DEFAULT CURRENT_TIMESTAMP COMMENT '잔액 갱신 시간',
    `is_deleted`           TINYINT(1)        NULL DEFAULT 0 COMMENT '삭제 여부',
    PRIMARY KEY (`id`),
    KEY `idx_ba_user` (`user_id`),
    KEY `idx_ba_bank` (`bank_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='연결 계좌';

CREATE TABLE `cards` (
    `id`                  BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`             BIGINT       NOT NULL,
    `card_company_code`   VARCHAR(10)  NOT NULL COMMENT '카드 회사 코드',
    `card_name`           VARCHAR(100) NOT NULL COMMENT '카드 이름',
    `number`              VARCHAR(16)      NULL COMMENT '카드 번호',
    `card_classification` ENUM('CREDIT','DEBIT')        NULL COMMENT '신용 / 체크',
    `card_type`           ENUM('CORPORATE','PERSONAL') NOT NULL COMMENT '법인 / 개인',
    `is_primary`          TINYINT(1)       NULL DEFAULT 0 COMMENT '주 거래 카드 여부',
    `is_agreed`           TINYINT(1)       NULL COMMENT '카드 이용 동의 여부',
    `created_at`          TIMESTAMP        NULL DEFAULT CURRENT_TIMESTAMP COMMENT '카드 등록 시간',
    `is_deleted`          TINYINT(1)       NULL DEFAULT 0 COMMENT '삭제 여부',
    `updated_at`          TIMESTAMP        NULL COMMENT '수정 일시',
    `deleted_at`          TIMESTAMP        NULL COMMENT '삭제 일시',
    PRIMARY KEY (`id`),
    KEY `idx_cards_user_type` (`user_id`, `card_type`, `is_deleted`),
    KEY `idx_cards_company` (`card_company_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='등록 카드';


-- =====================================================================
-- 4. 가맹점 · 지역
-- =====================================================================

CREATE TABLE `merchants` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `id2`           BIGINT       NOT NULL COMMENT '지역 코드(FK → region.id)',
    `name`          VARCHAR(150) NOT NULL,
    `category`      ENUM('ACCOMMODATION','RESTAURANT','OFFICE','ACTIVITY') NOT NULL COMMENT '가맹점 업종',
    `latitude`      DOUBLE       NOT NULL,
    `longitude`     DOUBLE       NOT NULL,
    `phone_number`  VARCHAR(20)      NULL,
    `rating`        DECIMAL(2,1)     NULL DEFAULT 0.0,
    `thumbnail_url` VARCHAR(255)     NULL,
    PRIMARY KEY (`id`),
    KEY `idx_merchants_region_cat` (`id2`, `category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='가맹점';

CREATE TABLE `restaurants` (
    `id`          BIGINT  NOT NULL AUTO_INCREMENT,
    `id2`         BIGINT  NOT NULL COMMENT '가맹점 고유번호(FK → merchants.id)',
    `food_type`   ENUM('KOREAN','JAPANESE','CHINESE','WESTERN','CAFE','DESSERT','BAR') NOT NULL,
    `price_level` TINYINT NOT NULL,
    PRIMARY KEY (`id`, `id2`),
    KEY `idx_restaurants_merchant` (`id2`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='음식점 상세';

CREATE TABLE `accommodations` (
    `id`                 BIGINT NOT NULL AUTO_INCREMENT,
    `id2`                BIGINT NOT NULL COMMENT '가맹점 고유번호(FK → merchants.id)',
    `accommodation_type` ENUM('HOTEL','PENSION','RESORT','GUESTHOUSE','POOL_VILLA') NOT NULL,
    `description`        TEXT       NULL,
    `check_in_time`      TIME       NULL,
    `check_out_time`     TIME       NULL,
    `noise_level`        ENUM('QUIET','NORMAL','BUSY') NULL DEFAULT 'NORMAL',
    PRIMARY KEY (`id`, `id2`),
    KEY `idx_accommodations_merchant` (`id2`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='숙소 상세';

CREATE TABLE `offices` (
    `id`          BIGINT NOT NULL AUTO_INCREMENT,
    `merchant_id` BIGINT NOT NULL,
    `description` TEXT       NULL,
    `noise_level` ENUM('QUIET','NORMAL','BUSY') NULL DEFAULT 'NORMAL',
    PRIMARY KEY (`id`, `merchant_id`),
    KEY `idx_offices_merchant` (`merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='공유오피스 상세';

CREATE TABLE `activities` (
    `id`               BIGINT NOT NULL AUTO_INCREMENT,
    `merchant_id`      BIGINT NOT NULL,
    `activity_type`    ENUM('MARINE','SPORTS','HEALING','CULTURE','FESTIVAL','SHOPPING','ETC') NOT NULL,
    `difficulty`       ENUM('EASY','NORMAL','HARD') NULL DEFAULT 'NORMAL',
    `duration_minutes` INT NULL,
    PRIMARY KEY (`id`, `merchant_id`),
    KEY `idx_activities_merchant` (`merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='액티비티 상세';

CREATE TABLE `merchant_tags` (
    `merchant_id` BIGINT NOT NULL,
    `tag_id`      BIGINT NOT NULL,
    PRIMARY KEY (`merchant_id`, `tag_id`),
    KEY `idx_mt_tag` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='가맹점 성향 태그';


-- =====================================================================
-- 5. 예약 · 리뷰
-- =====================================================================

CREATE TABLE `reservation_products` (
    `id`                  BIGINT        NOT NULL AUTO_INCREMENT COMMENT '예약 상품 고유번호(PK)',
    `product_name`        VARCHAR(150)  NOT NULL COMMENT '예약 상품명',
    `description`         TEXT              NULL COMMENT '예약 상품 설명',
    `product_detail_type` ENUM('ROOM','OFFICE_SEAT','MEETING_ROOM') NOT NULL COMMENT '상품 세부 유형',
    `max_headcount`       INT           NOT NULL DEFAULT 1 COMMENT '최대 수용 인원',
    `bed_type`            VARCHAR(50)       NULL COMMENT '침대 유형',
    `bed_count`           INT               NULL COMMENT '침대 개수',
    `price_per_unit`      DECIMAL(15,2) NOT NULL COMMENT '기준 단가',
    `price_unit`          ENUM('PER_DAY','PER_PERSON') NOT NULL COMMENT '가격 계산 단위',
    `merchant_id`         BIGINT        NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_rp_merchant` (`merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='예약 상품';

CREATE TABLE `product_daily_inventories` (
    `id`                 BIGINT     NOT NULL AUTO_INCREMENT COMMENT '일별 재고 고유번호(PK)',
    `product_id`         BIGINT     NOT NULL COMMENT '예약 상품 고유번호(FK)',
    `inventory_date`     DATE       NOT NULL COMMENT '예약 가능 날짜',
    `total_capacity`     INT        NOT NULL COMMENT '전체 재고 수',
    `remaining_capacity` INT        NOT NULL COMMENT '남은 재고 수',
    `is_available`       TINYINT(1) NOT NULL DEFAULT 1 COMMENT '접수 여부',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_pdi_product_date` (`product_id`, `inventory_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='상품 일별 재고';

CREATE TABLE `reservations` (
    `id`               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '예약 고유번호(PK)',
    `product_id`       BIGINT        NOT NULL COMMENT '예약 상품 고유번호(FK)',
    `reservation_code` VARCHAR(50)   NOT NULL COMMENT '예약번호',
    `start_at`         DATETIME      NOT NULL COMMENT '예약 시작 일시',
    `end_at`           DATETIME          NULL COMMENT '예약 종료 일시',
    `headcount`        INT           NOT NULL DEFAULT 1 COMMENT '예약 인원',
    `total_amount`     DECIMAL(15,2) NOT NULL COMMENT '예약 총액',
    `status`           ENUM('CONFIRMED','CANCELED','COMPLETED') NOT NULL DEFAULT 'CONFIRMED' COMMENT '예약 상태',
    `created_at`       TIMESTAMP         NULL DEFAULT CURRENT_TIMESTAMP COMMENT '예약 생성 시각',
    `user_id`          BIGINT        NOT NULL COMMENT '회원 고유 번호(FK)',
    `quantity`         INT           NOT NULL COMMENT '예약 수량',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_reservations_code` (`reservation_code`),
    KEY `idx_reservations_user` (`user_id`, `status`),
    KEY `idx_reservations_product` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='예약';

CREATE TABLE `reservation_daily_inventories` (
    `id`                 BIGINT NOT NULL AUTO_INCREMENT COMMENT '예약 재고 연결 고유번호(PK)',
    `reservation_id`     BIGINT NOT NULL COMMENT '예약 고유번호(FK)',
    `daily_inventory_id` BIGINT NOT NULL COMMENT '일별 재고 고유번호(FK)',
    `reserved_count`     INT    NOT NULL DEFAULT 1 COMMENT '예약 수량',
    PRIMARY KEY (`id`),
    KEY `idx_rdi_reservation` (`reservation_id`),
    KEY `idx_rdi_inventory` (`daily_inventory_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='예약-재고 연결';

CREATE TABLE `reservation_cancels` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '예약 취소 고유번호(PK)',
    `reservation_id` BIGINT        NOT NULL COMMENT '예약 고유번호(FK)',
    `cancel_fee`     DECIMAL(15,2) NOT NULL DEFAULT 0 COMMENT '취소 수수료',
    `refund_amount`  DECIMAL(15,2) NOT NULL DEFAULT 0 COMMENT '환불 금액',
    `canceled_at`    TIMESTAMP         NULL DEFAULT CURRENT_TIMESTAMP COMMENT '취소 시각',
    `refunded_at`    TIMESTAMP         NULL COMMENT '환불 완료 시각',
    PRIMARY KEY (`id`),
    KEY `idx_rc_reservation` (`reservation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='예약 취소';

CREATE TABLE `reviews` (
    `review_id`      BIGINT   NOT NULL AUTO_INCREMENT COMMENT '리뷰 고유 번호(PK)',
    `reservation_id` BIGINT   NOT NULL COMMENT '예약 번호(FK)',
    `rating`         TINYINT  NOT NULL COMMENT '별점(1~5)',
    `content`        TEXT         NULL COMMENT '리뷰 내용',
    `status`         ENUM('ACTIVE','DELETED') NOT NULL DEFAULT 'ACTIVE' COMMENT '리뷰 상태',
    `created_at`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '작성 일시',
    `updated_at`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '수정 일시',
    `image_url`      VARCHAR(500) NULL COMMENT '리뷰 이미지',
    PRIMARY KEY (`review_id`),
    KEY `idx_reviews_reservation` (`reservation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='리뷰';


-- =====================================================================
-- 6. 거래내역
-- =====================================================================

CREATE TABLE `transactions` (
    `id`                  BIGINT        NOT NULL AUTO_INCREMENT COMMENT '거래내역 고유 번호(PK)',
    `user_id`             BIGINT        NOT NULL COMMENT '회원 고유 번호(FK)',
    `wallet_id`           BIGINT        NOT NULL COMMENT '전자지갑 고유 번호(FK)',
    `bank_account_id`     BIGINT            NULL COMMENT '연결 계좌(FK). 충전/환불 기록용',
    `card_id`             BIGINT            NULL COMMENT '카드 고유 번호(FK)',
    `workation_id`        BIGINT            NULL COMMENT '워케이션 고유 번호(FK)',
    `reservation_id`      BIGINT            NULL COMMENT '예약 고유번호(FK)',
    `merchant_id`         BIGINT            NULL COMMENT '가맹점 고유 번호(FK) ★워케이션 자동분류 근거★',
    `payment_source_type` ENUM('CARD','WALLET') NOT NULL COMMENT '결제 수단',
    `merchant_name`       VARCHAR(150)  NOT NULL COMMENT '가맹점명',
    `amount`              DECIMAL(15,2) NOT NULL COMMENT '거래 금액',
    `transaction_type`    ENUM('DEPOSIT','WITHDRAWAL','PAYMENT') NOT NULL COMMENT '입금/출금 유형',
    `category_assigned`   VARCHAR(150)      NULL DEFAULT '기타' COMMENT '자동 분류 카테고리(워케이션 도메인 미사용)',
    `is_business_expense` TINYINT(1)        NULL DEFAULT 1 COMMENT '업무 경비 여부(초기 추정값)',
    `approved_number`     VARCHAR(50)       NULL COMMENT '카드 승인 번호',
    `transaction_number`  VARCHAR(50)       NULL COMMENT '서비스 거래번호',
    `status`              ENUM('PAID','FAILED','CANCELED') NULL COMMENT '결제 상태',
    `approved_at`         TIMESTAMP     NOT NULL COMMENT '승인 시각',
    `created_at`          TIMESTAMP         NULL COMMENT '생성일시',
    `updated_at`          TIMESTAMP         NULL COMMENT '수정 일시',
    `cancelled_at`        TIMESTAMP         NULL COMMENT '결제 취소 일시',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_transactions_number` (`transaction_number`),
    KEY `idx_tx_user_approved` (`user_id`, `approved_at`),
    KEY `idx_tx_workation` (`workation_id`, `transaction_type`),
    KEY `idx_tx_merchant` (`merchant_id`),
    KEY `idx_tx_card` (`card_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='거래내역';


-- =====================================================================
-- 7. 워케이션
-- =====================================================================

CREATE TABLE `workations` (
    `id`                    BIGINT        NOT NULL AUTO_INCREMENT COMMENT '워케이션 고유 번호(PK)',
    `user_id`               BIGINT        NOT NULL COMMENT '회원 고유 번호(FK)',
    `region_id`             BIGINT        NOT NULL COMMENT '지역 코드(FK)',
    `title`                 VARCHAR(100)  NOT NULL COMMENT '워케이션 등록 제목',
    `start_date`            DATE          NOT NULL COMMENT '워케이션 시작일',
    `end_date`              DATE          NOT NULL COMMENT '워케이션 종료일',
    `business_budget_total` DECIMAL(15,2)     NULL COMMENT '법인 총예산',
    `personal_budget_total` DECIMAL(15,2)     NULL COMMENT '개인 총예산',
    `status`                ENUM('ACTIVE','SETTLED') NOT NULL DEFAULT 'ACTIVE' COMMENT '워케이션 상태',
    `settled_at`            TIMESTAMP         NULL COMMENT '정산 완료 일시',
    `created_at`            TIMESTAMP         NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일',
    `updated_at`            TIMESTAMP         NULL COMMENT '수정 일시',
    PRIMARY KEY (`id`),
    KEY `idx_workations_user_status` (`user_id`, `status`),
    KEY `idx_workations_period` (`start_date`, `end_date`),
    KEY `idx_workations_region` (`region_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='워케이션 일정';

CREATE TABLE `expense_categories` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '지출 카테고리 고유 번호(PK)',
    `budget_type`  ENUM('WORK','PERSONAL') NOT NULL COMMENT '법인용/개인용 구분',
    `code`         VARCHAR(30)  NOT NULL COMMENT '카테고리 코드',
    `name`         VARCHAR(30)  NOT NULL COMMENT '화면 표시명',
    `description`  VARCHAR(100)     NULL COMMENT '화면에 노출할 설명문',
    `is_default`   TINYINT(1)   NOT NULL COMMENT '1=처음부터 표출, 0=＋ 목록에만',
    `is_deletable` TINYINT(1)   NOT NULL COMMENT '0=삭제 불가(기타)',
    `sort_order`   INT          NOT NULL COMMENT '노출 순서',
    `created_at`   TIMESTAMP        NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ec_type_code` (`budget_type`, `code`),
    KEY `idx_ec_type_default` (`budget_type`, `is_default`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='지출 카테고리 마스터';

CREATE TABLE `merchant_category_mappings` (
    `id`                  BIGINT      NOT NULL AUTO_INCREMENT,
    `budget_type`         ENUM('WORK','PERSONAL') NOT NULL COMMENT '법인용/개인용 구분',
    `merchant_category`   VARCHAR(30) NOT NULL COMMENT 'merchants.category 값',
    `expense_category_id` BIGINT      NOT NULL COMMENT '지출 카테고리 고유 번호(FK)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mcm_type_category` (`budget_type`, `merchant_category`),
    KEY `idx_mcm_expense_category` (`expense_category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='업종별 지출 카테고리 기본 매핑';

CREATE TABLE `user_category_rules` (
    `id`                  BIGINT    NOT NULL AUTO_INCREMENT,
    `user_id`             BIGINT    NOT NULL COMMENT '회원 고유 번호(FK)',
    `merchant_id`         BIGINT    NOT NULL COMMENT '가맹점 고유 번호(FK)',
    `expense_category_id` BIGINT    NOT NULL COMMENT '사용자가 지정한 카테고리(FK)',
    `budget_type`         ENUM('WORK','PERSONAL') NOT NULL COMMENT '법인용/개인용 구분',
    `created_at`          TIMESTAMP     NULL DEFAULT CURRENT_TIMESTAMP COMMENT '최초 정정 일시',
    `updated_at`          TIMESTAMP     NULL COMMENT '최종 정정 일시',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ucr_user_merchant_type` (`user_id`, `merchant_id`, `budget_type`),
    KEY `idx_ucr_expense_category` (`expense_category_id`),
    KEY `idx_ucr_merchant` (`merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='사용자별 가맹점 카테고리 정정 이력';

CREATE TABLE `user_category_labels` (
    `user_id`             BIGINT      NOT NULL COMMENT '회원 고유 번호(PK, FK)',
    `expense_category_id` BIGINT      NOT NULL COMMENT '지출 카테고리 고유 번호(PK, FK)',
    `custom_name`         VARCHAR(50) NOT NULL COMMENT '사용자가 지정한 표시명',
    `updated_at`          TIMESTAMP       NULL DEFAULT CURRENT_TIMESTAMP COMMENT '수정 일시',
    PRIMARY KEY (`user_id`, `expense_category_id`),
    KEY `idx_ucl_category` (`expense_category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='카테고리 표시명 별칭';

CREATE TABLE `budgets` (
    `id`                  BIGINT        NOT NULL AUTO_INCREMENT,
    `workation_id`        BIGINT        NOT NULL COMMENT '워케이션 고유 번호(FK)',
    `expense_category_id` BIGINT        NOT NULL COMMENT '지출 카테고리 고유 번호(FK)',
    `budget_type`         ENUM('WORK','PERSONAL') NOT NULL COMMENT '예산 유형(법인/개인)',
    `target_amount`       DECIMAL(15,2) NOT NULL COMMENT '카테고리 배정 예산',
    `created_at`          TIMESTAMP         NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`          TIMESTAMP         NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_budgets_wk_type_cat` (`workation_id`, `budget_type`, `expense_category_id`),
    KEY `idx_budgets_category` (`expense_category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='카테고리별 예산 배정';

CREATE TABLE `workation_expenses` (
    `id`                  BIGINT        NOT NULL AUTO_INCREMENT,
    `workation_id`        BIGINT        NOT NULL COMMENT '워케이션 고유 번호(FK)',
    `transaction_id`      BIGINT            NULL COMMENT '거래내역 고유 번호(FK). 앱 내 결제만 값 존재',
    `merchant_id`         BIGINT            NULL COMMENT '가맹점 고유 번호(FK). 자동분류 근거',
    `expense_category_id` BIGINT        NOT NULL COMMENT '지출 카테고리 고유 번호(FK)',
    `card_id`             BIGINT            NULL COMMENT '결제 카드 고유 번호(FK). 지갑 밖 법인카드 결제 시 선택',
    `budget_type`         ENUM('WORK','PERSONAL') NOT NULL COMMENT '업무/개인 경비 구분. 정산 필터 기준',
    `amount`              DECIMAL(15,2) NOT NULL COMMENT '지출 금액',
    `merchant_name`       VARCHAR(150)      NULL COMMENT '가맹점명(외부결제·수기 입력용)',
    `spent_at`            TIMESTAMP     NOT NULL COMMENT '지출 일시',
    `memo`                VARCHAR(255)      NULL COMMENT '지출 간단 메모',
    `is_auto_categorized` TINYINT(1)    NOT NULL DEFAULT 1 COMMENT '1=자동분류 유지, 0=사용자 확정',
    `created_at`          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
    `updated_at`          TIMESTAMP         NULL COMMENT '수정 일시',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_we_transaction` (`transaction_id`),
    KEY `idx_we_workation_spent` (`workation_id`, `spent_at`),
    KEY `idx_we_settlement` (`workation_id`, `budget_type`, `expense_category_id`),
    KEY `idx_we_merchant` (`merchant_id`),
    KEY `idx_we_card` (`card_id`),
    KEY `idx_we_category` (`expense_category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='워케이션 지출 내역';


-- =====================================================================
-- 8. 설문
-- =====================================================================

CREATE TABLE `survey_questions` (
    `id`       BIGINT       NOT NULL AUTO_INCREMENT,
    `question` VARCHAR(200) NOT NULL,
    `category` ENUM('COMMON','ACCOMMODATION','RESTAURANT','OFFICE','ACTIVITY') NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='설문 문항';

CREATE TABLE `survey_options` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT,
    `question_id` BIGINT      NOT NULL,
    `tag_id`      BIGINT      NOT NULL,
    `option_name` VARCHAR(20)     NULL,
    `weight`      TINYINT     NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_so_question` (`question_id`),
    KEY `idx_so_tag` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='설문 선택지';

CREATE TABLE `user_surveys` (
    `id`           BIGINT    NOT NULL AUTO_INCREMENT,
    `workation_id` BIGINT    NOT NULL COMMENT '워케이션 고유 번호(FK)',
    `created_at`   TIMESTAMP     NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_us_workation` (`workation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='워케이션별 설문 응답';

CREATE TABLE `user_survey_answers` (
    `id`        BIGINT NOT NULL AUTO_INCREMENT,
    `survey_id` BIGINT NOT NULL,
    `option_id` BIGINT NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_usa_survey` (`survey_id`),
    KEY `idx_usa_option` (`option_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='설문 응답 항목';


-- =====================================================================
-- 9. FOREIGN KEY
-- =====================================================================

-- 회원 · 인증
ALTER TABLE `user_device`                ADD CONSTRAINT `FK_users_TO_user_device_1`                FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);
ALTER TABLE `user_terms_agreements`      ADD CONSTRAINT `FK_users_TO_user_terms_agreements_1`      FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);
ALTER TABLE `user_terms_agreements`      ADD CONSTRAINT `FK_terms_TO_user_terms_agreements_1`      FOREIGN KEY (`term_id`) REFERENCES `terms` (`id`);
ALTER TABLE `user_notification_settings` ADD CONSTRAINT `FK_users_TO_user_notification_settings_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);
ALTER TABLE `notification_histories`     ADD CONSTRAINT `FK_users_TO_notification_histories_1`     FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);

-- 지갑 · 결제
ALTER TABLE `wallets`       ADD CONSTRAINT `FK_users_TO_wallets_1`                  FOREIGN KEY (`user_id`)           REFERENCES `users` (`id`);
ALTER TABLE `bank_accounts` ADD CONSTRAINT `FK_users_TO_bank_accounts_1`            FOREIGN KEY (`user_id`)           REFERENCES `users` (`id`);
ALTER TABLE `bank_accounts` ADD CONSTRAINT `FK_banks_TO_bank_accounts_1`            FOREIGN KEY (`bank_code`)         REFERENCES `banks` (`code`);
ALTER TABLE `cards`         ADD CONSTRAINT `FK_users_TO_cards_1`                    FOREIGN KEY (`user_id`)           REFERENCES `users` (`id`);
ALTER TABLE `cards`         ADD CONSTRAINT `FK_card_companies_TO_cards_1`           FOREIGN KEY (`card_company_code`) REFERENCES `card_companies` (`code`);

-- 가맹점
ALTER TABLE `merchants`      ADD CONSTRAINT `FK_region_TO_merchants_1`         FOREIGN KEY (`id2`)         REFERENCES `region` (`id`);
ALTER TABLE `restaurants`    ADD CONSTRAINT `FK_merchants_TO_restaurants_1`    FOREIGN KEY (`id2`)         REFERENCES `merchants` (`id`);
ALTER TABLE `accommodations` ADD CONSTRAINT `FK_merchants_TO_accommodations_1` FOREIGN KEY (`id2`)         REFERENCES `merchants` (`id`);
ALTER TABLE `offices`        ADD CONSTRAINT `FK_merchants_TO_offices_1`        FOREIGN KEY (`merchant_id`) REFERENCES `merchants` (`id`);
ALTER TABLE `activities`     ADD CONSTRAINT `FK_merchants_TO_activities_1`     FOREIGN KEY (`merchant_id`) REFERENCES `merchants` (`id`);
ALTER TABLE `merchant_tags`  ADD CONSTRAINT `FK_merchants_TO_merchant_tags_1`  FOREIGN KEY (`merchant_id`) REFERENCES `merchants` (`id`);
ALTER TABLE `merchant_tags`  ADD CONSTRAINT `FK_tags_TO_merchant_tags_1`       FOREIGN KEY (`tag_id`)      REFERENCES `tags` (`id`);

-- 예약 · 리뷰
ALTER TABLE `reservation_products`          ADD CONSTRAINT `FK_merchants_TO_reservation_products_1`                       FOREIGN KEY (`merchant_id`)        REFERENCES `merchants` (`id`);
ALTER TABLE `product_daily_inventories`     ADD CONSTRAINT `FK_reservation_products_TO_product_daily_inventories_1`       FOREIGN KEY (`product_id`)         REFERENCES `reservation_products` (`id`);
ALTER TABLE `reservations`                  ADD CONSTRAINT `FK_reservation_products_TO_reservations_1`                    FOREIGN KEY (`product_id`)         REFERENCES `reservation_products` (`id`);
ALTER TABLE `reservations`                  ADD CONSTRAINT `FK_users_TO_reservations_1`                                   FOREIGN KEY (`user_id`)            REFERENCES `users` (`id`);
ALTER TABLE `reservation_daily_inventories` ADD CONSTRAINT `FK_reservations_TO_reservation_daily_inventories_1`           FOREIGN KEY (`reservation_id`)     REFERENCES `reservations` (`id`);
ALTER TABLE `reservation_daily_inventories` ADD CONSTRAINT `FK_product_daily_inventories_TO_reservation_daily_inv_1`      FOREIGN KEY (`daily_inventory_id`) REFERENCES `product_daily_inventories` (`id`);
ALTER TABLE `reservation_cancels`           ADD CONSTRAINT `FK_reservations_TO_reservation_cancels_1`                     FOREIGN KEY (`reservation_id`)     REFERENCES `reservations` (`id`);
ALTER TABLE `reviews`                       ADD CONSTRAINT `FK_reservations_TO_reviews_1`                                 FOREIGN KEY (`reservation_id`)     REFERENCES `reservations` (`id`);

-- 거래내역
ALTER TABLE `transactions` ADD CONSTRAINT `FK_users_TO_transactions_1`         FOREIGN KEY (`user_id`)         REFERENCES `users` (`id`);
ALTER TABLE `transactions` ADD CONSTRAINT `FK_wallets_TO_transactions_1`       FOREIGN KEY (`wallet_id`)       REFERENCES `wallets` (`id`);
ALTER TABLE `transactions` ADD CONSTRAINT `FK_bank_accounts_TO_transactions_1` FOREIGN KEY (`bank_account_id`) REFERENCES `bank_accounts` (`id`);
ALTER TABLE `transactions` ADD CONSTRAINT `FK_cards_TO_transactions_1`         FOREIGN KEY (`card_id`)         REFERENCES `cards` (`id`);
ALTER TABLE `transactions` ADD CONSTRAINT `FK_workations_TO_transactions_1`    FOREIGN KEY (`workation_id`)    REFERENCES `workations` (`id`);
ALTER TABLE `transactions` ADD CONSTRAINT `FK_reservations_TO_transactions_1`  FOREIGN KEY (`reservation_id`)  REFERENCES `reservations` (`id`);
ALTER TABLE `transactions` ADD CONSTRAINT `FK_merchants_TO_transactions_1`     FOREIGN KEY (`merchant_id`)     REFERENCES `merchants` (`id`);

-- 워케이션
ALTER TABLE `workations`                 ADD CONSTRAINT `FK_users_TO_workations_1`                              FOREIGN KEY (`user_id`)             REFERENCES `users` (`id`);
ALTER TABLE `workations`                 ADD CONSTRAINT `FK_region_TO_workations_1`                             FOREIGN KEY (`region_id`)           REFERENCES `region` (`id`);
ALTER TABLE `merchant_category_mappings` ADD CONSTRAINT `FK_expense_categories_TO_merchant_category_mappings_1` FOREIGN KEY (`expense_category_id`) REFERENCES `expense_categories` (`id`);
ALTER TABLE `user_category_rules`        ADD CONSTRAINT `FK_users_TO_user_category_rules_1`                     FOREIGN KEY (`user_id`)             REFERENCES `users` (`id`);
ALTER TABLE `user_category_rules`        ADD CONSTRAINT `FK_merchants_TO_user_category_rules_1`                 FOREIGN KEY (`merchant_id`)         REFERENCES `merchants` (`id`);
ALTER TABLE `user_category_rules`        ADD CONSTRAINT `FK_expense_categories_TO_user_category_rules_1`        FOREIGN KEY (`expense_category_id`) REFERENCES `expense_categories` (`id`);
ALTER TABLE `user_category_labels`       ADD CONSTRAINT `FK_users_TO_user_category_labels_1`                    FOREIGN KEY (`user_id`)             REFERENCES `users` (`id`);
ALTER TABLE `user_category_labels`       ADD CONSTRAINT `FK_expense_categories_TO_user_category_labels_1`       FOREIGN KEY (`expense_category_id`) REFERENCES `expense_categories` (`id`);
ALTER TABLE `budgets`                    ADD CONSTRAINT `FK_workations_TO_budgets_1`                            FOREIGN KEY (`workation_id`)        REFERENCES `workations` (`id`);
ALTER TABLE `budgets`                    ADD CONSTRAINT `FK_expense_categories_TO_budgets_1`                    FOREIGN KEY (`expense_category_id`) REFERENCES `expense_categories` (`id`);
ALTER TABLE `workation_expenses`         ADD CONSTRAINT `FK_workations_TO_workation_expenses_1`                 FOREIGN KEY (`workation_id`)        REFERENCES `workations` (`id`);
ALTER TABLE `workation_expenses`         ADD CONSTRAINT `FK_transactions_TO_workation_expenses_1`               FOREIGN KEY (`transaction_id`)      REFERENCES `transactions` (`id`);
ALTER TABLE `workation_expenses`         ADD CONSTRAINT `FK_merchants_TO_workation_expenses_1`                  FOREIGN KEY (`merchant_id`)         REFERENCES `merchants` (`id`);
ALTER TABLE `workation_expenses`         ADD CONSTRAINT `FK_cards_TO_workation_expenses_1`                      FOREIGN KEY (`card_id`)             REFERENCES `cards` (`id`);
ALTER TABLE `workation_expenses`         ADD CONSTRAINT `FK_expense_categories_TO_workation_expenses_1`         FOREIGN KEY (`expense_category_id`) REFERENCES `expense_categories` (`id`);

-- 설문
ALTER TABLE `survey_options`      ADD CONSTRAINT `FK_survey_questions_TO_survey_options_1`     FOREIGN KEY (`question_id`)  REFERENCES `survey_questions` (`id`);
ALTER TABLE `survey_options`      ADD CONSTRAINT `FK_tags_TO_survey_options_1`                 FOREIGN KEY (`tag_id`)       REFERENCES `tags` (`id`);
ALTER TABLE `user_surveys`        ADD CONSTRAINT `FK_workations_TO_user_surveys_1`             FOREIGN KEY (`workation_id`) REFERENCES `workations` (`id`);
ALTER TABLE `user_survey_answers` ADD CONSTRAINT `FK_user_surveys_TO_user_survey_answers_1`    FOREIGN KEY (`survey_id`)    REFERENCES `user_surveys` (`id`);
ALTER TABLE `user_survey_answers` ADD CONSTRAINT `FK_survey_options_TO_user_survey_answers_1`  FOREIGN KEY (`option_id`)    REFERENCES `survey_options` (`id`);


SET FOREIGN_KEY_CHECKS = 1;


-- =====================================================================
-- 10. SEED DATA
-- =====================================================================

-- 지역
INSERT INTO `region` (`name`) VALUES ('강릉'), ('양양'), ('부산'), ('제주');

-- 지출 카테고리 마스터 : 법인 12 + 개인 11 = 23줄
INSERT INTO `expense_categories`
    (`budget_type`, `code`, `name`, `description`, `is_default`, `is_deletable`, `sort_order`)
VALUES
('WORK', 'ACCOMMODATION',  '숙박비',      '호텔·에어비앤비 등',           1, 1,  1),
('WORK', 'TRANSPORTATION', '교통비',      '항공·철도·택시·렌터카 등',     1, 1,  2),
('WORK', 'RENT',           '임차료',      '공유오피스·회의실 대여 등',     1, 1,  3),
('WORK', 'MEETING',        '회의비',      '업무 미팅 중 식음료·다과',      1, 1,  4),
('WORK', 'FOOD',           '식비',        '업무 식대, 회의 다과 등',       1, 1,  5),
('WORK', 'ETC',            '기타',        '그 외 업무 경비',              1, 0,  6),
('WORK', 'COMMUNICATION',  '통신비',      '데이터·와이파이 등 업무 통신',   0, 1,  7),
('WORK', 'SUPPLIES',       '소모품비',    '사무용품·비품 구매',            0, 1,  8),
('WORK', 'ENTERTAINMENT',  '접대비',      '거래처·협력사 응대',            0, 1,  9),
('WORK', 'VEHICLE',        '차량유지비',  '렌터카·주유·주차',             0, 1, 10),
('WORK', 'EDUCATION',      '교육·도서비', '업무 관련 자료·강의',           0, 1, 11),
('WORK', 'INSURANCE',      '보험료',      '출장자 여행자보험',             0, 1, 12),
('PERSONAL', 'ACCOMMODATION',  '숙박비',          '자기부담 숙박·기간 연장', 1, 1,  1),
('PERSONAL', 'TRANSPORTATION', '교통비',          '개인 이동·주말 나들이',   1, 1,  2),
('PERSONAL', 'FOOD',           '식비',            '개인 식사',              1, 1,  3),
('PERSONAL', 'LEISURE',        '여가비',          '관광·액티비티',           1, 1,  4),
('PERSONAL', 'SHOPPING',       '쇼핑',            '기념품·생활용품',         1, 1,  5),
('PERSONAL', 'ETC',            '기타',            '그 외 개인 지출',         1, 0,  6),
('PERSONAL', 'CAFE',           '카페·간식',       '커피·디저트·간식',        0, 1,  7),
('PERSONAL', 'GATHERING',      '모임비',          '술자리·친목 모임',        0, 1,  8),
('PERSONAL', 'HEALTH',         '건강·의료',       '병원·약국·운동',          0, 1,  9),
('PERSONAL', 'LAUNDRY',        '세탁·생활서비스', '세탁·수선 등',            0, 1, 10),
('PERSONAL', 'COMMUNICATION',  '통신비',          '개인 데이터·로밍',        0, 1, 11);

-- 업종 → 지출 카테고리 기본 매핑 : 8줄
INSERT INTO `merchant_category_mappings` (`budget_type`, `merchant_category`, `expense_category_id`) VALUES
('WORK',     'ACCOMMODATION', (SELECT `id` FROM `expense_categories` WHERE `budget_type`='WORK'     AND `code`='ACCOMMODATION')),
('WORK',     'RESTAURANT',    (SELECT `id` FROM `expense_categories` WHERE `budget_type`='WORK'     AND `code`='FOOD')),
('WORK',     'OFFICE',        (SELECT `id` FROM `expense_categories` WHERE `budget_type`='WORK'     AND `code`='RENT')),
('WORK',     'ACTIVITY',      (SELECT `id` FROM `expense_categories` WHERE `budget_type`='WORK'     AND `code`='ETC')),
('PERSONAL', 'ACCOMMODATION', (SELECT `id` FROM `expense_categories` WHERE `budget_type`='PERSONAL' AND `code`='ACCOMMODATION')),
('PERSONAL', 'RESTAURANT',    (SELECT `id` FROM `expense_categories` WHERE `budget_type`='PERSONAL' AND `code`='FOOD')),
('PERSONAL', 'OFFICE',        (SELECT `id` FROM `expense_categories` WHERE `budget_type`='PERSONAL' AND `code`='ETC')),
('PERSONAL', 'ACTIVITY',      (SELECT `id` FROM `expense_categories` WHERE `budget_type`='PERSONAL' AND `code`='LEISURE'));


-- =====================================================================
-- 11. 검증 쿼리
-- =====================================================================
-- SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA='workit';                 -- 37
-- SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
--  WHERE TABLE_SCHEMA='workit' AND CONSTRAINT_TYPE='FOREIGN KEY';                             -- 48
-- SELECT budget_type, COUNT(*) FROM expense_categories GROUP BY budget_type;                  -- WORK 12 / PERSONAL 11
-- SELECT COUNT(*) FROM merchant_category_mappings;                                            -- 8

ALTER TABLE workation_expenses DROP FOREIGN KEY FK_merchants_TO_workation_expenses_1;
ALTER TABLE workation_expenses DROP COLUMN merchant_id;


INSERT INTO users (email, password, name, phone_number, pass_ci, company_info)
VALUES ('test@workit.com', 'test1234', '김태균', '01000000000', 'CI_TEST_0001', 'KB테크');

SELECT id, email, name FROM users;