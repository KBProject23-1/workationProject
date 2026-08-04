package com.workit.domain.auth.service;

import com.workit.domain.auth.dto.response.IdentityVerificationResponseDTO;
import com.workit.domain.auth.dto.response.TermsListResponseDTO;
import com.workit.domain.auth.dto.response.TermsResponseDTO;
import com.workit.domain.auth.exception.AuthErrorCode;
import com.workit.domain.auth.mapper.AuthMapper;
import com.workit.domain.auth.provider.IdentityVerificationProvider;
import com.workit.domain.auth.provider.IdentityVerificationResult;
import com.workit.domain.auth.util.SignupTokenProvider;
import com.workit.exception.BusinessException;
import com.workit.global.util.PersonalDataCipher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthMapper authMapper;
    private final IdentityVerificationProvider identityVerificationProvider;
    private final SignupTokenProvider signupTokenProvider;
    private final SignupVerificationStore signupVerificationStore;

    @Override
    @Transactional(readOnly = true)
    public TermsListResponseDTO getTermsList() {

        List<TermsResponseDTO> terms = authMapper.selectTermsList()
                .stream()
                .map(TermsResponseDTO::from)
                .collect(Collectors.toList());

        return TermsListResponseDTO.of(terms);
    }

    @Override
    // DB 는 SELECT 만 수행하지만 Redis 임시 데이터 저장(side-effect)이 있으므로
    // readOnly=true 로 오인되지 않도록 일반 @Transactional 을 사용한다
    @Transactional
    public IdentityVerificationResponseDTO verifyIdentity(String identityVerificationId) {

        // 0. 요청 값 검증 (javax.validation 미사용 환경 → Service Layer 에서 수행)
        //    null/빈 값은 Provider 에서도 검증하지만, 가입 가능 여부 판단 전에 명시적으로 처리한다
        if (identityVerificationId == null || identityVerificationId.trim().isEmpty()) {
            throw new BusinessException(AuthErrorCode.INVALID_VERIFICATION_ID);
        }

        // 1. Provider 로 PASS 인증 결과 검증 (현재 Mock — 실패 시 BusinessException)
        IdentityVerificationResult result = identityVerificationProvider.verify(identityVerificationId);

        // 2. CI 중복 가입 검증
        //    - CI 원문을 그대로 비교하지 않고 SHA-256 해시로 변환해 조회 (knowledge.md: 검색용 hash 저장)
        //    - 이미 가입된 회원이면 회원가입 진행 불가 (docs: 409 DUPLICATE_USER)
        String ciHash = sha256Hex(result.getCi());
        if (authMapper.countByCiHash(ciHash) > 0) {
            throw new BusinessException(AuthErrorCode.DUPLICATE_USER);
        }

        // 3. 회원가입 임시 데이터 생성 + Redis 임시 저장
        //    - JWT Payload 에 개인정보를 담지 않는 대신, 회원가입 완료 시 복원할 데이터를
        //      signup:verification:{temporaryUserKey} 키로 짧은 TTL 동안 보관한다
        //    - CI/name 은 원문 대신 AES-256 암호화본만 저장 (knowledge.md: Redis 회원 정보 원문 저장 금지)
        String temporaryUserKey = UUID.randomUUID().toString();
        SignupVerificationData verificationData = SignupVerificationData.builder()
                .verificationId(identityVerificationId)
                .ciHash(ciHash)
                .encryptedCi(PersonalDataCipher.encrypt(result.getCi()))
                .encryptedName(PersonalDataCipher.encrypt(result.getName()))
                .build();
        signupVerificationStore.save(temporaryUserKey, verificationData);

        // 4. 회원가입 전용 임시 JWT 발급 (Payload: sub, temporaryUserKey, iat, exp — 개인정보 없음)
        String identityToken = signupTokenProvider.issue(temporaryUserKey);

        // 5. 응답 생성 — API Contract 유지 (identityToken, name)
        return IdentityVerificationResponseDTO.of(identityToken, result.getName());
    }

    /**
     * 검색용 SHA-256 해시 (소문자 hex)
     * - knowledge.md: 검색 필요한 개인정보는 원본 AES 암호화 + 검색용 SHA-256 hash 별도 저장
     * - spring-core DigestUtils 에 sha256DigestAsHex 가 없는 버전이므로 JDK 표준 MessageDigest 사용
     * - CI 는 평문 로그 출력 금지 — 해시 입력값 로그 금지
     */
    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }
}
