# RestTemplate 기반 TourAPI 동기화 사용법

## 무엇이 저장되는가

`merchants` 테이블의 구조는 변경하지 않습니다. 장소의 기본 정보만 기존 컬럼에
저장하고, TourAPI 동기화에 필요한 정보는 별도 테이블에서 관리합니다.

| 테이블 | 저장 내용 |
|---|---|
| `merchants` | 이름, 주소, 지역, 좌표, 전화번호, 사진 등 장소 기본 정보 |
| `activities` | 9개 관광 카테고리 중 해당 장소의 카테고리 |
| `restaurants` | 음식 종류, 가격 단계, 음식점 상세 설명 |
| `accommodations` | 숙소 종류, 상세 설명, 체크인·체크아웃 시각 |
| `tourism_merchant_sources` | TourAPI `contentid`, 수정 시각, 활성 상태 |
| `tourism_sync_runs` | 동기화 시작·완료 시각, 처리 건수, 성공·실패 상태 |

`tourism_merchant_sources.merchant_id`가 `merchants.id`를 연결합니다. 예를 들어
TourAPI `contentid=12345`가 merchant 100번으로 저장됐다면 보조 테이블에는
`merchant_id=100, content_id=12345`가 기록됩니다. 다음 동기화에서는
`content_id`로 100번 merchant를 찾아 UPDATE하므로 중복 생성되지 않습니다.

## 1. DB 준비

새 데이터베이스를 처음 만드는 경우에는 최종 테이블 구조가 반영된 아래 파일만
실행하면 됩니다.

```text
database/ERD.sql
```

이미 운영 중인 기존 데이터베이스를 업그레이드하는 경우에만 다음 통합 SQL을
한 번 실행합니다.

```text
database/migration_tourapi.sql
```

통합 SQL은 `merchants`를 ALTER하지 않고 아래 두 테이블을 만듭니다.

- `tourism_merchant_sources`
- `tourism_sync_runs`

또한 기존 `activities.activity_type`의 구형 값을 신형 값으로 안전하게
변환한 뒤, 최종 ENUM을 TourAPI 9개 카테고리와 `NONE`으로 맞춥니다. 이 작업이
빠지면 `Data truncated for column 'activity_type'` 오류가 발생합니다.

`activities.description`을 `LONGTEXT`로 추가하거나 확장하고,
상세정보 갱신 여부를 기록하는 `tourism_merchant_sources.detail_modified_time`을
추가합니다. `merchants` 테이블은 변경하지 않습니다.

`restaurants.description`과 `accommodations.description`을
`LONGTEXT`로 준비합니다. 음식점·숙소의 상세조회 결과가 길어도 잘리지 않게 하기
위한 변경이며, 역시 `merchants` 테이블은 변경하지 않습니다.

그리고 `region` 테이블에 다음 행이 있어야 합니다.

```text
제주
```

예전 버전의 마이그레이션을 이미 실행했다면 `merchants`에 추가된 컬럼은 현재
코드에서 사용하지 않습니다. 운영 데이터가 있을 수 있으므로 자동으로 DROP하지
않습니다.

## 2. TourAPI 인증키 설정

`src/main/resources/application-secret.properties`에 공공데이터포털에서 제공하는
일반 인증키를 그대로 넣습니다. `%2F`, `%2B`, `%3D`가 포함된 Encoding 형태여도
애플리케이션이 자동으로 한 번 디코딩한 후 올바르게 전송합니다. Decoding 형태의
키를 넣어도 그대로 사용할 수 있습니다.

```properties
tourism.api.service-key=포털에서_제공한_일반_인증키
```

인증키가 없으면 수동 호출과 자동 동기화 모두 TourAPI를 호출할 수 없습니다.

## 3. 최초 전체 수집

서버를 실행한 다음 아래 API를 한 번 호출합니다.

```http
POST /api/v1/tourism/sync?mode=FULL
```

로컬 서버가 8080 포트라면 다음과 같이 호출할 수 있습니다.

```bash
curl -X POST "http://localhost:8080/api/v1/tourism/sync?mode=FULL"
```

FULL 동기화는 여가 9개 카테고리와 음식점·숙소의 제주 데이터를 모두
페이지 단위로 가져옵니다. 과거에 저장됐지만 이번 전체 응답에서 더 이상 발견되지 않은 장소는
`tourism_merchant_sources.is_active=0`으로 바뀌고 추천·목록에서 숨겨집니다.
`merchants` 행은 삭제하지 않으므로 기존 북마크나 리뷰의 외래키는 유지됩니다.

## 4. 변경분 수집

직접 변경분만 갱신하려면 다음 API를 호출합니다.

```http
POST /api/v1/tourism/sync?mode=INCREMENTAL
```

INCREMENTAL은 전일과 당일의 변경된 활성·비활성 데이터를 조회합니다. 같은
`contentid`가 있으면 기존 merchant를 UPDATE하고, 처음 본 `contentid`면 새
merchant를 INSERT합니다.

