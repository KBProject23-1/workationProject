package com.workit.global.common.dto;

import com.workit.exception.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

// Controller에서 ResponseEntity 를 직접 조립하지 않도록 공통 응답을 생성하는 팩토리
//
// 사용 예:
//   return GlobalResponseFactory.success(userService.getMyProfile(userId));
//   return GlobalResponseFactory.success(createdId, "프로필이 저장되었습니다.");
//   return GlobalResponseFactory.error(HttpStatus.UNAUTHORIZED, AuthErrorCode.INVALID_CREDENTIALS);
public final class GlobalResponseFactory {

    private GlobalResponseFactory() {
    }

    /** 200 OK - 기본 메시지("요청 성공") */
    public static <T> ResponseEntity<CommonResponse<T>> success(T data) {
        return ResponseEntity.ok(CommonResponse.success(data));
    }

    /** 200 OK - 커스텀 메시지 */
    public static <T> ResponseEntity<CommonResponse<T>> success(T data, String message) {
        return ResponseEntity.ok(CommonResponse.success(data, message));
    }

    /** 실패 - ErrorCode 기반 (상태 코드, 에러 코드명, 기본 메시지 사용) */
    public static <T> ResponseEntity<CommonResponse<T>> error(HttpStatus status, ErrorCode errorCode) {
        return ResponseEntity.status(status)
                .body(CommonResponse.error(errorCode.getErrorCode(), errorCode.getMessage()));
    }

    /** 실패 - ErrorCode 기반이지만 상황별 메시지를 직접 지정할 때 */
    public static <T> ResponseEntity<CommonResponse<T>> error(HttpStatus status, ErrorCode errorCode, String message) {
        return ResponseEntity.status(status)
                .body(CommonResponse.error(errorCode.getErrorCode(), message));
    }

    /** 실패 - 상황별 메시지를 직접 지정할 때 (예: 예외 인스턴스의 상세 메시지 전달) */
    public static <T> ResponseEntity<CommonResponse<T>> error(HttpStatus status, String errorCode, String message) {
        return ResponseEntity.status(status)
                .body(CommonResponse.error(errorCode, message));
    }
}
