package com.workit.domain.user.service;

import com.workit.domain.user.exception.UserErrorCode;
import com.workit.exception.BusinessException;
import com.workit.global.util.PersonalDataCipher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;

// 이메일 인증번호 발송 Mock 구현 (개발/테스트용 — 실제 SMTP/이메일 서비스 연동 전까지)
//
// - 실제 이메일은 발송하지 않는다 — 서버가 6자리 숫자 인증번호를 생성해 임시 저장하고,
//   개발 편의를 위해 로그에 [MOCK EMAIL] 형태로 출력한다 (docs 명시 형식)
// - 인증번호 관리(저장/TTL/재발급 시 기존 인증번호 무효화)는 EmailVerificationStore 에 위임 —
//   이메일 발송 로직과 인증번호 관리 로직이 분리되어 있어, 운영 환경에서는
//   이 구현체를 실제 이메일 발송 구현으로 교체할 수 있다 (docs: 교체 가능하도록 분리)
// - 이메일(개인정보)은 Service Layer 에서만 AES-256 암호화한다
//   (knowledge.md: Redis 개인정보 원문 저장 금지 — MockPassServiceImpl 과 동일)
// - Redis 임시 저장은 DB 트랜잭션과 무관한 side-effect 이므로 별도 @Transactional 을 사용하지 않는다
//   (MockPassServiceImpl 과 동일)
@Service
@Slf4j
public class MockEmailVerificationServiceImpl implements EmailVerificationService {

    /** 인증번호 자릿수 (docs: 6자리 숫자) */
    private static final int CODE_LENGTH = 6;

    /** 인증번호 생성용 CSPRNG — 예측 불가능한 난수 (보안 정책) */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final EmailVerificationStore emailVerificationStore;

    public MockEmailVerificationServiceImpl(EmailVerificationStore emailVerificationStore) {
        this.emailVerificationStore = emailVerificationStore;
    }

    @Override
    public void issueVerificationCode(String email) {

        // 1. 6자리 숫자 인증번호 생성 (SecureRandom — 예측 불가)
        String verificationCode = generateVerificationCode();

        // 2. 인증 세션 구성 — email 은 AES-256 암호화본만 저장 (Redis 원문 저장 금지)
        //    - expiresAt 은 저장소가 소유한 TTL(기본 5분) 기준으로 계산 (Service 하드코딩 금지)
        //    - verified=false 로 시작 (인증번호 확인 API 성공 시 true 전환)
        EmailVerificationSession session = EmailVerificationSession.builder()
                .encryptedEmail(PersonalDataCipher.encrypt(email))
                .verificationCode(verificationCode)
                .expiresAt(Instant.now().plus(emailVerificationStore.getTtl()).toEpochMilli())
                .verified(false)
                .build();

        // 3. 임시 저장 — 같은 이메일(key)에 덮어써 기존 인증번호를 무효화한다 (docs)
        //    - 저장 실패(직렬화 오류 등) → EMAIL_VERIFICATION_SEND_FAILED(500) 으로 변환 (docs: 인증번호 발급 실패)
        try {
            emailVerificationStore.save(email, session);
        } catch (RuntimeException e) {
            throw new BusinessException(UserErrorCode.EMAIL_VERIFICATION_SEND_FAILED);
        }

        // 4. 개발 편의 — 실제 이메일 발송 대신 Mock 로그 출력 (docs 명시 형식)
        //    - 운영 환경에서는 실제 이메일 발송 구현으로 교체되어 이 로그 대신 실제 발송이 수행된다
        //    - docs 가 개발 편의를 위해 명시적으로 요구하는 Mock 로그 형식 — 지식 규칙과 별개로 허용
        log.info("[MOCK EMAIL] email={}, verificationCode={}", email, verificationCode);
    }

    /** 6자리 숫자 인증번호 생성 (각 자리 0~9, SecureRandom) */
    private String generateVerificationCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(SECURE_RANDOM.nextInt(10));
        }
        return sb.toString();
    }
}
