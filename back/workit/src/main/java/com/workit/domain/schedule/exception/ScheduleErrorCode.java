package com.workit.domain.schedule.exception;

import com.workit.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

// 일정 도메인 에러 코드
// 새로운 에러가 필요하면 예외 클래스를 만들지 말고 여기에 한 줄 추가함
@Getter
@RequiredArgsConstructor
public enum ScheduleErrorCode implements ErrorCode {

    /* 400 */
    SCHEDULED_AT_REQUIRED     (HttpStatus.BAD_REQUEST, "방문 일시를 입력해 주세요."),
    SCHEDULED_AT_OUT_OF_PERIOD(HttpStatus.BAD_REQUEST, "워케이션 기간 안의 날짜여야 합니다."),
    MERCHANT_CATEGORY_INVALID (HttpStatus.BAD_REQUEST, "음식점과 여가 활동만 일정으로 등록할 수 있습니다."),
    DAYS_OUT_OF_RANGE         (HttpStatus.BAD_REQUEST, "조회 기간은 1일에서 7일까지입니다."),

    /* 404 */
    MERCHANT_NOT_FOUND        (HttpStatus.NOT_FOUND,   "가맹점을 찾을 수 없습니다."),
    SCHEDULE_NOT_FOUND        (HttpStatus.NOT_FOUND,   "일정을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String message;
}
