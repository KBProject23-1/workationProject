DROP DATABASE IF EXISTS workit;

CREATE DATABASE workit;

USE workit;

CREATE TABLE `users` (
                         `id`                    BIGINT          NOT NULL AUTO_INCREMENT COMMENT '회원 고유 번호(PK)',
                         `email_hash`            VARCHAR(100)    NOT NULL                COMMENT '유저 이메일 (로그인 ID) SHA-256',
                         `email_encrypt`         VARCHAR(100)    NOT NULL                COMMENT '유저 이메일 (로그인 ID) AES',
                         `name_encrypt`          VARCHAR(50)     NOT NULL                COMMENT '유저 이름 AES',
                         `phone_number_hash`     VARCHAR(255)    NOT NULL                COMMENT '유저 핸드폰 번호 SHA-256',
                         `phone_number_encrypt`  VARCHAR(255)    NOT NULL                COMMENT '유저 핸드폰 번호 AES',
                         `status`                VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE, PENDING, BLOCKED, WITHDRAWN',
                         `created_at`            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '유저 계정 생성 시간',
                         `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '유저 계정 업데이트 시간',
                         `deleted_at`            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '유저 계정 삭제 시간',



    -- 제약 조건 설정
                         PRIMARY KEY (`id`),
                         UNIQUE KEY `ux_users_email` (`email_hash`),          -- 로그인 ID 중복 방지
                         UNIQUE KEY `ux_users_phone` (`phone_number_hash`),   -- 휴대폰 번호 중복 가입 방지
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='회원 기본 계정 정보 테이블';

CREATE TABLE `user_auth` (
                         `id`                        BIGINT          NOT NULL AUTO_INCREMENT COMMENT '인증 정보 고유 번호(PK)',
                         `user_id`                   BIGINT          NOT NULL                COMMENT '회원 고유 번호 (FK, users.id 참조)',
                         `password_hash`             VARCHAR(255)    NOT NULL                COMMENT '유저 비밀번호 (BCrypt 암호화)',
                         `identity_ci_hash`          VARCHAR(255)    NOT NULL                COMMENT '유저 PASS 인증 식별값 SHA-256 (1인1계정 검증)',
                         `identity_ci_encrypt`       VARCHAR(255)    NOT NULL                COMMENT '유저 PASS 인증 식별값 AES',
                         `created_at`                DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '유저 계정 생성 시간',
                         `updated_at`                DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '유저 계정 업데이트 시간',


    -- 제약 조건 설정
                         PRIMARY KEY (`id`),
                         UNIQUE KEY `ux_users_pass_ci` (`pass_ci_hash`),     -- PASS CI값 중복 가입 방지 (원천 차단)
                         CONSTRAINT `fk_user_auth_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
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
                               `pin_hash`       CHAR(60)      NOT NULL                COMMENT '자산 거래용 6자리 핀번호 (BCrypt 암호화문)',
                               `last_login_at`  DATETIME      NULL                    COMMENT '해당 기기 최종 로그인 일시',
                               `created_at`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '기기 최초 인증 등록 일시',

    -- 제약 조건 설정
                               PRIMARY KEY (`id`),
                               UNIQUE KEY `ux_user_device_id` (`user_id`, `device_id`), -- 한 유저가 동일 기기를 중복 등록하는 것 방지
                               CONSTRAINT `fk_user_device_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='회원별 보안 핀번호 및 로그인 기기 관리 테이블 (비식별 관계)';

CREATE TABLE `terms` (
                         `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '약관 고유 번호(PK)',
                         `title`       VARCHAR(100)  NOT NULL                COMMENT '약관 제목',
                         `content`     LONGTEXT          NOT NULL                COMMENT '약관 본문 상세 내용',
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

-- =========================================================================================
-- 1. 워케이션 거점 지역
-- =========================================================================================

CREATE TABLE `region` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '지역 코드 번호(PK)',
    `name`        VARCHAR(50)  NOT NULL                COMMENT '지역 이름 (예: 부산, 제주특별자치도)',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '지역 등록 일시',

    -- 제약 조건 설정
    PRIMARY KEY (`id`),
    UNIQUE KEY `ux_region_name` (`name`)   -- 동일 지역 중복 등록 방지
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='워케이션 거점 지역 마스터 테이블';


-- =========================================================================================
-- 2. 지출 카테고리 마스터
-- =========================================================================================

CREATE TABLE `expense_categories` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '지출 카테고리 고유 번호(PK)',
    `user_id`       BIGINT        NULL                    COMMENT '회원 고유 번호 (FK, users.id 참조). NULL=시스템 기본',
    `budget_type`   ENUM('WORK', 'PERSONAL') NOT NULL     COMMENT '법인용(WORK) / 개인용(PERSONAL) 구분',
    `code`          VARCHAR(30)   NOT NULL                COMMENT '카테고리 코드 (예: ACCOMMODATION)',
    `name`          VARCHAR(30)   NOT NULL                COMMENT '화면 표시명 (기본 이름)',
    `description`   VARCHAR(100)  NULL                    COMMENT '화면에 노출할 설명문',
    `is_default`    TINYINT(1)    NOT NULL DEFAULT 0      COMMENT '예산 화면 기본 표출 여부 (1:처음부터 표출, 0:＋ 목록에만)',
    `is_deletable`  TINYINT(1)    NOT NULL DEFAULT 1      COMMENT '삭제 가능 여부 (0:삭제 불가 - 기타 카테고리)',
    `sort_order`    INT           NOT NULL DEFAULT 0      COMMENT '화면 노출 순서',
    `created_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '카테고리 생성 일시',

    -- 제약 조건 설정
    PRIMARY KEY (`id`),
    UNIQUE KEY `ux_expense_categories_type_code` (`budget_type`, `code`),  -- 예산 유형별 코드 중복 방지
    KEY `ix_expense_categories_lookup` (`budget_type`, `is_default`, `sort_order`),  -- 목록 조회 성능
    CONSTRAINT `fk_expense_categories_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='지출 카테고리 마스터 테이블 (기존 category ENUM 대체)';


-- =========================================================================================
-- 3. 카테고리 표시명 별칭
-- =========================================================================================

CREATE TABLE `user_category_labels` (
    `user_id`              BIGINT       NOT NULL COMMENT '회원 고유 번호 (PK 겸 FK, users.id 참조)',
    `expense_category_id`  BIGINT       NOT NULL COMMENT '지출 카테고리 고유 번호 (PK 겸 FK, expense_categories.id 참조)',
    `custom_name`          VARCHAR(50)  NOT NULL COMMENT '사용자가 지정한 표시명 (최대 10자 제한은 애플리케이션에서 검증)',
    `updated_at`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '별칭 최종 수정 일시',

    -- 제약 조건 설정 (식별 관계 - 두 FK 가 모두 PK 를 구성)
    PRIMARY KEY (`user_id`, `expense_category_id`),
    KEY `ix_user_category_labels_category` (`expense_category_id`),
    CONSTRAINT `fk_user_category_labels_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_user_category_labels_category_id` FOREIGN KEY (`expense_category_id`) REFERENCES `expense_categories` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='카테고리 표시명 사용자 별칭 테이블 (식별 관계)';


-- =========================================================================================
-- 4. 워케이션 등록 폼
-- =========================================================================================

CREATE TABLE `workations` (
    `id`                     BIGINT         NOT NULL AUTO_INCREMENT COMMENT '워케이션 고유 번호(PK)',
    `user_id`                BIGINT         NOT NULL                COMMENT '회원 고유 번호 (FK, users.id 참조)',
    `region_id`              BIGINT         NOT NULL                COMMENT '지역 코드 번호 (FK, region.id 참조)',
    `title`                  VARCHAR(100)   NOT NULL                COMMENT '워케이션 제목',
    `start_date`             DATE           NOT NULL                COMMENT '워케이션 시작일',
    `end_date`               DATE           NOT NULL                COMMENT '워케이션 종료일',
    `business_budget_total`  DECIMAL(15,2)  NOT NULL DEFAULT 0.00   COMMENT '법인 총예산',
    `personal_budget_total`  DECIMAL(15,2)  NOT NULL DEFAULT 0.00   COMMENT '개인 총예산',
    `status`                 ENUM('ACTIVE', 'SETTLED') NOT NULL DEFAULT 'ACTIVE' COMMENT '진행 상태 (ACTIVE:진행 중, SETTLED:정산 완료)',
    `settled_at`             DATETIME       NULL                    COMMENT '정산 완료 일시',
    `created_at`             DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '워케이션 생성 일시',
    `updated_at`             DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '워케이션 최종 수정 일시',

    -- 제약 조건 설정
    PRIMARY KEY (`id`),
    KEY `ix_workations_user_status` (`user_id`, `status`),        -- 진행 중 워케이션 조회 / 중복 등록 검증
    KEY `ix_workations_user_settled` (`user_id`, `settled_at`),   -- 기록 목록 최신순 정렬
    CONSTRAINT `fk_workations_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_workations_region_id` FOREIGN KEY (`region_id`) REFERENCES `region` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='워케이션 일정 및 총예산 테이블 (비식별 관계)';


-- =========================================================================================
-- 5. 카테고리별 예산 배정
-- =========================================================================================

CREATE TABLE `budgets` (
    `id`                   BIGINT         NOT NULL AUTO_INCREMENT COMMENT '예산 배정 고유 번호(PK)',
    `workation_id`         BIGINT         NOT NULL                COMMENT '워케이션 고유 번호 (FK, workations.id 참조)',
    `expense_category_id`  BIGINT         NOT NULL                COMMENT '지출 카테고리 고유 번호 (FK, expense_categories.id 참조)',
    `budget_type`          ENUM('WORK', 'PERSONAL') NOT NULL      COMMENT '예산 유형 (WORK:법인, PERSONAL:개인)',
    `target_amount`        DECIMAL(15,2)  NOT NULL DEFAULT 0.00   COMMENT '카테고리에 배정한 예산',
    `created_at`           DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '예산 배정 일시',
    `updated_at`           DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '예산 최종 수정 일시',

    -- 제약 조건 설정
    PRIMARY KEY (`id`),
    UNIQUE KEY `ux_budgets_workation_type_category` (`workation_id`, `budget_type`, `expense_category_id`),  -- 같은 카테고리 중복 배정 방지
    KEY `ix_budgets_category` (`expense_category_id`),
    CONSTRAINT `fk_budgets_workation_id` FOREIGN KEY (`workation_id`) REFERENCES `workations` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_budgets_category_id` FOREIGN KEY (`expense_category_id`) REFERENCES `expense_categories` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='워케이션 카테고리별 예산 배정 테이블 (비식별 관계)';


-- =========================================================================================
-- 6. 워케이션 지출 내역
-- =========================================================================================

CREATE TABLE `workation_expenses` (
    `id`                   BIGINT         NOT NULL AUTO_INCREMENT COMMENT '지출 내역 고유 번호(PK)',
    `workation_id`         BIGINT         NOT NULL                COMMENT '워케이션 고유 번호 (FK, workations.id 참조)',
    `transaction_id`       BIGINT         NULL                    COMMENT '거래내역 고유 번호 (FK, transactions.id 참조). 앱 내 결제만 값 존재',
    `expense_category_id`  BIGINT         NOT NULL                COMMENT '지출 카테고리 고유 번호 (FK, expense_categories.id 참조)',
    `card_id`              BIGINT         NULL                    COMMENT '결제 카드 고유 번호 (FK, cards.id 참조). 지갑 밖 법인카드 결제 시 선택',
    `budget_type`          ENUM('WORK', 'PERSONAL') NOT NULL      COMMENT '업무/개인 경비 구분. 정산 필터 기준',
    `amount`               DECIMAL(15,2)  NOT NULL                COMMENT '지출 금액',
    `merchant_name`        VARCHAR(150)   NULL                    COMMENT '가맹점명 (외부 결제 수기 입력용)',
    `spent_at`             DATETIME       NOT NULL                COMMENT '지출 일시',
    `memo`                 VARCHAR(255)   NULL                    COMMENT '지출 간단 메모',
    `is_auto_categorized`  TINYINT(1)     NOT NULL DEFAULT 1      COMMENT '자동분류 상태 (1:자동분류 유지, 0:사용자 확정)',
    `created_at`           DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '지출 등록 일시',
    `updated_at`           DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '지출 최종 수정 일시',

    -- 제약 조건 설정
    PRIMARY KEY (`id`),
    UNIQUE KEY `ux_workation_expenses_transaction` (`transaction_id`),  -- 같은 결제가 두 번 등록되는 것 방지
    KEY `ix_workation_expenses_workation_spent` (`workation_id`, `spent_at`),  -- 지출 목록 기간 조회
    KEY `ix_workation_expenses_settlement` (`workation_id`, `budget_type`, `expense_category_id`),  -- 예산·정산 집계
    KEY `ix_workation_expenses_card` (`card_id`),
    CONSTRAINT `fk_workation_expenses_workation_id` FOREIGN KEY (`workation_id`) REFERENCES `workations` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_workation_expenses_category_id` FOREIGN KEY (`expense_category_id`) REFERENCES `expense_categories` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='워케이션 지출 내역 테이블 (비식별 관계)';



-- =========================================================================================
-- 7. 가맹점 업종
-- =========================================================================================

CREATE TABLE `merchant_category_mappings` (
    `id`                   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '매핑 고유 번호(PK)',
    `budget_type`          ENUM('WORK', 'PERSONAL') NOT NULL    COMMENT '법인용 / 개인용 구분',
    `merchant_category`    VARCHAR(30)  NOT NULL                COMMENT '가맹점 업종 코드 (merchants.category 값)',
    `expense_category_id`  BIGINT       NOT NULL                COMMENT '지출 카테고리 고유 번호 (FK, expense_categories.id 참조)',
    `created_at`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '매핑 등록 일시',

    -- 제약 조건 설정
    PRIMARY KEY (`id`),
    UNIQUE KEY `ux_merchant_category_mappings_type_category` (`budget_type`, `merchant_category`),  -- 업종당 기본 매핑은 유형별 1건
    KEY `ix_merchant_category_mappings_category` (`expense_category_id`),
    CONSTRAINT `fk_merchant_category_mappings_category_id` FOREIGN KEY (`expense_category_id`) REFERENCES `expense_categories` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='가맹점 업종별 카테고리 기본 매핑 테이블 (자동분류 기준)';


-- =========================================================================================
-- 8. 사용자 정정 규칙 (자동분류 1단계)
--    사용자가 특정 가맹점의 분류를 직접 바꾸면 이후 같은 가맹점 결제에 우선 적용
-- =========================================================================================

CREATE TABLE `user_category_rules` (
    `id`                   BIGINT     NOT NULL AUTO_INCREMENT COMMENT '정정 규칙 고유 번호(PK)',
    `user_id`              BIGINT     NOT NULL                COMMENT '회원 고유 번호 (FK, users.id 참조)',
    `merchant_id`          BIGINT     NOT NULL                COMMENT '가맹점 고유 번호 (FK, merchants.id 참조)',
    `expense_category_id`  BIGINT     NOT NULL                COMMENT '사용자가 지정한 카테고리 (FK, expense_categories.id 참조)',
    `budget_type`          ENUM('WORK', 'PERSONAL') NOT NULL  COMMENT '법인용 / 개인용 구분',
    `created_at`           DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '최초 정정 일시',
    `updated_at`           DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '최종 정정 일시',

    -- 제약 조건 설정
    PRIMARY KEY (`id`),
    UNIQUE KEY `ux_user_category_rules_user_merchant_type` (`user_id`, `merchant_id`, `budget_type`),  -- 동일 가맹점 규칙 중복 방지
    KEY `ix_user_category_rules_category` (`expense_category_id`),
    CONSTRAINT `fk_user_category_rules_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_user_category_rules_category_id` FOREIGN KEY (`expense_category_id`) REFERENCES `expense_categories` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자별 가맹점 카테고리 정정 규칙 테이블 (자동분류 최우선)';

-- ================================================================
-- 외래키 제약 조건 검사 일시 비활성화 (선택 사항 - 테이블 생성 순서 무관하게 실행 가능)
SET FOREIGN_KEY_CHECKS = 0;

-- 1. 은행 기본 정보 테이블
CREATE TABLE `banks` (
                         `code` VARCHAR(10) PRIMARY KEY COMMENT '은행 코드 (PK)',
                         `name` VARCHAR(50) NOT NULL COMMENT '은행명',
                         `logo_url` VARCHAR(255) NULL COMMENT '은행 로고 이미지 경로'
);

-- 2. 카드사 기본 정보 테이블
CREATE TABLE `card_companies` (
                                  `code` VARCHAR(10) PRIMARY KEY COMMENT '카드사 코드 (PK)',
                                  `name` VARCHAR(50) NOT NULL COMMENT '카드사명',
                                  `logo_url` VARCHAR(255) NULL COMMENT '카드사 로고 이미지 경로'
);

-- 3. 지갑 테이블
CREATE TABLE `wallets` (
                           `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
                           `user_id` BIGINT NOT NULL,
                           `balance` DECIMAL(15, 2) NULL DEFAULT 0.00 COMMENT '시스템 계좌 충전금',
                           `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP COMMENT '지갑 업데이트 시간'
);

-- 4. 계좌 테이블
CREATE TABLE `bank_accounts` (
                                 `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
                                 `user_id` BIGINT NOT NULL,
                                 `bank_code` VARCHAR(10) NOT NULL COMMENT '은행 코드',
                                 `account_number` VARCHAR(50) NOT NULL COMMENT '계좌번호',
                                 `product_name` VARCHAR(100) NOT NULL COMMENT '계좌 이름',
                                 `balance` DECIMAL(15, 2) NULL DEFAULT 0.00 COMMENT '계좌 잔액',
                                 `is_primary` TINYINT(1) NULL DEFAULT 0 COMMENT '주거래 계좌 여부',
                                 `is_withdrawal_agreed` TINYINT(1) NULL COMMENT '계좌 입출금 동의 여부',
                                 `withdrawal_agreed_at` TIMESTAMP NULL COMMENT '오픈 뱅킹 약관 동의 시간',
                                 `balance_updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP COMMENT '계좌 잔액 업데이트 시간',
                                 `is_deleted` TINYINT(1) NULL DEFAULT 0 COMMENT '등록 계좌 삭제 여부'
);

-- 5. 카드 테이블 (card_companies 외래키 포함)
CREATE TABLE `cards` (
                         `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
                         `user_id` BIGINT NOT NULL,
                         `card_company_code` VARCHAR(10) NOT NULL COMMENT '카드 회사 코드(카드사 구분)',
                         `card_name` VARCHAR(100) NOT NULL COMMENT '카드 이름(사용자가 지정한 카드 별칭 - 없을때 기본 카드 이름)',
                         `card_number` VARCHAR(16) NULL COMMENT '카드 번호',
                         `card_classification` ENUM('CREDIT', 'DEBIT') NULL COMMENT '신용 / 체크 구분용',
                         `card_type` ENUM('WORK', 'PERSONAL') NOT NULL COMMENT '법인 / 개인 구분용',
                         `is_primary` TINYINT(1) NULL DEFAULT 0 COMMENT '주 거래 카드 여부',
                         `is_agreed` TINYINT(1) NULL COMMENT '카드 이용 동의 여부',
                         `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP COMMENT '카드 등록 시간(카드 이용 동의 시간)',
                         `is_deleted` TINYINT(1) NULL DEFAULT 0 COMMENT '등록 카드 삭제 여부',
                         `updated_at` TIMESTAMP NULL COMMENT '수정 일시',
                         `deleted_at` TIMESTAMP NULL COMMENT '삭제 일시',
                         CONSTRAINT `FK_card_companies_TO_cards` FOREIGN KEY (`card_company_code`) REFERENCES `card_companies` (`code`)
);

-- 6. 거래 내역 테이블 (wallets, cards, bank_accounts, merchants 외래키 포함)
CREATE TABLE `transactions` (
                                `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '거래내역 고유 번호(PK)',
                                `user_id` BIGINT NOT NULL COMMENT '회원 고유 번호(FK)',
                                `wallet_id` BIGINT NULL COMMENT '전자지갑 ID',
                                `bank_account_id` BIGINT NULL COMMENT '주거래 계좌 ID 충전 / 환불 기록용',
                                `card_id` BIGINT NULL COMMENT '카드 고유 번호(FK)',
                                `workation_id` BIGINT NULL COMMENT '워케이션 고유 번호(FK)',
                                `reservation_id` BIGINT NULL COMMENT '예약과 관련된 거래인 경우 연결되는 예약 고유번호(FK)',
                                `merchant_id` BIGINT NULL COMMENT '결제한 가맹점 고유 번호(FK)',
                                `payment_source_type` ENUM('CARD', 'WALLET') NOT NULL COMMENT '결제 수단',
                                `merchant_name` VARCHAR(150) NOT NULL COMMENT '가맹점명',
                                `amount` DECIMAL(15, 2) NOT NULL COMMENT '거래 금액',
                                `transaction_type` ENUM('DEPOSIT', 'WITHDRAWAL', 'PAYMENT') NOT NULL COMMENT '입금/출금 유형',
                                `category_assigned` VARCHAR(150) NULL DEFAULT '기타' COMMENT '자동 분류된 지출 카테고리',
                                `is_business_expense` TINYINT(1) NULL DEFAULT 1 COMMENT '업무 경비 여부',
                                `approved_number` VARCHAR(50) NULL COMMENT '카드 승인 번호',
                                `transaction_number` VARCHAR(50) NULL COMMENT '우리 서비스 거래번호',
                                `status` ENUM('PAID', 'FAILED', 'CANCELED') NULL COMMENT '결제 상태',
                                `approved_at` TIMESTAMP NOT NULL COMMENT '승인 시각',
                                `created_at` TIMESTAMP NULL COMMENT '생성일시',
                                `updated_at` TIMESTAMP NULL COMMENT '수정 일시',
                                `cancelled_at` TIMESTAMP NULL COMMENT '결제 취소 일시',
                                CONSTRAINT `FK_wallets_TO_transactions` FOREIGN KEY (`wallet_id`) REFERENCES `wallets` (`id`),
                                CONSTRAINT `FK_cards_TO_transactions` FOREIGN KEY (`card_id`) REFERENCES `cards` (`id`),
                                CONSTRAINT `FK_bank_accounts_TO_transactions` FOREIGN KEY (`bank_account_id`) REFERENCES `bank_accounts` (`id`),
                                CONSTRAINT `FK_merchants_TO_transactions` FOREIGN KEY (`merchant_id`) REFERENCES `merchants` (`id`)
);

-- 7. 연동 가능 계좌 테이블
CREATE TABLE `linkable_accounts` (
                                     `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '연동 가능 계좌 고유 번호(PK)',
                                     `user_id` BIGINT NOT NULL COMMENT '회원 고유 번호(FK)',
                                     `bank_code` VARCHAR(10) NOT NULL COMMENT '은행 코드(FK)',
                                     `account_number` VARCHAR(50) NOT NULL COMMENT '계좌번호(Mock)',
                                     `product_name` VARCHAR(100) NOT NULL COMMENT '계좌 상품명',
                                     `is_linked` TINYINT(1) NULL DEFAULT 0 COMMENT '이미 연동(등록) 완료했는지 여부'
);

-- 8. 연동 가능 카드 테이블
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

-- 외래키 제약 조건 검사 재활성화
SET FOREIGN_KEY_CHECKS = 1;
-- ================================================================

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
                             `id`	BIGINT	PRIMARY KEY,
                             `id2`	BIGINT	NOT NULL,
                             `name`	VARCHAR(150)	NOT NULL,
                             `address`  VARCHAR(255) NOT NULL,
                             `taxpayer_identification_number` VARCHAR(10) NULL COMMENT '사업자등록번호(하이픈 없이 숫자만)',
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

CREATE TABLE `activities` (
                              `id`	BIGINT	NOT NULL,
                              `merchant_id`	BIGINT	NOT NULL,
                              `activity_type`	ENUM( 'MARINE', 'SPORTS', 'HEALING', 'CULTURE', 'FESTIVAL', 'SHOPPING', 'ETC' )	NOT NULL,
                              `difficulty`	ENUM( 'EASY', 'NORMAL', 'HARD' )	NULL	DEFAULT 'NORMAL',
                              `duration_minutes`	INT	NULL
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

CREATE TABLE `reservation_cancels` (
                                       `id`	BIGINT	NOT NULL	COMMENT '예약 취소 고유번호(PK)',
                                       `reservation_id`	BIGINT	NOT NULL	COMMENT '예약 고유번호(FK)',
                                       `cancel_fee`	DECIMAL(15, 2)	NOT NULL	DEFAULT 0	COMMENT '취소 수수료',
                                       `refund_amount`	DECIMAL(15, 2)	NOT NULL	DEFAULT 0	COMMENT '환불 금액',
                                       `canceled_at`	TIMESTAMP	NULL	DEFAULT CURRENT_TIMESTAMP	COMMENT '취소 시각',
                                       `refunded_at`	TIMESTAMP	NULL	COMMENT '환불 완료 시각'
);


ALTER TABLE `restaurants` ADD CONSTRAINT `PK_RESTAURANTS` PRIMARY KEY (
                                                                       `id`

    );

ALTER TABLE `merchant_tags` ADD CONSTRAINT `PK_MERCHANT_TAGS` PRIMARY KEY (
                                                                           `merchant_id`,
                                                                           `tag_id`
    );

ALTER TABLE `survey_questions` ADD CONSTRAINT `PK_SURVEY_QUESTIONS` PRIMARY KEY (
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


ALTER TABLE `reservations` ADD CONSTRAINT `PK_RESERVATIONS` PRIMARY KEY (
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
                                                                             `id`
    );

ALTER TABLE `activities` ADD CONSTRAINT `PK_ACTIVITIES` PRIMARY KEY (
                                                                     `id`,
                                                                     `merchant_id`
    );

ALTER TABLE `reservation_products` ADD CONSTRAINT `PK_RESERVATION_PRODUCTS` PRIMARY KEY (
                                                                                         `id`
    );

ALTER TABLE `reservation_cancels` ADD CONSTRAINT `PK_RESERVATION_CANCELS` PRIMARY KEY (
                                                                                       `id`
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

-- ========================================================================================
-- 워케이션 파트 외래키 (담당: 김태균)
-- transactions / cards / merchants 가 위에서 생성된 이후에 실행되어야 하므로 하단에 배치했습니다.
-- =========================================================================================
ALTER TABLE `workation_expenses` ADD CONSTRAINT `fk_workation_expenses_transaction_id` FOREIGN KEY (`transaction_id`) REFERENCES `transactions` (`id`);
ALTER TABLE `workation_expenses` ADD CONSTRAINT `fk_workation_expenses_card_id`        FOREIGN KEY (`card_id`)        REFERENCES `cards` (`id`);
ALTER TABLE `user_category_rules` ADD CONSTRAINT `fk_user_category_rules_merchant_id`  FOREIGN KEY (`merchant_id`)    REFERENCES `merchants` (`id`);
-- =========================================================================================
-- 워케이션 파트 기본 데이터 (담당: 김태균)
-- expense_categories 는 서비스 동작에 필수인 마스터 데이터입니다.
-- 이 데이터가 없으면 카테고리 조회 / 예산 배분 / 지출 자동분류가 모두 동작하지 않습니다.
-- =========================================================================================

-- =========================================================================================
-- 1. 워케이션 거점 지역
-- =========================================================================================
INSERT INTO `region` (`name`) VALUES
    ('부산'),
    ('강릉'),
    ('여수'),
    ('제주');
-- =========================================================================================
-- 2. 지출 카테고리 마스터 - 법인용 12건
--    is_default = 1 : 예산 화면에 처음부터 표출 (6건)
--    is_default = 0 : ＋ 버튼을 눌렀을 때 목록에만 표시
--    is_deletable = 0 : 기타 카테고리는 분류되지 않은 지출이 모이는 곳이라 삭제 불가
--    user_id = NULL : 전체 사용자 공통 기본 카테고리
-- =========================================================================================
INSERT INTO `expense_categories`
    (`user_id`, `budget_type`, `code`, `name`, `description`, `is_default`, `is_deletable`, `sort_order`)
VALUES
    (NULL, 'WORK', 'ACCOMMODATION',  '숙박비',     '호텔·에어비앤비 등 숙소 요금',      1, 1,  1),
    (NULL, 'WORK', 'TRANSPORTATION', '교통비',     '항공·철도·버스·현지 이동',          1, 1,  2),
    (NULL, 'WORK', 'RENT',           '임차료',     '공유오피스·회의실 대여 등',         1, 1,  3),
    (NULL, 'WORK', 'MEETING',        '회의비',     '업무 미팅 중 식음료·다과',          1, 1,  4),
    (NULL, 'WORK', 'FOOD',           '식비',       '근무일 식대',                       1, 1,  5),
    (NULL, 'WORK', 'ETC',            '기타',       '위 항목에 없는 지출',               1, 0,  6),
    (NULL, 'WORK', 'COMMUNICATION',  '통신비',     '데이터·와이파이 등 업무 통신',      0, 1,  7),
    (NULL, 'WORK', 'SUPPLIES',       '소모품비',   '업무용 소모품 구입',                0, 1,  8),
    (NULL, 'WORK', 'ENTERTAINMENT',  '접대비',     '거래처 접대 비용',                  0, 1,  9),
    (NULL, 'WORK', 'VEHICLE',        '차량유지비', '렌터카·주유·주차 등',               0, 1, 10),
    (NULL, 'WORK', 'EDUCATION',      '교육·도서비','업무 관련 교육·도서 구입',          0, 1, 11),
    (NULL, 'WORK', 'INSURANCE',      '보험료',     '여행자보험 등',                     0, 1, 12);
-- =========================================================================================
-- 3. 지출 카테고리 마스터 - 개인용 11건
-- =========================================================================================
INSERT INTO `expense_categories`
    (`user_id`, `budget_type`, `code`, `name`, `description`, `is_default`, `is_deletable`, `sort_order`)
VALUES
    (NULL, 'PERSONAL', 'ACCOMMODATION',  '숙박비',           '개인 부담 숙소 요금',        1, 1,  1),
    (NULL, 'PERSONAL', 'TRANSPORTATION', '교통비',           '개인 이동 비용',             1, 1,  2),
    (NULL, 'PERSONAL', 'FOOD',           '식비',             '식사 비용',                  1, 1,  3),
    (NULL, 'PERSONAL', 'LEISURE',        '여가비',           '관광·액티비티·문화생활',     1, 1,  4),
    (NULL, 'PERSONAL', 'SHOPPING',       '쇼핑',             '기념품·의류 등 구매',        1, 1,  5),
    (NULL, 'PERSONAL', 'ETC',            '기타',             '위 항목에 없는 지출',        1, 0,  6),
    (NULL, 'PERSONAL', 'CAFE',           '카페·간식',        '커피·디저트·간식',           0, 1,  7),
    (NULL, 'PERSONAL', 'GATHERING',      '모임비',           '지인·동료와의 모임 비용',    0, 1,  8),
    (NULL, 'PERSONAL', 'HEALTH',         '건강·의료',        '약국·병원·운동',             0, 1,  9),
    (NULL, 'PERSONAL', 'LAUNDRY',        '세탁·생활서비스',  '세탁·생활 편의 서비스',      0, 1, 10),
    (NULL, 'PERSONAL', 'COMMUNICATION',  '통신비',           '개인 데이터·로밍',           0, 1, 11);
-- =========================================================================================
-- 4. 가맹점 업종 - 카테고리 기본 매핑 (자동분류 2단계)
--    merchants.category 값을 기준으로 지출을 어느 카테고리로 분류할지 정한다
--    merchants 테이블의 category 값이 확정되면 매핑을 추가·조정합니다
-- =========================================================================================
INSERT INTO `merchant_category_mappings` (`budget_type`, `merchant_category`, `expense_category_id`)
SELECT 'WORK', 'ACCOMMODATION', id FROM `expense_categories` WHERE `budget_type` = 'WORK' AND `code` = 'ACCOMMODATION';

INSERT INTO `merchant_category_mappings` (`budget_type`, `merchant_category`, `expense_category_id`)
SELECT 'WORK', 'TRANSPORT', id FROM `expense_categories` WHERE `budget_type` = 'WORK' AND `code` = 'TRANSPORTATION';

INSERT INTO `merchant_category_mappings` (`budget_type`, `merchant_category`, `expense_category_id`)
SELECT 'WORK', 'OFFICE', id FROM `expense_categories` WHERE `budget_type` = 'WORK' AND `code` = 'RENT';

INSERT INTO `merchant_category_mappings` (`budget_type`, `merchant_category`, `expense_category_id`)
SELECT 'WORK', 'RESTAURANT', id FROM `expense_categories` WHERE `budget_type` = 'WORK' AND `code` = 'FOOD';

INSERT INTO `merchant_category_mappings` (`budget_type`, `merchant_category`, `expense_category_id`)
SELECT 'PERSONAL', 'ACCOMMODATION', id FROM `expense_categories` WHERE `budget_type` = 'PERSONAL' AND `code` = 'ACCOMMODATION';

INSERT INTO `merchant_category_mappings` (`budget_type`, `merchant_category`, `expense_category_id`)
SELECT 'PERSONAL', 'TRANSPORT', id FROM `expense_categories` WHERE `budget_type` = 'PERSONAL' AND `code` = 'TRANSPORTATION';

INSERT INTO `merchant_category_mappings` (`budget_type`, `merchant_category`, `expense_category_id`)
SELECT 'PERSONAL', 'RESTAURANT', id FROM `expense_categories` WHERE `budget_type` = 'PERSONAL' AND `code` = 'FOOD';

INSERT INTO `merchant_category_mappings` (`budget_type`, `merchant_category`, `expense_category_id`)
SELECT 'PERSONAL', 'CAFE', id FROM `expense_categories` WHERE `budget_type` = 'PERSONAL' AND `code` = 'CAFE';

INSERT INTO `merchant_category_mappings` (`budget_type`, `merchant_category`, `expense_category_id`)
SELECT 'PERSONAL', 'ACTIVITY', id FROM `expense_categories` WHERE `budget_type` = 'PERSONAL' AND `code` = 'LEISURE';