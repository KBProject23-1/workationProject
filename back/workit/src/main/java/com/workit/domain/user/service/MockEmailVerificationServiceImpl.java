package com.workit.domain.user.service;

import com.workit.domain.user.exception.UserErrorCode;
import com.workit.exception.BusinessException;
import com.workit.global.util.PersonalDataCipher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.security.SecureRandom;
import java.time.Instant;

// 이메일 인증번호 발송 Mock 구현 (개발/테스트용 — 실제 SMTP/이메일 서비스 연동 전까지)
//
// - 실제 이메일은 발송하지 않는다 — 서버가 6자리 숫자 인증번호를 생성해 임시 저장하고,
//   개발 편의를 위해 로그에 [MOCK EMAIL] 형태로 출력한다 (docs 명시 형식)
// - 인증번호 관리(저장/TTL/재발급 시 기존 인증번호 무효화)는 EmailVerificationStore 에 위임 —
//   이메일 발송 로직과 인증번호 관리 로직이 분리되어 있어, 운영 환경에서는
//   이 구현체를 실제 이메일 발송 구현으로 교체할 수 있다 (docs: 교체 가능하도록 분리)
// - 인증 세션은 사용자(userId) 기준으로 저장한다 (docs: 서버가 userId + email + 인증번호 저장)
//   - 이메일 변경 API 는 Request Body 를 받지 않으므로 인증 완료 정보를 userId 로만 조회한다
// - 이메일(개인정보)은 Service Layer 에서만 AES-256 암호화/복호화한다
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
    public void issueVerificationCode(Long userId, String email) {

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

        // 3. 임시 저장 — 같은 사용자(key)에 덮어써 기존 인증번호를 무효화한다 (docs)
        //    - 사용자당 유효한 인증 세션은 하나 — 새 이메일로 재발급하면 이전 세션이 폐기된다
        //    - 저장 실패(직렬화 오류 등) → EMAIL_VERIFICATION_SEND_FAILED(500) 으로 변환 (docs: 인증번호 발급 실패)
        try {
            emailVerificationStore.save(userId, session);
        } catch (RuntimeException e) {
            throw new BusinessException(UserErrorCode.EMAIL_VERIFICATION_SEND_FAILED);
        }

        // 4. 개발 편의 — 실제 이메일 발송 대신 Mock 로그 출력 (docs 명시 형식)
        //    - 운영 환경에서는 실제 이메일 발송 구현으로 교체되어 이 로그 대신 실제 발송이 수행된다
        //    - docs 가 개발 편의를 위해 명시적으로 요구하는 Mock 로그 형식 — 지식 규칙과 별개로 허용
        log.info("[MOCK EMAIL] email={}, verificationCode={}", email, verificationCode);
    }

    @Override
    public void confirmVerificationCode(Long userId, String email, String verificationCode) {

        // 1. 해당 사용자의 인증정보 조회 — 없으면(TTL 만료/미발급) 인증 실패
        //    (docs: 인증번호 발송 기록이 없음 → 400)
        EmailVerificationSession session = emailVerificationStore.find(userId);
        if (session == null) {
            throw new BusinessException(UserErrorCode.EMAIL_VERIFICATION_NOT_FOUND);
        }

        // 2. 요청 이메일과 인증 세션에 저장된 이메일 대조 — 다르면 인증정보 없음으로 처리
        //    - 사용자 기준 단일 세션이므로, 인증번호를 받지 않은 이메일은 확인할 수 없다
        //      (이메일-key 저장 시점의 \"해당 이메일 세션 없음\" 동작과 동일 — 원인 비노출)
        String storedEmail = PersonalDataCipher.decrypt(session.getEncryptedEmail());
        if (!storedEmail.equals(email)) {
            throw new BusinessException(UserErrorCode.EMAIL_VERIFICATION_NOT_FOUND);
        }

        // 3. 인증번호 유효시간(5분) 만료 확인 — 만료된 인증번호는 사용할 수 없다 (docs)
        //    - expiresAt 은 저장소 TTL 기준 발급 시각에 설정되며, epoch millis 로 비교한다
        if (session.getExpiresAt() <= System.currentTimeMillis()) {
            throw new BusinessException(UserErrorCode.EMAIL_VERIFICATION_CODE_EXPIRED);
        }

        // 4. 이미 인증 완료(verified) 된 인증정보 재사용 확인 (docs: 인증 성공 후 동일 인증번호 재사용 불가)
        //    - 인증 성공 시점에 verified=true 로 저장되므로, 이후 동일 인증번호로는 재인증할 수 없다
        if (session.isVerified()) {
            throw new BusinessException(UserErrorCode.EMAIL_ALREADY_VERIFIED);
        }

        // 5. 사용자 입력 인증번호와 저장된 인증번호 비교 — 불일치 시 인증 실패 (docs)
        //    - 인증번호는 6자리 숫자 1회성 값이므로 평문 비교 (Redis 에도 평문 보관 — 5분 TTL)
        if (!session.getVerificationCode().equals(verificationCode)) {
            throw new BusinessException(UserErrorCode.EMAIL_VERIFICATION_CODE_INVALID);
        }

        // 6. 인증 성공 — 인증정보를 verified=true 로 변경해 다시 저장한다 (인증 완료 상태)
        //    - 같은 사용자(key)에 덮어쓰므로 TTL(5분)이 재적용되어, 이후 이메일 변경 API가
        //      인증 완료 상태를 조회할 수 있도록 유지한다 (docs: 인증 완료 상태 저장 — 예: status VERIFIED)
        //    - verified=true 인 세션은 이후 동일 인증번호 재사용 시 4번에서 차단된다
        session.setVerified(true);
        try {
            emailVerificationStore.save(userId, session);
        } catch (RuntimeException e) {
            throw new BusinessException(UserErrorCode.EMAIL_VERIFICATION_SEND_FAILED);
        }
    }

    @Override
    public String getVerifiedEmail(Long userId) {

        // 1. 해당 사용자의 인증정보 조회 — 없으면 이메일 변경 불가 (docs: 인증정보 없음 → 이메일 인증 필요)
        EmailVerificationSession session = emailVerificationStore.find(userId);
        if (session == null) {
            throw new BusinessException(UserErrorCode.EMAIL_VERIFICATION_REQUIRED);
        }

        // 2. 인증정보 유효시간 만료 확인 — 만료 시 이메일 변경 불가 (docs: 인증정보 만료 → 이메일 인증 필요)
        if (session.getExpiresAt() <= System.currentTimeMillis()) {
            throw new BusinessException(UserErrorCode.EMAIL_VERIFICATION_REQUIRED);
        }

        // 3. 인증 완료 상태(VERIFIED) 확인 — 인증번호 확인을 완료하지 않았으면 이메일 변경 불가
        //    (docs: 이메일 인증 미완료 → 이메일 인증 필요)
        if (!session.isVerified()) {
            throw new BusinessException(UserErrorCode.EMAIL_VERIFICATION_REQUIRED);
        }

        // 4. 세션에 저장된 이메일 AES-256 복호화 후 반환 (Service Layer 에서만 복호화)
        //    - 프론트가 전달한 이메일이 아닌, 서버가 인증 완료 처리한 이메일 (docs 보안 조건)
        return PersonalDataCipher.decrypt(session.getEncryptedEmail());
    }

    @Override
    public void consumeVerification(Long userId) {

        // DB 트랜잭션이 커밋된 후(afterCommit)에만 인증 세션을 삭제한다 (재사용 방지)
        // - 이메일 변경의 users UPDATE 가 롤백되면 인증 세션도 유지되어 사용자가 재시도할 수 있어야 한다
        //   (knowledge.md: Redis 는 DB 트랜잭션의 일부가 아니며 커밋 확정 후 처리 —
        //    AuthServiceImpl.deleteRefreshTokenAfterCommit 과 동일 패턴)
        // - 실제 트랜잭션 밖(테스트 등)에서는 즉시 삭제한다
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    emailVerificationStore.delete(userId);
                }
            });
        } else {
            emailVerificationStore.delete(userId);
        }
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
