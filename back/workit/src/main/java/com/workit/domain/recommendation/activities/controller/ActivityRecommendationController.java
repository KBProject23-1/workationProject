package com.workit.domain.recommendation.activities.controller;

import com.workit.domain.recommendation.activities.dto.request.ActivityRecommendationCreateRequestDTO;
import com.workit.domain.recommendation.activities.dto.response.ActivityRecommendationResponseDTO;
import com.workit.domain.recommendation.activities.service.ActivityRecommendationService;
import com.workit.domain.recommendation.dto.request.RecommendationRecalculateRequestDTO;
import com.workit.global.dto.CommonResponse;
import com.workit.global.response.GlobalResponseFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/recommendations/activities")
@RequiredArgsConstructor
public class ActivityRecommendationController {

    private final ActivityRecommendationService activityRecommendationService;

    @PostMapping
    public ResponseEntity<CommonResponse<ActivityRecommendationResponseDTO>> activityRecommendationAdd(
            @RequestBody ActivityRecommendationCreateRequestDTO request) {
        // 인증 기능이 연결되면 JWT에서 사용자 ID를 가져오도록 교체
        Long userId = 9001L;
        return GlobalResponseFactory.created(activityRecommendationService.addActivityRecommendation(userId, request));
    }

    @GetMapping
    public ResponseEntity<CommonResponse<ActivityRecommendationResponseDTO>> activityRecommendationList(
            @RequestParam(value = "referenceMerchantId", required = false) Long referenceMerchantId,
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        // 인증 기능이 연결되면 JWT에서 사용자 ID를 가져오도록 교체
        Long userId = 9001L;
        return GlobalResponseFactory.success(activityRecommendationService.findActivityRecommendation(
                userId, referenceMerchantId, cursor, size));
    }

    @PostMapping("/{recommendationRequestId}/recalculate")
    public ResponseEntity<CommonResponse<ActivityRecommendationResponseDTO>> activityRecommendationRecalculate(
            @PathVariable("recommendationRequestId") Long recommendationRequestId,
            @RequestBody RecommendationRecalculateRequestDTO request) {
        Long userId = 9001L;
        return GlobalResponseFactory.created(activityRecommendationService.recalculateActivityRecommendation(
                userId, recommendationRequestId, request));
    }
}
