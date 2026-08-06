package com.workit.domain.recommendation.activities.controller;

import com.workit.domain.recommendation.activities.dto.request.ActivityRecommendationCreateRequestDTO;
import com.workit.domain.recommendation.activities.dto.response.ActivityRecommendationResponseDTO;
import com.workit.domain.recommendation.activities.service.ActivityRecommendationService;
import com.workit.domain.recommendation.dto.request.RecommendationRecalculateRequestDTO;
import com.workit.global.dto.CommonResponse;
import com.workit.global.response.GlobalResponseFactory;
import com.workit.security.CurrentUser;
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
            @CurrentUser Long userId,
            @RequestBody ActivityRecommendationCreateRequestDTO request) {
        return GlobalResponseFactory.created(activityRecommendationService.addActivityRecommendation(userId, request));
    }

    @GetMapping
    public ResponseEntity<CommonResponse<ActivityRecommendationResponseDTO>> activityRecommendationList(
            @CurrentUser Long userId,
            @RequestParam(value = "referenceMerchantId", required = false) Long referenceMerchantId,
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return GlobalResponseFactory.success(activityRecommendationService.findActivityRecommendation(
                userId, referenceMerchantId, cursor, size));
    }

    @PostMapping("/{recommendationRequestId}/recalculate")
    public ResponseEntity<CommonResponse<ActivityRecommendationResponseDTO>> activityRecommendationRecalculate(
            @CurrentUser Long userId,
            @PathVariable("recommendationRequestId") Long recommendationRequestId,
            @RequestBody RecommendationRecalculateRequestDTO request) {
        return GlobalResponseFactory.created(activityRecommendationService.recalculateActivityRecommendation(
                userId, recommendationRequestId, request));
    }
}
