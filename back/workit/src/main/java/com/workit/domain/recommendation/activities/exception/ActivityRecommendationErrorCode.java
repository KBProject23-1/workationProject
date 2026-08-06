package com.workit.domain.recommendation.activities.exception;

import com.workit.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ActivityRecommendationErrorCode implements ErrorCode {
    ACTIVITY_CANDIDATE_NOT_FOUND(HttpStatus.NOT_FOUND, "추천 가능한 여가 장소가 없습니다."),
    ACTIVITY_REFERENCE_MERCHANT_NOT_FOUND(HttpStatus.NOT_FOUND, "추천 기준 숙소를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String message;
}