수동 API와 매일 오전 3시 자동 실행은 모두 여가·음식점·숙소를 함께 갱신합니다.

## 5. 매일 자동 갱신

기본 설정은 매일 오전 3시, 한국 시간 기준입니다.

```properties
tourism.sync.enabled=true
tourism.sync.cron=0 0 3 * * *
tourism.sync.zone=Asia/Seoul
```

자동 갱신 시간을 바꾸고 싶으면 cron 설정만 변경하면 됩니다. 자동 실행을 끄려면
`tourism.sync.enabled=false`로 설정합니다.

## 6. 수집 지역

여가·음식점·숙소 모두 제주특별자치도 전체만 조회합니다.

- 제주특별자치도 전체 (`lDongRegnCd=50`)
- 여가: 9개 신분류 카테고리
- 음식점: `contentTypeId=39`
- 숙소: `contentTypeId=32`

주소를 수집한 다음 제주인지 판별하는 방식이 아니라 TourAPI 요청 자체에
`lDongRegnCd=50`을 전달합니다. 음식점·숙소 요청에는 중분류 필터를 걸지 않으므로
제주에 등록된 해당 타입 전체가 대상입니다.

주소 문자열로 지역을 추측하지 않습니다. 제주 요청 결과는 이름이 `제주`인
`region_id`에 직접 연결합니다.

## 7. 저장 규칙

- 여가는 `merchants.category=ACTIVITY`, 음식점은 `RESTAURANT`, 숙소는
  `ACCOMMODATION`으로 저장됩니다.
- TourAPI에 가격이 없으므로 `price=0`으로 저장됩니다.
- 평점은 제공되지 않으므로 `rating=NULL`로 저장됩니다.
- 기존 `merchants`의 주소·위도·경도는 NOT NULL입니다. TourAPI 응답에 주소 또는
  좌표가 없으면 잘못된 값으로 채우지 않고 해당 항목을 건너뜁니다.
- TourAPI에서 삭제된 장소는 merchant를 물리 삭제하지 않고 보조 테이블에서만
  비활성화합니다.

### 상세 description

신규 장소이거나 TourAPI `modifiedtime`이 바뀐 장소는 종류와 관계없이
`detailCommon2`와
`detailIntro2`를 추가 호출합니다. 응답의 HTML 태그를 제거하고 값이 있는 항목만
아래 형식으로 `activities.description`에 합쳐 저장합니다.

```text
[소개]
장소 소개 내용

[이용시간]
09:00~18:00

[휴무일]
매주 월요일

[주차정보]
주차 가능 / 무료
```

문의 전화번호는 description에 포함하지 않습니다. `detailCommon2.tel`을 우선
사용하고, 없으면 `detailIntro2`의 여가·음식점·숙소 문의 필드에서 전화번호를 추출해
`merchants.phone_number`에 저장합니다.

숙소는 상세조회에서 시각을 해석할 수 있는 경우 `check_in_time`과
`check_out_time`에도 `HH:mm:ss` 형식으로 저장합니다. 상세 문구 전체는
`accommodations.description`에 보존됩니다. 음식점 상세 문구는
`restaurants.description`에 저장됩니다.

### 음식점·숙소 타입 변환

분류 정의서의 신분류 코드를 사용합니다.

- 음식: `FD01→KOREAN`, `FD020100→CHINESE`, `FD020200→JAPANESE`,
  `FD03→WESTERN`(제과는 `DESSERT`), `FD04→BAR`, `FD05→CAFE`
- 숙박: `AC01/AC04→HOTEL`, `AC02→RESORT`, `AC03→PENSION`,
  `AC06→GUESTHOUSE`; 이름에 `풀빌라`가 있으면 `POOL_VILLA`

TourAPI는 가격 단계를 제공하지 않으므로 `restaurants.price_level=2`(중간/정보
없음), `merchants.price=0`(가격 정보 없음)으로 저장합니다.

상세 API 호출이 일시적으로 실패해도 기본 merchant 동기화는 계속 진행합니다.
실패한 항목의 상세 반영 시각은 기록하지 않으므로 다음 FULL 동기화에서 다시
시도합니다. 상세 API는 장소당 2회 호출하므로 개발계정 호출 한도에 도달하면
다음 날 FULL 동기화를 다시 실행하거나 운영 트래픽 증설이 필요할 수 있습니다.

## 8. 상태 확인

가장 최근 동기화 결과는 다음 API로 확인합니다.

```http
GET /api/v1/tourism/sync/status
```

`status=SUCCESS`면 정상 완료, `status=FAILED`면 `errorMessage`를 확인하면 됩니다.
지원하는 9개 카테고리는 다음 API로 확인할 수 있습니다.

```http
GET /api/v1/tourism/categories
```

수집되는 상위 종류(`ACTIVITY`, `RESTAURANT`, `ACCOMMODATION`)는 다음 API로
확인할 수 있습니다.

```http
GET /api/v1/tourism/place-types
```
