package com.workit.domain.review.exception;

import com.workit.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReviewErrorCode implements ErrorCode {

    MERCHANT_NOT_FOUND(HttpStatus.NOT_FOUND, "가맹점을 찾을 수 없습니다."),
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "리뷰를 찾을 수 없습니다."),
    RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "예약을 찾을 수 없습니다."),
    TRANSACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "결제 내역을 찾을 수 없습니다."),
    DUPLICATE_REVIEW(HttpStatus.CONFLICT, "이미 작성했거나 삭제된 내역입니다."),
    INVALID_RATING(HttpStatus.BAD_REQUEST, "별점은 1점부터 5점까지 1점 단위로 입력해야 합니다."),
    UNSUPPORTED_RESERVATION_CATEGORY(HttpStatus.BAD_REQUEST, "숙소 또는 공유 오피스 예약만 리뷰를 작성할 수 있습니다."),
    UNSUPPORTED_TRANSACTION_CATEGORY(HttpStatus.BAD_REQUEST, "식당 또는 여가 결제만 리뷰를 작성할 수 있습니다."),
    CANCELED_RESERVATION(HttpStatus.BAD_REQUEST, "취소된 예약에는 리뷰를 작성할 수 없습니다."),
    PAYMENT_NOT_COMPLETED(HttpStatus.BAD_REQUEST, "결제가 완료된 내역에만 리뷰를 작성할 수 있습니다."),
    REVIEW_NOT_AVAILABLE_YET(HttpStatus.BAD_REQUEST, "아직 리뷰를 작성할 수 있는 시점이 아닙니다."),
    REVIEW_PERIOD_EXPIRED(HttpStatus.BAD_REQUEST, "리뷰 작성 또는 수정 가능 기간이 지났습니다."),
    ATMOSPHERE_REQUIRED(HttpStatus.BAD_REQUEST, "공유 오피스 리뷰는 분위기 태그를 반드시 선택해야 합니다."),
    ATMOSPHERE_ONLY_OFFICE(HttpStatus.BAD_REQUEST, "분위기 태그는 공유 오피스 리뷰에만 사용할 수 있습니다."),
    UPDATE_FIELD_REQUIRED(HttpStatus.BAD_REQUEST, "수정할 리뷰 항목이 필요합니다."),
    REVIEW_NOT_ACTIVE(HttpStatus.BAD_REQUEST, "삭제된 리뷰는 수정하거나 다시 삭제할 수 없습니다.");

    private final HttpStatus status;
    private final String message;
}
