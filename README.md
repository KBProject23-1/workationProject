# WorkIt — 워케이션 관리 플랫폼 백엔드

> KB IT's Your Life 7기 프로젝트 — 기업 임직원 워케이션 관리 플랫폼의 백엔드 서버

## 프로젝트 개요

워케이션(Work + Vacation) 기반 기업 복지 플랫폼의 백엔드 API 서버입니다.  
임직원이 워케이션 일정을 등록하고, 숙소·공유오피스·음식점·여가 활동을 예약하며, 지출 내역을 관리하고 정산까지 처리할 수 있는 REST API를 제공합니다.

**현재 거점 지역**: 제주특별자치도 ( TourAPI 연동으로 숙소·음식점·여가 활동 자동 수집)

## 기술 스택

| 구분 | 기술 |
|------|------|
| **언어** | Java 8 (JDK 1.8) |
| **프레임워크** | Spring MVC 5.3 + Spring Security 5.7 |
| **ORM** | MyBatis 3.5 |
| **데이터베이스** | MySQL 8.x |
| **데이터베이스 커넥션 풀** | HikariCP 4.0 |
| **캐시/세션** | Redis (Spring Data Redis 2.7 + Lettuce 6.2) |
| **인증** | JWT (jjwt 0.11.5) — HttpOnly Cookie 기반, CSRF 방어 |
| **빌드** | Gradle (WAR 배포) |
| **로깅** | Log4j2 |
| **파일 생성** | Apache POI (Excel), OpenPDF (PDF, CJK 폰트 지원) |
| **公共场所 데이터** | 한국관광공사 TourAPI 2.0 (RestTemplate 기반 동기화) |
| **테스트** | JUnit 5 + Mockito 4.11 |

## 프로젝트 구조

```
workationProject/
├── .github/
│   ├── ISSUE_TEMPLATE/
│   └── PULL_REQUEST_TEMPLATE.md
├── database/
│   ├── ERD.sql                          # 전체 테이블 DDL + 기본 데이터
│   ├── agreements/                       # 약관 동의 데이터 (5건)
│   └── notification-templates/           # 알림 템플릿 데이터
└── back/
    └── workit/
        ├── build.gradle                  # 의존성 및 빌드 설정
        ├── TOURISM_REST_TEMPLATE.md      # TourAPI 동기화 가이드 문서
        └── src/
            ├── main/
            │   ├── java/com/workit/
            │   │   ├── config/           # Spring 설정 (Root, Servlet, Security, Redis, Web)
            │   │   ├── security/         # JWT 필터, @CurrentUser, 접근 제어
            │   │   ├── exception/        # 전역 예외 처리 (BusinessException, ErrorCode)
            │   │   ├── global/           # 공통 응답, DTO, 유틸리티
            │   │   │   ├── dto/          # CommonResponse, PageResponseDTO
            │   │   │   ├── response/     # GlobalResponseFactory
            │   │   │   ├── util/         # 암호화, 마스킹, 파일 업로드, 이메일 검증
            │   │   │   └── constant/     # PaymentPolicy 등 상수
            │   │   └── domain/           # 비즈니스 도메인 (아래 상세)
            │   ├── resources/
            │   │   ├── application.properties
            │   │   ├── application-secret.properties  (gitignored)
            │   │   ├── mybatis-config.xml
            │   │   └── log4j2.xml
            │   └── webapp/               # WAR 배포용 웹 앱 루트
            └── test/                     # 38개 테스트 파일
```

## 도메인 구조

각 도메인은 `controller → service → mapper` 3계층으로 구성되며, 도메인별 예외 코드와 VO를 가집니다.

