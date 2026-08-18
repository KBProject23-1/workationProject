USE workit;

-- =====================================================================================
-- 1. TourAPI에서 사용하는 세부 테이블을 최종 타입으로 맞춘다.
-- =====================================================================================

ALTER TABLE restaurants
    MODIFY COLUMN food_type ENUM (
        'KOREAN', 'JAPANESE', 'CHINESE', 'WESTERN', 'CAFE', 'DESSERT', 'BAR'
    ) NOT NULL,
    MODIFY COLUMN price_level TINYINT NOT NULL;

-- 구형 여가 ENUM을 사용하는 DB도 데이터 손실 없이 변경할 수 있도록 잠시 함께 허용한다.
ALTER TABLE activities
    MODIFY COLUMN activity_type ENUM (
        'MARINE', 'SPORTS', 'HEALING', 'CULTURE', 'FESTIVAL', 'SHOPPING', 'ETC',
        'WATER_SPORTS', 'LAND_SPORTS', 'RURAL_EXPERIENCE', 'WELLNESS_TOURISM',
        'CAFE_TEA_HOUSE', 'NATURAL_PARK', 'MOUNTAIN_SCENERY', 'WATER_SENERY',
        'NATURAL_ECOLOGY', 'NONE'
    ) NOT NULL;

UPDATE activities
   SET activity_type = CASE activity_type
       WHEN 'MARINE' THEN 'WATER_SPORTS'
       WHEN 'SPORTS' THEN 'LAND_SPORTS'
       WHEN 'HEALING' THEN 'WELLNESS_TOURISM'
       WHEN 'CULTURE' THEN 'NONE'
       WHEN 'FESTIVAL' THEN 'NONE'
       WHEN 'SHOPPING' THEN 'NONE'
       WHEN 'ETC' THEN 'NONE'
       ELSE activity_type
   END
 WHERE activity_type IN (
       'MARINE', 'SPORTS', 'HEALING', 'CULTURE', 'FESTIVAL', 'SHOPPING', 'ETC'
   );

ALTER TABLE activities
    MODIFY COLUMN activity_type ENUM (
        'WATER_SPORTS', 'LAND_SPORTS', 'RURAL_EXPERIENCE', 'WELLNESS_TOURISM',
        'CAFE_TEA_HOUSE', 'NATURAL_PARK', 'MOUNTAIN_SCENERY', 'WATER_SENERY',
        'NATURAL_ECOLOGY', 'NONE'
    ) NOT NULL;

-- =====================================================================================
-- 2. merchants를 변경하지 않고 TourAPI 동기화 상태를 관리한다.
-- =====================================================================================

CREATE TABLE IF NOT EXISTS tourism_sync_runs
(
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    mode              VARCHAR(20) NOT NULL,
    status            VARCHAR(20) NOT NULL,
    target_dates      VARCHAR(40) NULL,
    processed_count   INT NOT NULL DEFAULT 0,
    deactivated_count INT NOT NULL DEFAULT 0,
    error_message     VARCHAR(1000) NULL,
    started_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at       DATETIME NULL,
    KEY ix_tourism_sync_runs_started_at (started_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tourism_merchant_sources
(
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id          BIGINT NOT NULL,
    content_id           VARCHAR(30) NOT NULL COMMENT 'TourAPI contentid',
    modified_time        VARCHAR(14) NULL COMMENT 'TourAPI modifiedtime',
    last_seen_sync_id    BIGINT NULL COMMENT '마지막 FULL 동기화 실행 ID',
    detail_modified_time VARCHAR(14) NULL COMMENT '상세정보 반영 modifiedtime',
    is_active            TINYINT(1) NOT NULL DEFAULT 1,
    created_at           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_tourism_merchant_sources_merchant
        FOREIGN KEY (merchant_id) REFERENCES merchants (id) ON DELETE CASCADE,
    UNIQUE KEY ux_tourism_sources_merchant (merchant_id),
    UNIQUE KEY ux_tourism_sources_content (content_id),
    KEY ix_tourism_sources_active_sync (is_active, last_seen_sync_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- 이전 버전에서 보조 테이블만 만들어진 경우 상세 반영 시각 컬럼을 보충한다.
SET @detail_modified_exists = (
    SELECT COUNT(*)
      FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'tourism_merchant_sources'
       AND COLUMN_NAME = 'detail_modified_time'
);

SET @detail_modified_sql = IF(
    @detail_modified_exists = 0,
    'ALTER TABLE tourism_merchant_sources ADD COLUMN detail_modified_time VARCHAR(14) NULL COMMENT ''상세정보 반영 modifiedtime'' AFTER last_seen_sync_id',
    'SELECT 1'
);

PREPARE detail_modified_statement FROM @detail_modified_sql;
EXECUTE detail_modified_statement;
DEALLOCATE PREPARE detail_modified_statement;

-- =====================================================================================
-- 3. 상세조회 결과를 잘리지 않게 저장한다.
-- =====================================================================================

SET @activity_description_exists = (
    SELECT COUNT(*)
      FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'activities'
       AND COLUMN_NAME = 'description'
);

SET @activity_description_sql = IF(
    @activity_description_exists = 0,
    'ALTER TABLE activities ADD COLUMN description LONGTEXT NULL COMMENT ''TourAPI 상세보기 통합 내용'' AFTER activity_type',
    'ALTER TABLE activities MODIFY COLUMN description LONGTEXT NULL COMMENT ''TourAPI 상세보기 통합 내용'''
);

PREPARE activity_description_statement FROM @activity_description_sql;
EXECUTE activity_description_statement;
DEALLOCATE PREPARE activity_description_statement;

SET @restaurant_description_exists = (
    SELECT COUNT(*)
      FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'restaurants'
       AND COLUMN_NAME = 'description'
);

SET @restaurant_description_sql = IF(
    @restaurant_description_exists = 0,
    'ALTER TABLE restaurants ADD COLUMN description LONGTEXT NULL COMMENT ''TourAPI 상세보기 통합 내용'' AFTER price_level',
    'ALTER TABLE restaurants MODIFY COLUMN description LONGTEXT NULL COMMENT ''TourAPI 상세보기 통합 내용'''
);

PREPARE restaurant_description_statement FROM @restaurant_description_sql;
EXECUTE restaurant_description_statement;
DEALLOCATE PREPARE restaurant_description_statement;

ALTER TABLE accommodations
    MODIFY COLUMN description LONGTEXT NULL COMMENT 'TourAPI 상세보기 통합 내용';
