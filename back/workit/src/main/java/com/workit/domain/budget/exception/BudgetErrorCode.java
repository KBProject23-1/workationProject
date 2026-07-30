package com.workit.domain.budget.exception;

import com.workit.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

// 예산 도메인 에러 코드
// 새로운 에러가 필요하면 예외 클래스를 만들지 말고 여기에 한 줄 추가함
@Getter
@RequiredArgsConstructor
public enum BudgetErrorCode implements ErrorCode {

    /* 400 */
    BUDGET_ITEMS_REQUIRED     (HttpStatus.BAD_REQUEST, "배정할 카테고리를 하나 이상 선택해 주세요."),
    BUDGET_SUM_MISMATCH       (HttpStatus.BAD_REQUEST, "카테고리 배정 합계가 총예산과 일치하지 않습니다."),
    BUDGET_ETC_REQUIRED       (HttpStatus.BAD_REQUEST, "기타 카테고리는 반드시 포함되어야 합니다."),
    BUDGET_TYPE_MISMATCH      (HttpStatus.BAD_REQUEST, "해당 예산 유형에 사용할 수 없는 카테고리입니다."),
    BUDGET_AMOUNT_NEGATIVE    (HttpStatus.BAD_REQUEST, "배정 금액은 0원 이상이어야 합니다."),
    BUDGET_CATEGORY_DUPLICATED(HttpStatus.BAD_REQUEST, "같은 카테고리를 두 번 배정할 수 없습니다."),
    BUDGET_CATEGORY_IN_USE    (HttpStatus.BAD_REQUEST, "이미 지출이 등록된 카테고리를 목록에서 제외할 수 없습니다."),

    /* 403 */
    BUDGET_ETC_DELETE_DENIED  (HttpStatus.FORBIDDEN,   "기타 카테고리는 삭제할 수 없습니다."),

    /* 404 */
    BUDGET_NOT_FOUND          (HttpStatus.NOT_FOUND,   "예산 배정 정보를 찾을 수 없습니다."),
    BUDGET_NOT_SET            (HttpStatus.NOT_FOUND,   "설정된 예산이 없습니다. 등록 API를 사용해 주세요."),

    /* 409 */
    BUDGET_ALREADY_EXISTS     (HttpStatus.CONFLICT,    "이미 예산이 설정되어 있습니다. 수정 API를 사용해 주세요."),
    BUDGET_ITEM_ALREADY_EXISTS(HttpStatus.CONFLICT,    "이미 예산에 추가된 카테고리입니다."),
    BUDGET_ITEM_HAS_EXPENSE   (HttpStatus.CONFLICT,    "이 카테고리로 등록된 지출이 있습니다.");

    private final HttpStatus status;
    private final String message;
}
