package com.workit.domain.recommendation.accommodation.exception;

import com.workit.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AccommodationRecommendationErrorCode implements ErrorCode {
    INVALID_RECOMMENDATION_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 추천 요청입니다."),
    WORKATION_NOT_FOUND(HttpStatus.NOT_FOUND, "진행 중인 워케이션을 찾을 수 없습니다."),
    ACCOMMODATION_CANDIDATE_NOT_FOUND(HttpStatus.NOT_FOUND, "추천 가능한 숙소가 없습니다."),
    REFERENCE_MERCHANT_NOT_FOUND(HttpStatus.NOT_FOUND, "추천 기준 공유오피스를 찾을 수 없습니다."),
    RECOMMENDATION_CONDITION_NOT_READY(HttpStatus.CONFLICT, "추천에 필요한 설문 또는 예산 정보가 부족합니다.");

    private final HttpStatus status;
    private final String message;
}
