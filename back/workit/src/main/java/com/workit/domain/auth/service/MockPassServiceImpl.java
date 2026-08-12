package com.workit.domain.auth.service;

import com.workit.domain.auth.MockPassStatus;
import com.workit.domain.auth.dto.request.MockPassCompleteRequestDTO;
import com.workit.domain.auth.dto.response.MockPassStatusResponseDTO;
import com.workit.domain.auth.exception.AuthErrorCode;
import com.workit.exception.BusinessException;
import com.workit.global.util.PersonalDataCipher;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.regex.Pattern;

// Mock PASS 본인인증 서비스 구현
//
// - identityVerificationId 는 반드시 여기서 생성한다 (프론트 생성/전달 금지 — 요구사항 확정)
// - Mock CI 도 이 서비스가 생성해 세션에 보관한다 (실제 PASS 응답 흉내)
// - 개인정보(name/휴대폰/CI)는 Service Layer 에서만 AES-256 암호화/복호화 한다
//   (knowledge.md: Controller/Mapper 에서 암호화 금지, Redis 개인정보 원문 저장 금지)
// - Redis 세션 저장은 DB 트랜잭션과 무관한 side-effect 이므로 별도 @Transactional 을 사용하지 않는다
@Service
public class MockPassServiceImpl implements MockPassService {

    /** Mock CI 접두사 — 실제 CI 가 아니며 개인정보가 아님 */
    private static final String MOCK_CI_PREFIX = "MOCK-CI-";

    /** 휴대폰 번호 형식 — 숫자 10~11자리 (예: 01012345678) */
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\d{10,11}$");

    private final MockPassStore mockPassStore;

    public MockPassServiceImpl(MockPassStore mockPassStore) {
        this.mockPassStore = mockPassStore;
    }

    @Override
    public MockPassStatusResponseDTO complete(MockPassCompleteRequestDTO request) {

        // 1. 필수 값 검증 (javax.validation 미사용 환경 → Service Layer 에서 수행)
        if (request == null || isBlank(request.getName()) || isBlank(request.getPhoneNumber())) {
            throw new BusinessException(AuthErrorCode.INVALID_PASS_REQUEST);
        }
        String name = request.getName().trim();
        String phoneNumber = request.getPhoneNumber().trim();

        // 2. 휴대폰 형식 검증 — 숫자 10~11자리만 허용
        if (!PHONE_PATTERN.matcher(phoneNumber).matches()) {
            throw new BusinessException(AuthErrorCode.INVALID_PASS_REQUEST);
        }

        // 3. 인증 세션 데이터 생성 (전부 백엔드가 생성 — 프론트 입력/전달 없음)
        //    - identityVerificationId: 예측 불가능한 UUID (프론트가 만들거나 보내지 않는다)
        //    - CI: 동일 휴대폰 → 동일 CI 결정값 (아이디 찾기/비밀번호·PIN 재인증 일치 보장)
        //          실제 PASS 도 인증 세션과 무관하게 동일 인물에게 항상 동일 CI 를 반환한다
        String identityVerificationId = UUID.randomUUID().toString();
        String ci = MOCK_CI_PREFIX + sha256Hex(phoneNumber);

        // 4. VERIFIED 세션 등록 — 개인정보 AES-256 암호화 후 저장 (Redis 개인정보 원문 저장 금지)
        MockPassSession session = MockPassSession.builder()
                .identityVerificationId(identityVerificationId)
                .status(MockPassStatus.VERIFIED.name())
                .encryptedName(PersonalDataCipher.encrypt(name))
                .encryptedPhone(PersonalDataCipher.encrypt(phoneNumber))
                .encryptedCi(PersonalDataCipher.encrypt(ci))
                .used(false)
                .build();
        mockPassStore.save(identityVerificationId, session);

        // 5. 개인정보는 응답에 내려주지 않는다 — identityVerificationId/status 만 반환
        //    (name/phoneNumber/CI 는 Redis 세션에만 암호화 보관)
        return MockPassStatusResponseDTO.of(identityVerificationId, MockPassStatus.VERIFIED.name());
    }

    /**
     * SHA-256 hex 변환 — 동일 휴대폰 → 동일 CI 를 보장하는 결정적 해시
     * - 개인정보(휴대폰) 원문이 CI 에 직접 노출되지 않도록 hash 로만 구성한다
     * - AuthServiceImpl.sha256Hex 와 동일한 hex 인코딩 (지식 규칙: 검색용 개인정보는 SHA-256 hash)
     */
    private static String sha256Hex(String value) {
        byte[] digest = sha256Bytes(value);
        StringBuilder sb = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }

    private static byte[] sha256Bytes(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
