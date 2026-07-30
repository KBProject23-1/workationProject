package com.workit.domain.category.exception;

import com.workit.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

// 카테고리 도메인 에러 코드
// 새로운 에러가 필요하면 예외 클래스를 만들지 말고 여기에 한 줄 추가함
@Getter
@RequiredArgsConstructor
public enum CategoryErrorCode implements ErrorCode {

    /* 400 */
    CATEGORY_NAME_TOO_LONG(HttpStatus.BAD_REQUEST, "카테고리 이름은 10자를 넘을 수 없습니다."),

    /* 404 */
    CATEGORY_NOT_FOUND    (HttpStatus.NOT_FOUND,   "존재하지 않는 카테고리입니다.");

    private final HttpStatus status;
    private final String message;
}
