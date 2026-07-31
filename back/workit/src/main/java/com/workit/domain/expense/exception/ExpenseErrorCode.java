package com.workit.domain.expense.exception;

import com.workit.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

// 지출 도메인 에러 코드
// 새로운 에러가 필요하면 예외 클래스를 만들지 말고 여기에 한 줄 추가함
@Getter
@RequiredArgsConstructor
public enum ExpenseErrorCode implements ErrorCode {

    /* 400 */
    SPENT_DATE_OUT_OF_PERIOD (HttpStatus.BAD_REQUEST, "지출 일시가 워케이션 기간을 벗어났습니다."),
    CATEGORY_TYPE_MISMATCH   (HttpStatus.BAD_REQUEST, "선택한 카테고리가 해당 예산 유형에 없습니다."),
    CARD_REQUIRED            (HttpStatus.BAD_REQUEST, "법인 지출은 사용한 법인카드를 선택해야 합니다."),
    CARD_TYPE_MISMATCH       (HttpStatus.BAD_REQUEST, "선택한 카드가 법인카드가 아닙니다."),
    AMOUNT_INVALID           (HttpStatus.BAD_REQUEST, "지출 금액은 0원보다 커야 합니다."),

    /* 403 */
    APP_PAYMENT_NOT_EDITABLE (HttpStatus.FORBIDDEN, "앱 내 결제 건은 금액과 일시를 수정할 수 없습니다."),
    APP_PAYMENT_NOT_DELETABLE(HttpStatus.FORBIDDEN, "앱 내 결제 건은 삭제할 수 없습니다."),

    /* 404 */
    EXPENSE_NOT_FOUND        (HttpStatus.NOT_FOUND, "지출 내역을 찾을 수 없습니다."),
    CARD_NOT_FOUND           (HttpStatus.NOT_FOUND, "카드를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String message;
}
