package com.workit.domain.workation.exception;

import com.workit.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;


// 워케이션 도메인 에러 코드
// 새로운 에러가 필요하면 예외 클래스를 만들지 말고 여기에 한 줄 추가함
@Getter
@RequiredArgsConstructor
public enum WorkationErrorCode implements ErrorCode {

    /* 400 */
    REGION_NOT_FOUND   (HttpStatus.BAD_REQUEST, "존재하지 않는 지역입니다."),
    EXPENSE_OUT_OF_PERIOD (HttpStatus.BAD_REQUEST, "이미 등록된 지출이 변경한 기간을 벗어납니다."),

    /* 403 */
    ACCESS_DENIED      (HttpStatus.FORBIDDEN,   "본인의 워케이션만 접근할 수 있습니다."),

    /* 404 */
    WORKATION_NOT_FOUND(HttpStatus.NOT_FOUND,   "워케이션을 찾을 수 없습니다."),

    /* 409 */
    ALREADY_ACTIVE     (HttpStatus.CONFLICT,    "이미 진행 중인 워케이션이 있습니다. 정산 완료 후 등록할 수 있습니다."),
    ALREADY_SETTLED    (HttpStatus.CONFLICT,    "이미 정산이 완료된 워케이션입니다."),
    // 아직 이용하지 않은 예약은 환불이 걸려 있어 사용자가 직접 취소해야 한다.
    // 이미 이용했거나 취소된 예약은 이력으로 남기고 연결만 끊으므로 삭제를 막지 않는다.
    RESERVATION_EXISTS (HttpStatus.CONFLICT,    "예약이 남아 있어 삭제할 수 없습니다. 예약을 먼저 취소해 주세요.");

    private final HttpStatus status;
    private final String message;
}