```
domain/
├── auth/           # 인증 (로그인, 회원가입, 비밀번호 재설정, PASS 본인인증)
├── user/           # 사용자 (프로필, 이메일 인증, 회원 탈퇴)
├── security/       # 보안 (PIN 검증, 기기 관리)
├── account/        # 계좌 (연동, 조회)
├── card/           # 카드 (연동, 조회, 닉네임)
├── wallet/         # 지갑 (충전, 환불, 잔액 조회)
├── transaction/    # 거래 내역 (입출금, 결제 이력)
├── ledger/         # 복식부기 원장 (잔액 진실 원천, append-only)
├── payment/        # 결제 (PG 경계 추상화, 오케스트레이션, 상태머신)
├── workation/      # 워케이션 (등록, 수정, 삭제, 상태 관리)
├── budget/         # 예산 (카테고리별 배정, 경고 알림)
├── expense/        # 지출 (내역 관리, 자동분류, 요약)
├── category/       # 카테고리 (지출 카테고리 CRUD)
├── schedule/       # 일정 (방문 계획, 1시간 전 알림)
├── reservation/    # 예약 (숙소/공유오피스 예약, 재고 관리, 취소)
├── recommendation/ # 추천 (숙소, 공유오피스, 음식점, 여가)
├── survey/         # 설문 (워케이션 스타일 설문조사)
├── merchant/       # 가맹점 (숙소, 공유오피스, 음식점, 여가)
├── tourism/        # TourAPI 연동 (데이터 동기화, 스케줄러)
├── review/         # 리뷰 (작성, 수정, 삭제, 평점 동기화)
├── bookmark/       # 북마크
├── settlement/     # 정산 (문서 생성, Excel/PDF 다운로드, 알림)
└── notification/   # 알림 (템플릿 기반, 카테고리별 수신 설정)
```

## 주요 기능

### 인증 및 회원 관리
- PASS 본인인증 기반 회원가입 (중복가입 방지 — CI 해시)
- 쿠키 기반 JWT 인증 (HttpOnly accessToken/refreshToken, Refresh Token Rotation)
- CSRF 방어 (XSRF-TOKEN Cookie + X-XSRF-TOKEN Header)
- 아이디 찾기 / 비밀번호 재설정 (Redis 기반 5분 토큰)
- PIN 번호 설정 및 변경 (BCrypt, 기기별, 재사용 방지)
- 프로필 관리 (닉네임, 회사명)
- 계정 설정 (이메일 변경, 휴대폰 번호 변경, 비밀번호 변경)
- 회원 탈퇴

### 보안 및 개인정보 보호
- 이메일, 이름, 휴대폰 번호 **이중 암호화** (SHA-256 해시 + AES 암호화)
- 비밀번호/PIN BCrypt 단방향 해시
- Redis 기반 Refresh Token 관리 (SHA-256 hash 저장)
- 로그인 실패 횟수 관리 (Redis)
- SQL 바인딩 파라미터 평문 노출 방지 (Logging 설정)

### 워케이션 관리
- 워케이션 등록/수정/삭제
- 진행 상태 관리 (ACTIVE, SETTLED)
- 법인/개인 예산 분리 관리
- D-1 알림 (시작/종료)

### 예산 및 지출
- 카테고리별 예산 배정 (법인 12종, 개인 11종)
- 지출 내역 등록 및 관리
- **자동분류 시스템**: 사용자 정정 규칙 → 가맹점 업종 매핑 → 기타
- 업무/개인 경비 구분
- 결제 시 업무/개인 선택값 저장
- 법인카드 미보유자 자동 분류

### 결제 시스템
- 카드 결제 / 지갑 충전·환불
- **복식부기 원장** (ledger_entries): 잔액의 진.truth 원천, append-only
- PG 경계 추상화 (FakePgClient — 실제 PG사 연동 준비)
- 상태머신 기반 거래 상태 관리 (REQUESTED → AUTHORIZED → PAID)
- 멱등성 키 기반 중복 결제 방지
- 결제 트랜잭션 데드락 재시도 (DeadlockRetrier)
- PIN 검증 기반 결제

### 예약 시스템
- 숙소/공유오피스 예약 상품 관리
- 날짜별 재고 관리 (product_daily_inventories)
- 예약 생성/취소/완료
- 서버 시작 시 예약 상태 스케줄러 확인

