package com.workit.domain.recommendation.restaurant.exception;

import com.workit.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum RestaurantRecommendationErrorCode implements ErrorCode {
    INVALID_RECOMMENDATION_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 추천 요청입니다."),
    INVALID_MEAL_TYPE(HttpStatus.BAD_REQUEST, "올바른 식사 유형을 선택해 주세요."),
    WORKATION_NOT_FOUND(HttpStatus.NOT_FOUND, "진행 중인 워케이션을 찾을 수 없습니다."),
    RESTAURANT_CANDIDATE_NOT_FOUND(HttpStatus.NOT_FOUND, "추천 가능한 음식점이 없습니다."),
    REFERENCE_MERCHANT_NOT_FOUND(HttpStatus.NOT_FOUND, "추천 기준 장소를 찾을 수 없습니다."),
    RECOMMENDATION_CONDITION_NOT_READY(HttpStatus.CONFLICT, "추천에 필요한 설문 또는 예산 정보가 부족합니다."),
    BREAKFAST_RECOMMENDATION_DISABLED(HttpStatus.CONFLICT, "아침 제외형 설문에서는 아침 추천을 이용할 수 없습니다.");

    private final HttpStatus status;
    private final String message;
}
