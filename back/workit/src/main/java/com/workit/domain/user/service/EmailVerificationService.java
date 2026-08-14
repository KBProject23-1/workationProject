package com.workit.domain.user.service;

// 이메일 인증번호 발송 서비스 (User 도메인 — 이메일 변경 사전 단계)
//
// 책임:
// - 이메일 발송 로직과 인증번호 관리 로직을 분리한다 (docs: Mock 구현은 실제 이메일 서비스
//   연동으로 교체할 수 있도록 이메일 발송 로직과 인증번호 관리 로직을 분리)
//   - 이메일 발송 로직: 이 서비스 (인증번호 생성 + 발송 — 개발 환경에서는 [MOCK EMAIL] 로그)
//   - 인증번호 관리 로직: EmailVerificationStore (임시 저장, TTL, 재발급 시 기존 인증번호 무효화)
// - 실제 SMTP/이메일 발송 서비스가 연동되면 이 인터페이스의 구현체만 실제 발송 구현으로 교체한다
//   (UserService 는 인터페이스에만 의존 — 교체 시 UserService 변경 불필요)
//
// 인증 세션은 사용자(userId) 기준으로 저장한다 (docs: 서버가 userId + email + 인증번호 저장)
// - 이메일 변경 API 는 Request Body 를 받지 않으므로, 인증 완료 정보를 userId 로만 조회할 수 있어야 한다
//
// API 흐름:
//   UserController → UserService → EmailVerificationService(이 인터페이스) → EmailVerificationStore
public interface EmailVerificationService {

    /**
     * 이메일 인증번호 발급 + 임시 저장 (Mock 발송)
     *
     * 흐름:
     *   1. 6자리 숫자 인증번호 생성 (docs: 6자리 숫자)
     *   2. 인증번호 세션 구성 — email AES-256 암호화, 만료 시각(expiresAt) = now + 저장소 TTL
     *   3. 임시 저장 — 같은 사용자(key)의 기존 인증번호는 덮어써져 무효화된다
     *      (docs: 재발급 시 기존 인증번호 폐기 — 사용자당 유효한 인증 세션은 하나)
     *   4. 실제 이메일은 발송하지 않는다 — 개발 편의를 위해 [MOCK EMAIL] 로그 출력
     *      (docs: [MOCK EMAIL] email=new@example.com, verificationCode=123456)
     *
     * 이메일 형식/사용자 검증은 호출 측(UserService)에서 수행 완료된 값만 전달받는다.
     *
     * @param userId 로그인 사용자 id (인증 세션 key)
     * @param email 정규화된(trim + lowercase) 인증 대상 이메일
     */
    void issueVerificationCode(Long userId, String email);

    /**
     * 이메일 인증번호 확인 (Mock 검증)
     *
     * 흐름 (docs: 이메일 인증번호 확인 처리 로직):
     *   1. 해당 사용자의 인증정보 조회 — 없으면(TTL 만료/미발급) EMAIL_VERIFICATION_NOT_FOUND(400)
     *   2. 요청 이메일과 인증 세션에 저장된 이메일 대조 — 다르면 인증정보 없음으로 처리
     *      (사용자 기준 단일 세션이므로, 인증번호를 받지 않은 이메일은 확인할 수 없다)
     *   3. 인증번호 유효시간(5분) 만료 확인 — 만료 시 EMAIL_VERIFICATION_CODE_EXPIRED(400)
     *   4. 이미 인증 완료(verified) 된 인증정보 재사용 확인 → EMAIL_ALREADY_VERIFIED(400)
     *      (인증 성공 후 동일 인증번호 재사용 방지)
     *   5. 사용자 입력 인증번호와 저장된 인증번호 비교 — 불일치 → EMAIL_VERIFICATION_CODE_INVALID(400)
     *   6. 일치 시 인증정보를 verified=true 로 변경해 저장 (인증 완료 상태 유지 —
     *      이후 이메일 변경 API 가 인증 완료된 이메일을 조회/사용)
     *
     * 인증번호/사용자 검증은 호출 측(UserService)에서 수행 완료된 값만 전달받는다.
     *
     * @param userId            로그인 사용자 id (인증 세션 key)
     * @param email             정규화된(trim + lowercase) 인증 대상 이메일
     * @param verificationCode  사용자가 입력한 6자리 인증번호
     */
    void confirmVerificationCode(Long userId, String email, String verificationCode);

    /**
     * 인증 완료된 이메일 조회 — 이메일 변경 API 가 Request Body 없이 현재 사용자의 인증 완료 정보를 사용
     *
     * 흐름 (docs: 이메일 변경 처리 로직):
     *   1. 해당 사용자의 인증정보 조회 — 없으면(TTL 만료/미발급) → EMAIL_VERIFICATION_REQUIRED(400)
     *   2. 인증정보 유효시간 만료 확인 — 만료 시 → EMAIL_VERIFICATION_REQUIRED(400)
     *   3. 인증 완료 상태(VERIFIED) 확인 — 인증번호 확인을 완료하지 않았으면 → EMAIL_VERIFICATION_REQUIRED(400)
     *   4. 세션에 저장된 이메일 AES-256 복호화 후 반환 (Service Layer 에서만 복호화)
     *
     * 세부 실패 원인(미발급/만료/미인증)은 구분해 노출하지 않는다 (docs: 모두 \"이메일 인증 필요\").
     *
     * @param userId 로그인 사용자 id (인증 세션 key)
     * @return 인증 완료된 이메일 (정규화된 값 — 복호화 결과)
     */
    String getVerifiedEmail(Long userId);

    /**
     * 이메일 인증 세션 소비(삭제) — 이메일 변경 성공 후 동일 인증 결과 재사용 방지
     *
     * - Redis 삭제는 DB 트랜잭션과 분리하여 DB 커밋 확정 후(afterCommit) 수행한다
     *   (knowledge.md: Redis 는 DB 트랜잭션의 일부가 아니며, 롤백 시 세션이 사라지면 재시도 불가)
     *
     * @param userId 로그인 사용자 id (인증 세션 key)
     */
    void consumeVerification(Long userId);
}
