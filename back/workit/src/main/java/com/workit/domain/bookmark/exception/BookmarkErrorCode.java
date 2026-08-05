package com.workit.domain.bookmark.exception;

import com.workit.exception.ErrorCode;
import org.springframework.http.HttpStatus;

// 북마크 도메인 에러 코드
// 규격: BOOKMARK_ + 사유
public enum BookmarkErrorCode implements ErrorCode {

    INVALID_BOOKMARK_ID(HttpStatus.BAD_REQUEST, "올바르지 않은 북마크 ID입니다."),
    INVALID_BOOKMARK_SEARCH_CONDITION(HttpStatus.BAD_REQUEST, "북마크 조회 조건이 올바르지 않습니다."),
    INVALID_BOOKMARK_CURSOR(HttpStatus.BAD_REQUEST, "북마크 커서가 올바르지 않습니다."),
    MERCHANT_NOT_FOUND(HttpStatus.NOT_FOUND, "가맹점이 존재하지 않습니다."),
    BOOKMARK_NOT_FOUND(HttpStatus.NOT_FOUND, "북마크를 찾을 수 없습니다."),
    BOOKMARK_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 북마크한 가맹점입니다."),
    BOOKMARK_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 북마크에 접근할 수 없습니다.");

    private final HttpStatus status;
    private final String message;

    BookmarkErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    @Override
    public HttpStatus getStatus() {
        return status;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
