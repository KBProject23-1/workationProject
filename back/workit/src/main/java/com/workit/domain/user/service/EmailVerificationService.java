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
// API 흐름:
//   UserController → UserService → EmailVerificationService(이 인터페이스) → EmailVerificationStore
public interface EmailVerificationService {

    /**
     * 이메일 인증번호 발급 + 임시 저장 (Mock 발송)
     *
     * 흐름:
     *   1. 6자리 숫자 인증번호 생성 (docs: 6자리 숫자)
     *   2. 인증번호 세션 구성 — email AES-256 암호화, 만료 시각(expiresAt) = now + 저장소 TTL
     *   3. 임시 저장 — 같은 이메일의 기존 인증번호는 덮어써져 무효화된다 (docs: 재발급 시 기존 인증번호 폐기)
     *   4. 실제 이메일은 발송하지 않는다 — 개발 편의를 위해 [MOCK EMAIL] 로그 출력
     *      (docs: [MOCK EMAIL] email=new@example.com, verificationCode=123456)
     *
     * 이메일 형식/사용자 검증은 호출 측(UserService)에서 수행 완료된 값만 전달받는다.
     *
     * @param email 정규화된(trim + lowercase) 인증 대상 이메일
     */
    void issueVerificationCode(String email);
}
