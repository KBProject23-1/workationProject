package com.workit.domain.auth.service;

import com.workit.domain.auth.MockPassStatus;
import com.workit.domain.auth.dto.request.MockPassCompleteRequestDTO;
import com.workit.domain.auth.dto.response.MockPassStatusResponseDTO;
import com.workit.domain.auth.exception.AuthErrorCode;
import com.workit.exception.BusinessException;
import com.workit.global.util.PersonalDataCipher;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

// Mock PASS 본인인증 서비스 구현
//
// - 프론트가 생성한 identityVerificationId 를 VERIFIED 세션으로 등록한다.
//   (Mock 인증 완료 자체는 팝업 UX 로 프론트가 흉내 내지만, 백엔드 등록을 거치지 않으면
//    verify-identity 가 거부하므로 성공 여부는 여전히 백엔드가 관리한다)
// - 개인정보(name/휴대폰)는 Service Layer 에서만 AES-256 암호화/복호화 한다
//   (knowledge.md: Controller/Mapper 에서 암호화 금지, Redis 개인정보 원문 저장 금지)
// - Redis 세션 저장은 DB 트랜잭션과 무관한 side-effect 이므로 별도 @Transactional 을 사용하지 않는다
@Service
public class MockPassServiceImpl implements MockPassService {

    /** Mock 인증 고유 번호 접두어 — 프론트가 생성한 ID 도 이 형식을 따라야 한다 */
    private static final String ID_PREFIX = "mock-";

    /** identityVerificationId 최대 길이 — DB 컬럼/Redis 키 과다 방지 */
    private static final int ID_MAX_LENGTH = 100;

    /** 휴대폰 번호 형식 — 숫자 10~11자리 (예: 01012345678) */
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\d{10,11}$");

    private final MockPassStore mockPassStore;

    public MockPassServiceImpl(MockPassStore mockPassStore) {
        this.mockPassStore = mockPassStore;
    }

    @Override
    public MockPassStatusResponseDTO complete(MockPassCompleteRequestDTO request) {

        // 1. 필수 값 검증 (javax.validation 미사용 환경 → Service Layer 에서 수행)
        if (request == null || isBlank(request.getIdentityVerificationId())) {
            throw new BusinessException(AuthErrorCode.INVALID_VERIFICATION_ID);
        }
        if (isBlank(request.getName()) || isBlank(request.getPhoneNumber())) {
            throw new BusinessException(AuthErrorCode.INVALID_PASS_REQUEST);
        }

        // 2. 형식 검증 — Mock 인증 ID 형식(mock- 접두어/최대 길이) + 휴대폰 숫자 형식
        String identityVerificationId = request.getIdentityVerificationId().trim();
        if (identityVerificationId.length() > ID_MAX_LENGTH
                || !identityVerificationId.startsWith(ID_PREFIX)) {
            throw new BusinessException(AuthErrorCode.INVALID_PASS_REQUEST);
        }
        if (!PHONE_PATTERN.matcher(request.getPhoneNumber().trim()).matches()) {
            throw new BusinessException(AuthErrorCode.INVALID_PASS_REQUEST);
        }

        // 3. VERIFIED 세션 등록 — 개인정보 AES-256 암호화 후 저장 (Redis 개인정보 원문 저장 금지)
        String name = request.getName().trim();
        MockPassSession session = MockPassSession.builder()
                .identityVerificationId(identityVerificationId)
                .status(MockPassStatus.VERIFIED.name())
                .encryptedName(PersonalDataCipher.encrypt(name))
                .encryptedPhone(PersonalDataCipher.encrypt(request.getPhoneNumber().trim()))
                .build();
        mockPassStore.save(identityVerificationId, session);

        return MockPassStatusResponseDTO.of(identityVerificationId,
                MockPassStatus.VERIFIED.name(), name);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
