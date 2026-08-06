package com.workit.domain.auth.provider;

import com.workit.domain.auth.exception.AuthErrorCode;
import com.workit.exception.BusinessException;
import org.springframework.stereotype.Component;

// PASS 본인인증 검증 Mock 구현체
//
// - 실제 PortOne 연동 이전까지 auth 플로우 개발/테스트를 가능하게 하는 임시 구현
// - 실연동 구현(PortOneIdentityVerificationProvider)으로 교체 시 Service 코드는 수정하지 않는다
// - 인증 실패 케이스: null/빈 값 또는 INVALID_IDENTIFIER → BusinessException(INVALID_VERIFICATION_ID)
@Component
public class MockIdentityVerificationProvider implements IdentityVerificationProvider {

    /** Mock 전용 고정 이름 — 화면 표시용 (실제 연동 전 임시 값) */
    private static final String MOCK_NAME = "홍길동";

    /** Mock CI 접두사 — 실제 CI 가 아니며 개인정보가 아님 */
    private static final String MOCK_CI_PREFIX = "MOCK-CI-";

    /** Mock 전화번호 접두사 — 실제 전화번호가 아니며 개인정보가 아님 */
    private static final String MOCK_PHONE_PREFIX = "010";

    /** 실패 시나리오 테스트용 식별자 — 이 값을 넘기면 인증 실패로 처리된다 */
    public static final String INVALID_IDENTIFIER = "invalid";

    @Override
    public IdentityVerificationResult verify(String identityVerificationId) {
        String id = identityVerificationId == null ? "" : identityVerificationId.trim();

        if (id.isEmpty() || INVALID_IDENTIFIER.equalsIgnoreCase(id)) {
            throw new BusinessException(AuthErrorCode.INVALID_VERIFICATION_ID);
        }

        // 실제 연동 전까지는 입력값 기반의 고정 Mock 결과를 반환한다.
        // phone 은 CI 처럼 입력 id 로부터 유도한다 — users.phone_number_hash(UNIQUE) 충돌 방지용
        // (실제 PASS 연동 시 본인인증 결과의 실제 휴대폰 번호가 반환된다)
        return IdentityVerificationResult.builder()
                .ci(MOCK_CI_PREFIX + id)
                .name(MOCK_NAME)
                .phoneNumber(mockPhone(id))
                .build();
    }

    /** 입력 id 에서 숫자만 추출해 010-XXXXXXXX 형식(11자리)의 Mock 전화번호를 만든다 */
    private static String mockPhone(String id) {
        StringBuilder digits = new StringBuilder();
        for (char c : id.toCharArray()) {
            if (Character.isDigit(c)) {
                digits.append(c);
            }
        }
        String tail = digits.length() >= 8
                ? digits.substring(digits.length() - 8)
                : padTo8(digits.toString());
        return MOCK_PHONE_PREFIX + tail;
    }

    private static String padTo8(String digits) {
        StringBuilder sb = new StringBuilder(digits);
        while (sb.length() < 8) {
            sb.insert(0, "0");
        }
        return sb.toString();
    }
}
