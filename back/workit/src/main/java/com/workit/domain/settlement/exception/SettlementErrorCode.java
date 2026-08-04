package com.workit.domain.settlement.exception;

import com.workit.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

// 정산 도메인 에러 코드
@Getter
@RequiredArgsConstructor
public enum SettlementErrorCode implements ErrorCode {

    /* 404 */
    NO_EXPENSE_TO_SETTLE (HttpStatus.NOT_FOUND, "정산할 지출 내역이 없습니다."),
    NO_EXPENSE_TO_EXPORT (HttpStatus.NOT_FOUND, "출력할 지출 내역이 없습니다."),

    /* 500 */
    EXCEL_EXPORT_FAILED  (HttpStatus.INTERNAL_SERVER_ERROR, "엑셀 파일 생성에 실패했습니다."),
    PDF_EXPORT_FAILED    (HttpStatus.INTERNAL_SERVER_ERROR, "PDF 파일 생성에 실패했습니다."),
    FILE_WRITE_FAILED    (HttpStatus.INTERNAL_SERVER_ERROR, "파일 전송에 실패했습니다.");

    private final HttpStatus status;
    private final String message;
}