### 추천 시스템
- 카테고리별 추천 (숙소, 공유오피스, 음식점, 여가)
- 다중 점수 기반 랭킹 (가격, 선호도, 접근성, 평점)
- 기준 장소 기반 거리 계산
- 설문 기반 선호도 반영

### TourAPI 연동
- 한국관광공사 TourAPI 2.0 RestTemplate 기반 동기화
- 매일 오전 3시 자동 갱신 (cron 스케줄러)
- FULL/INCREMENTAL 동기화 모드
- 여가 9개 카테고리, 음식점, 숙소 제주 데이터 수집
- 상세 description 통합 저장 (HTML 태그 제거)
- 동기화 이력 관리 (tourism_sync_runs)

### 알림 시스템
- **템플릿 기반 공통 알림 생성** (notification_templates)
- 카테고리별 수신 설정 (예산, 충전/환불, 결제, 워케이션, 정산, 일정)
- 알림 유형별 중복 방지 (배치 처리로 N+1 쿼리 개선)
- 커서 기반 페이징 조회

### 정산
- 정산 문서 생성 (매출전표 포함)
- Excel 다운로드 (Apache POI)
- PDF 다운로드 (OpenPDF, 한글 CJK 폰트)
- 정산 지연 알림

## 데이터베이스 테이블

### 핵심 테이블 (ERD.sql 기준)

| 테이블 | 설명 |
|--------|------|
| `users` | 회원 기본 계정 (이중 암호화) |
| `user_auth` | 비밀번호, PASS CI |
| `user_profile` | 닉네임, 회사명 |
| `user_device` | 기기별 PIN 관리 |
| `terms` / `user_terms_agreements` | 약관 동의 |
| `user_notification_settings` | 알림 수신 설정 |
| `notification_histories` / `notification_templates` | 알림 이력/템플릿 |
| `region` | 거점 지역 |
| `workations` | 워케이션 일정 및 총예산 |
| `expense_categories` | 지출 카테고리 (법인 12종, 개인 11종) |
| `user_category_labels` | 카테고리 표시명 별칭 |
| `budgets` | 카테고리별 예산 배정 |
| `workation_expenses` | 지출 내역 |
| `merchant_category_mappings` | 가맹점 업종-카테고리 매핑 |
| `user_category_rules` | 사용자 정정 규칙 |
| `banks` / `card_companies` | 은행/카드사 마스터 |
| `wallets` | 전자 지갑 |
| `bank_accounts` | 연동 계좌 |
| `cards` | 연동 카드 |
| `transactions` | 거래 내역 |
| `ledger_entries` | 복식부기 원장 |
| `merchants` / `accommodations` / `offices` / `restaurants` / `activities` | 가맹점 |
| `reservation_products` / `product_daily_inventories` | 예약 상품/재고 |
| `reservations` / `reservation_daily_inventories` / `reservation_cancels` | 예약 |
| `reviews` | 리뷰 |
| `bookmarks` | 북마크 |
| `recommendation_requests` / `recommendation_results` | 추천 |
| `survey_questions` / `survey_options` / `user_surveys` / `user_survey_answers` | 설문 |
| `schedules` | 음식점·여가 방문 계획 |
| `tourism_sync_runs` / `tourism_merchant_sources` | TourAPI 동기화 |

## API 엔드포인트 (도메인별)

### 인증 (`/api/v1/auth`)
- `POST /signup` — 회원가입 완료
- `POST /signup/check-email` — 이메일 중복 확인
- `POST /signup/verify-identity` — PASS 본인인증 검증
- `POST /login` — 로그인 (이메일/비밀번호 또는 PIN)
- `POST /refresh` — Refresh Token 재발급
- `POST /logout` — 로그아웃
- `POST /me/pin` — PIN 최초 설정
- `PATCH /me/pin` — PIN 변경
- `POST /forgot-id` — 아이디 찾기
- `POST /forgot-password` — 비밀번호 재설정
- `POST /reset-password` — 새 비밀번호 설정

