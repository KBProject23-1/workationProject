package com.workit.domain.recommendation.restaurant.controller;

import com.workit.domain.recommendation.restaurant.dto.request.RecommendationRecalculateRequestDTO;
import com.workit.domain.recommendation.restaurant.dto.request.RestaurantRecommendationCreateRequestDTO;
import com.workit.domain.recommendation.restaurant.dto.response.RecommendationCandidateListResponseDTO;
import com.workit.domain.recommendation.restaurant.dto.response.RestaurantReferenceResponseDTO;
import com.workit.domain.recommendation.common.dto.RecommendationListResponseDTO;
import com.workit.domain.recommendation.restaurant.dto.response.RestaurantRecommendationResponseDTO;
import com.workit.domain.recommendation.restaurant.service.RestaurantRecommendationService;
import com.workit.domain.recommendation.restaurant.vo.MealType;
import com.workit.global.dto.CommonResponse;
import com.workit.global.response.GlobalResponseFactory;
import com.workit.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/recommendations/restaurants")
@RequiredArgsConstructor
public class RestaurantRecommendationController {

    private final RestaurantRecommendationService restaurantRecommendationService;

    @GetMapping("/reference-place")
    public ResponseEntity<CommonResponse<RestaurantReferenceResponseDTO>> getRestaurantReferencePlace(
            @CurrentUser Long userId,
            @RequestParam("mealType") MealType mealType) {
        return GlobalResponseFactory.success(restaurantRecommendationService.findRestaurantReferencePlace(userId, mealType));
    }

    @GetMapping("/reference-place-candidates")
    public ResponseEntity<CommonResponse<RecommendationCandidateListResponseDTO>> getRestaurantReferencePlaceCandidates(
            @CurrentUser Long userId) {
        return GlobalResponseFactory.success(restaurantRecommendationService.findRestaurantReferencePlaceCandidates(userId));
    }

    @PostMapping
    public ResponseEntity<CommonResponse<RecommendationListResponseDTO<RestaurantRecommendationResponseDTO.Item>>> addRestaurantRecommendation(
            @CurrentUser Long userId,
            @RequestBody RestaurantRecommendationCreateRequestDTO request) {
        return GlobalResponseFactory.created(restaurantRecommendationService.addRestaurantRecommendation(userId, request));
    }

    @GetMapping
    public ResponseEntity<CommonResponse<RecommendationListResponseDTO<RestaurantRecommendationResponseDTO.Item>>> getRestaurantRecommendation(
            @CurrentUser Long userId,
            @RequestParam(value = "referenceMerchantId", required = false) Long referenceMerchantId,
            @RequestParam("mealType") MealType mealType,
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return GlobalResponseFactory.success(restaurantRecommendationService.findRestaurantRecommendation(
                userId, referenceMerchantId, mealType, cursor, size));
    }

    @PostMapping("/{recommendationRequestId}/recalculate")
    public ResponseEntity<CommonResponse<RecommendationListResponseDTO<RestaurantRecommendationResponseDTO.Item>>> recalculateRestaurantRecommendation(
            @CurrentUser Long userId,
            @PathVariable("recommendationRequestId") Long recommendationRequestId,
            @RequestBody RecommendationRecalculateRequestDTO request) {
        return GlobalResponseFactory.created(restaurantRecommendationService.recalculateRestaurant(
                userId, recommendationRequestId, request));
    }
}
