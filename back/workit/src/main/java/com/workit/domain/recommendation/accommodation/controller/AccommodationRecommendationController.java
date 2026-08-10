package com.workit.domain.recommendation.accommodation.controller;

import com.workit.domain.recommendation.accommodation.dto.request.RecommendationRecalculateRequestDTO;
import com.workit.domain.recommendation.accommodation.dto.response.AccommodationRecommendationResponseDTO;
import com.workit.domain.recommendation.accommodation.dto.response.RecommendationCandidateListResponseDTO;
import com.workit.domain.recommendation.accommodation.dto.response.RecommendationReferenceResponseDTO;
import com.workit.domain.recommendation.common.dto.RecommendationListResponseDTO;
import com.workit.domain.recommendation.accommodation.service.AccommodationRecommendationService;
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
@RequestMapping("/api/v1/recommendations/accommodations")
@RequiredArgsConstructor
public class AccommodationRecommendationController {

    private final AccommodationRecommendationService accommodationRecommendationService;

    @GetMapping("/reference-place")
    public ResponseEntity<CommonResponse<RecommendationReferenceResponseDTO>> findReferencePlace(
            @CurrentUser Long userId) {
        return GlobalResponseFactory.success(accommodationRecommendationService.findAccommodationReferencePlace(userId));
    }

    @GetMapping("/reference-place-candidates")
    public ResponseEntity<CommonResponse<RecommendationCandidateListResponseDTO>> findReferencePlaceCandidates(
            @CurrentUser Long userId) {
        return GlobalResponseFactory.success(
                accommodationRecommendationService.findAccommodationReferencePlaceCandidates(userId));
    }

    @PostMapping
    public ResponseEntity<CommonResponse<RecommendationListResponseDTO<AccommodationRecommendationResponseDTO.Item>>> addAccommodationRecommendation(
            @CurrentUser Long userId) {
        return GlobalResponseFactory.created(accommodationRecommendationService.addAccommodationRecommendation(userId));
    }

    @GetMapping
    public ResponseEntity<CommonResponse<RecommendationListResponseDTO<AccommodationRecommendationResponseDTO.Item>>> getAccommodationRecommendation(
            @CurrentUser Long userId,
            @RequestParam(value = "referenceMerchantId", required = false) Long referenceMerchantId,
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return GlobalResponseFactory.success(accommodationRecommendationService.findAccommodationRecommendation(
                userId, referenceMerchantId, cursor, size));
    }

    @PostMapping("/{recommendationRequestId}/recalculate")
    public ResponseEntity<CommonResponse<RecommendationListResponseDTO<AccommodationRecommendationResponseDTO.Item>>> recalculateAccommodationRecommendation(
            @CurrentUser Long userId,
            @PathVariable("recommendationRequestId") Long recommendationRequestId,
            @RequestBody RecommendationRecalculateRequestDTO request) {
        return GlobalResponseFactory.created(
                accommodationRecommendationService.recalculateAccommodation(userId, recommendationRequestId, request));
    }
}