### 사용자 (`/api/v1/users`)
- `GET /me` — 내 정보 조회
- `PATCH /me` — 프로필 수정
- `PATCH /me/password` — 비밀번호 변경

### 워케이션 (`/api/v1/workations`)
- 워케이션 CRUD
- 예산 배정/수정
- 정산 기록 조회

### 가맹점 (`/api/v1/merchants`)
- 가맹점 목록/상세 조회
- 카테고리별 필터링

### 예약 (`/api/v1/reservations`)
- 예약 생성/목록/상세/취소
- 상품 목록/상세 조회

### 추천 (`/api/v1/recommendations`)
- 카테고리별 추천 조회
- 기준 장소 검색

### 지출/정산 (`/api/v1/workations/{id}/expenses`, `/api/v1/settlements`)
- 지출 등록/수정/삭제/목록
- 정산 문서 생성/다운로드

### 알림 (`/api/v1/notifications`)
- 커서 기반 목록 조회
- 읽지 않은 알림 개수
- 단건/전체 읽음 처리
- 카테고리별 수신 설정 조회/변경

### TourAPI (`/api/v1/tourism`)
- `POST /sync?mode=FULL|INCREMENTAL` — 전체/변경분 동기화
- `GET /sync/status` — 최근 동기화 결과
- `GET /categories` — 추천 카테고리 목록
- `GET /place-types` — 수집 상위 종류

## 설정

### 환경 변수 (`application-secret.properties`)

```properties
# DB
jdbc.driver=com.mysql.cj.jdbc.Driver
jdbc.url=jdbc:mysql://localhost:3306/workit?useSSL=false&serverTimezone=Asia/Seoul
jdbc.username=用户名
jdbc.password=비밀번호

# JWT
jwt.secret=시크릿키

# TourAPI
tourism.api.service-key=공공데이터포털_일반인증키
```

### 주요 설정 (`application.properties`)

| 설정 | 기본값 | 설명 |
|------|--------|------|
| `jwt.access-token-expiration` | 15분 | Access Token 유효 시간 |
| `jwt.refresh-token-expiration` | 20160분 (14일) | Refresh Token 유효 시간 |
| `tourism.sync.enabled` | true | TourAPI 자동 동기화 활성화 |
| `tourism.sync.cron` | 0 0 3 * * * | 매일 오전 3시 동기화 |
| `mock.pass.ttl.minutes` | 10분 | Mock PASS 인증 세션 유효 시간 |
| `mock.email-verification.ttl.minutes` | 5분 | 이메일 인증번호 유효 시간 |

## 빌드 및 실행

### 사전 요구사항

- JDK 1.8 (Java 8)
- Gradle
- MySQL 8.x
- Redis

### 빌드

```bash
cd back/workit
./gradlew clean build
```

### 실행

```bash
cd back/workit
./gradlew bootRun
# 또는 WAR 배포
./gradlew war
```

서버는 기본적으로 `8080` 포트에서 실행됩니다.

### 테스트

```bash
./gradlew test
```

38개 테스트 파일 (Controller, Service, Util 테스트 포함)

## 브랜치 전략

- `main`: 프로덕션 배포용
- `dev/back`: 개발 통합 브랜치
- `feat/*`, `feature/*`: 기능 개발 브랜치 (이슈 번호 포함)
- `fix/*`: 버그 수정 브랜치
- `refactor/*`: 리팩토링 브랜치
- `hotfix/*`: 긴급 수정 브랜치
- `test/*`: 테스트 브랜치
- `chore/*`: 설정 변경 브랜치

### 커밋 컨벤션

```
[#이슈번호] 타입: 설명
```

**타입**: `feat`, `fix`, `refactor`, `style`, `chore`, `hotfix`, `test`, `perf`, `docs`

