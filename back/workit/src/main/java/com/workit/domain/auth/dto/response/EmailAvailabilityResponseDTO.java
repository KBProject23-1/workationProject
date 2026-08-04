package com.workit.domain.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

// 회원가입 이메일 중복 확인 응답
// API 스펙(docs): GET /api/v1/auth/signup/check-email → data.available
// - available: true  = 사용 가능한 이메일 (중복 없음)
// - available: false = 이미 사용 중인 이메일 (중복)
// 중복 여부가 API 오류(4xx)가 아닌 200 SUCCESS 로 반환되는 이유:
//   가입 화면에서 실시간 중복 체크가 정상적으로 수행된 결과이므로 성공 응답으로 내려준다.
@Getter
@Builder
public class EmailAvailabilityResponseDTO {

    private boolean available;

    public static EmailAvailabilityResponseDTO of(boolean available) {
        return EmailAvailabilityResponseDTO.builder()
                .available(available)
                .build();
    }
}
