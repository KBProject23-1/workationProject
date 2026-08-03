package com.workit.global.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

// 모든 API 공통 응답 포맷
//
// 성공: { "status": "SUCCESS", "message": "요청 성공", "data": { ... } }
// 실패: { "status": "ERROR",   "errorCode": "INVALID_CREDENTIALS", "message": "..." }
//
// 사용하지 않는 필드는 JSON에서 제외한다 (NON_NULL)
// - 성공 응답에는 errorCode 가 나오지 않는다
// - 실패 응답에는 data 가 나오지 않는다
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommonResponse<T> {

    /** SUCCESS | ERROR */
    private final String status;

    /** 성공/실패 공통 메시지 */
    private final String message;

    /** 성공 시 응답 데이터 */
    private final T data;

    /** 실패 시 에러 코드명 (예: INVALID_CREDENTIALS) */
    private final String errorCode;

    private CommonResponse(String status, String message, T data, String errorCode) {
        this.status = status;
        this.message = message;
        this.data = data;
        this.errorCode = errorCode;
    }

    /** 기본 메시지("요청 성공")로 성공 응답 생성 */
    public static <T> CommonResponse<T> success(T data) {
        return new CommonResponse<>("SUCCESS", "요청 성공", data, null);
    }

    /** 커스텀 메시지로 성공 응답 생성 */
    public static <T> CommonResponse<T> success(T data, String message) {
        return new CommonResponse<>("SUCCESS", message, data, null);
    }

    /** 에러 응답 생성 */
    public static <T> CommonResponse<T> error(String errorCode, String message) {
        return new CommonResponse<>("ERROR", message, null, errorCode);
    }
}
