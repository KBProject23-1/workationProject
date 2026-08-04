-- =====================================================================================
-- users 이메일/이름 암호화 컬럼 크기 조정 마이그레이션
-- 상태: 승인 완료 → database/ERD.sql 에 반영됨 (실 DB 적용 시 이 파일의 ALTER 를 실행)
--
-- 배경 (분석 결과):
--   PersonalDataCipher 저장 형식 = Base64(IV 12B + AES-256/GCM ciphertext(평문+tag 16B))
--   → 암호문 전체 바이트 = 평문 길이 + 28, Base64 변환 후 = ceil((평문+28)/3) * 4
--
--   현재 users.email_encrypt VARCHAR(100):
--     - 수용 가능 최대 이메일 평문: 47자
--     - 48자 이상 이메일 → insert 시 "Data too long for column" (500) 발생
--
--   서비스 이메일 검증 정책 (EmailValidator.MAX_EMAIL_LENGTH = 254, RFC 5321):
--     - 최대 254자 이메일 → 암호문 282B → Base64 376자
--     - 따라서 VARCHAR(100) 으로는 254자 정책을 수용할 수 없음
--
--   결론: email_encrypt 를 VARCHAR(400) 으로 확장 (254자 + 여유 18자)
--
-- 참고 (동일 유형 점검 결과):
--   users.name_encrypt 는 한글 3자("홍길동", UTF-8 9B)도 52자로 VARCHAR(50) 초과 →
--   이번 승인에 포함하여 VARCHAR(100) 으로 함께 확장.
--   users.phone_number_encrypt VARCHAR(255): 11자 핸드폰 → 52자, 여유 충분.
--   users.email_hash VARCHAR(100): SHA-256 hex 64자, 문제 없음.
-- =====================================================================================

-- 1. 이메일 암호화 컬럼 확장 (254자 이메일까지 수용)
ALTER TABLE `users`
    MODIFY COLUMN `email_encrypt` VARCHAR(400) NOT NULL COMMENT '유저 이메일 (로그인 ID) AES';

-- 2. 이름 암호화 컬럼 확장 — 한글 이름도 수용 (예: "홍길동" → 52자 초과)
ALTER TABLE `users`
    MODIFY COLUMN `name_encrypt` VARCHAR(100) NOT NULL COMMENT '유저 이름 AES';
