package com.workit.domain.tourism.exception;

import com.workit.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum TourismErrorCode implements ErrorCode {
    API_KEY_NOT_CONFIGURED(HttpStatus.SERVICE_UNAVAILABLE, "TourAPI 서비스 키가 설정되지 않았습니다."),
    SYNC_IN_PROGRESS(HttpStatus.CONFLICT, "관광 데이터 동기화가 이미 실행 중입니다."),
    TOUR_API_ERROR(HttpStatus.BAD_GATEWAY, "한국관광공사 TourAPI 호출에 실패했습니다.");

    private final HttpStatus status;
    private final String message;
}
