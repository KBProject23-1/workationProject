package com.workit.domain.auth.util;

// 이메일 마스킹 유틸 (docs 아이디 찾기 응답 예시: user****@example.com)
//
// 규칙:
// - 로컬파트('@' 앞) 앞 4자리만 유지하고 나머지는 '*' 로 대체
//   예: user1234@example.com → user****@example.com
// - 로컬파트가 4자 이하이면 마지막 2자리를 '*' 로 대체 (짧은 로컬파트도 최소 1자 이상 마스킹)
//   예: user@example.com → us**@example.com
// - 도메인('@' 뒤)은 마스킹하지 않는다
// - '@' 가 없거나 로컬파트가 비어 있는 비정상 입력은 원문 그대로 반환 (마스킹 실패보다 안전)
public class EmailMasker {

    /** 유지할 로컬파트 최대 길이 */
    private static final int KEEP_MAX = 4;

    /** 로컬파트가 짧을 때 뒤에서 마스킹할 자리 수 */
    private static final int MASK_TAIL = 2;

    private EmailMasker() {
        // 유틸 클래스, 인스턴스화 방지
    }

    public static String mask(String email) {
        if (email == null) {
            return null;
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            // 정상 가입 데이터에는 발생하지 않는 비정상 형식 — 원문 그대로 반환
            return email;
        }
        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex);

        int keep = Math.min(KEEP_MAX, Math.max(1, localPart.length() - MASK_TAIL));

        StringBuilder maskedBuilder = new StringBuilder(localPart.length());
        for (int i = 0; i < localPart.length(); i++) {
            maskedBuilder.append(i < keep ? localPart.charAt(i) : '*');
        }
        return maskedBuilder.append(domain).toString();
    }
}
