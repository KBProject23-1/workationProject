package com.workit.domain.recommendation.controller;

import com.workit.domain.recommendation.accommodation.service.AccommodationRecommendationService;
import com.workit.domain.recommendation.dto.response.RecommendationCandidateListResponseDTO;
import com.workit.domain.recommendation.dto.response.RecommendationReferenceResponseDTO;
import com.workit.domain.recommendation.enums.RecommendationType;
import com.workit.domain.recommendation.enums.MealType;
import com.workit.domain.recommendation.dto.request.RecommendationRecalculateRequestDTO;
import com.workit.domain.recommendation.exception.RecommendationErrorCode;
import com.workit.domain.recommendation.mapper.RecommendationMapper;
import com.workit.domain.recommendation.restaurant.service.RestaurantRecommendationService;
import com.workit.domain.recommendation.service.RecommendationService;
import com.workit.domain.recommendation.vo.RecommendationRequestVO;
import com.workit.exception.BusinessException;
import com.workit.global.dto.CommonResponse;
import com.workit.global.response.GlobalResponseFactory;
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
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final AccommodationRecommendationService accommodationRecommendationService;
    private final RestaurantRecommendationService restaurantRecommendationService;
    private final RecommendationMapper recommendationMapper;

    @GetMapping("/reference-place")
    public ResponseEntity<CommonResponse<Object>> referencePlaceDetails(
            @RequestParam("recommendationType") RecommendationType recommendationType,
            @RequestParam(value = "mealType", required = false) MealType mealType) {
        // 인증 기능이 연결되면 JWT에서 사용자 ID를 가져오도록 교체
        Long userId = 1L;
        Object response = recommendationType == RecommendationType.RESTAURANT
                ? restaurantRecommendationService.findRestaurantReferencePlace(userId, mealType)
                : recommendationService.findReferencePlace(userId, recommendationType);
        return GlobalResponseFactory.success(response);
    }

    @GetMapping("/reference-place-candidates")
    public ResponseEntity<CommonResponse<RecommendationCandidateListResponseDTO>> referencePlaceCandidateList(
            @RequestParam(value = "recommendationType", defaultValue = "ACCOMMODATION")
            RecommendationType recommendationType) {
        // 인증 기능이 연결되면 JWT에서 사용자 ID를 가져오도록 교체
        Long userId = 1L;
        RecommendationCandidateListResponseDTO response = recommendationType == RecommendationType.RESTAURANT
                ? restaurantRecommendationService.findRestaurantReferencePlaceCandidates(userId)
                : recommendationService.findReferencePlaceCandidates(userId);
        return GlobalResponseFactory.success(response);
    }

    @PostMapping("/{recommendationRequestId}/recalculate")
    public ResponseEntity<CommonResponse<Object>> recommendationAdd(
            @PathVariable("recommendationRequestId") Long recommendationRequestId,
            @RequestBody RecommendationRecalculateRequestDTO request) {
        // 인증 기능이 연결되면 JWT에서 사용자 ID를 가져오도록 교체
        Long userId = 1L;
        RecommendationRequestVO previous = recommendationMapper.selectRecommendationRequest(recommendationRequestId, userId);
        if (previous == null) {
            throw new BusinessException(RecommendationErrorCode.INVALID_RECOMMENDATION_REQUEST);
        }
        if (previous.getRecommendationType() == RecommendationType.ACCOMMODATION) {
            return GlobalResponseFactory.created(
                    accommodationRecommendationService.recalculateAccommodation(userId, recommendationRequestId, request));
        }
        if (previous.getRecommendationType() == RecommendationType.RESTAURANT) {
            return GlobalResponseFactory.created(
                    restaurantRecommendationService.recalculateRestaurant(userId, recommendationRequestId, request));
        }
        throw new BusinessException(RecommendationErrorCode.INVALID_RECOMMENDATION_REQUEST);
    }
}