## 기여자

| 이름 | 커밋 수 |
|------|---------|
| jack243453 | 112 |
| androktg | 85 |
| Jeonggil (jeonggilAhn) | 128 |
| JunYoung (junyoung00) | 20 |
| nue | 19 |
| soo | 17 |
| KIM HYE BIN | 10 |
| sunkon25 | 20 |

## 주요 이슈 및 PR

- [#245] 알림 템플릿 기반 공통 알림 생성, 도메인별 알림 구현 (입출금, 결제, 일정, 정산, 예산, 워케이션)
- [#248] 숙소/음식점 추천 리뷰 수 정합성 개선
- [#240] 알림 수신 설정 조회/변경, 커서 기반 목록 조회, 전체 읽음 처리
- [#241] 정산 매출전표 레이아웃 정리, 앱 결제 유입 시점 변경
- [#236] ERD 변경에 따른 회원 인증 및 사용자 도메인 리팩토링
- [#238] 리뷰 변경 시 CUD 평점 동기화
- [#230] 카테고리별 추천 데이터 파이프라인 구축
- [#226] 더미 데이터 생성 API
- [#220] ErrorCode 기반 예외 전환 (지출/예산/워케이션)
- [#218] 결제 취소 API 삭제
- [#208] 계정 설정 API (비밀번호 변경, 이메일 변경, 휴대폰 변경, 비밀번호 재인증)
- [#201] 회원 탈퇴 API
- [#200] 일정 시각 수정 API
- [#197] 비밀번호 재설정 및 Mock PASS 인증 수정
- [#191] HttpOnly Cookie 인증 및 CSRF 적용
- [#187] Mock PASS 인증 통합 (회원가입/아이디 찾기)
- [#186] 워케이션 삭제 시 예약 처리
- [#182] 워케이션 스케줄러 통합 조회 및 일정 등록/삭제 API
- [#177] 로그인 시 기기별 PIN 등록 여부 확인
- [#174] Mock PASS 인증 기반 회원가입
- [#176] 설문 사용자 단위 조회 및 삭제 시 보존
- [#168] 지출 요약 필터, 개인카드 업무 지출 지원
- [#163] 결제 오케스트레이션 PaymentService 이관, PG 경계 추상화, 멱등성 키, 원장 zero-sum
- [#157] 카드결제 PG 경계 추상화, ledger_entries 테이블, TransactionStatus enum
- [#152] 예약 결제 지출 유입 및 정산 완료 후 유입 차단
- [#151] 사용자 비밀번호 변경 API
- [#146] 사용자 프로필 수정 API
- [#143] 충전 파트 중복 결제 문제 해결 및 멱등성 키
- [#136] 충전/환불/결제 로직에 PIN 검증 추가
- [#135] PIN 재설정 API (기존 PIN 재사용 방지)
- [#131] PIN 최초 설정 API
- [#126] 비밀번호 재설정 API (Redis 토큰 관리)
- [#113] Refresh Token 기반 로그아웃
- [#108] Spring Security JWT 인증 계층 구현
- [#104] Refresh Token Rotation 기반 토큰 재발급
- [#99] 로그인 JWT 발급 및 Refresh Token 연동
- [#89] 최종 회원가입 완료 API
- [#65] PASS 본인인증 Provider 추상화 및 Mock 구현
- [#41] 예약 생성/취소/상세 조회 API
- [#40] 정산 API
- [#34] 워케이션 지출 API

## 참고 문서

- `TOURISM_REST_TEMPLATE.md` — TourAPI 동기화 사용법 (DB 준비, 동기화 모드, 저장 규칙, 상태 확인)
- `database/ERD.sql` — 전체 테이블 DDL 및 기본 데이터
- `database/agreements/` — 약관 동의 데이터 (전자금융거래, 마케팅, 개인정보, 개인정보 제3자 제공, 서비스)
- `database/notification-templates/` — 알림 템플릿 초기 데이터
