package com.workit.domain.recommendation.offices.controller;

import com.workit.domain.recommendation.offices.dto.request.OfficeRecommendationCreateRequestDTO;
import com.workit.domain.recommendation.offices.dto.request.OfficeRecommendationRecalculateRequestDTO;
import com.workit.domain.recommendation.offices.dto.response.OfficeRecommendationCandidateListResponseDTO;
import com.workit.domain.recommendation.offices.dto.response.OfficeRecommendationResultItemResponseDTO;
import com.workit.domain.recommendation.offices.dto.response.OfficeRecommendationReferenceResponseDTO;
import com.workit.domain.recommendation.common.dto.response.RecommendationListResponseDTO;
import com.workit.domain.recommendation.offices.service.OfficeRecommendationService;
import com.workit.global.dto.CommonResponse;
import com.workit.global.response.GlobalResponseFactory;
import com.workit.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/recommendations/offices")
@RequiredArgsConstructor
public class OfficeRecommendationController {

    private final OfficeRecommendationService officeRecommendationService;

    @GetMapping("/reference-place")
    public ResponseEntity<CommonResponse<OfficeRecommendationReferenceResponseDTO>> findOfficeReferencePlace(
            @CurrentUser Long userId) {
        return GlobalResponseFactory.success(officeRecommendationService.findOfficeReferencePlace(userId));
    }

    @GetMapping("/reference-place-candidates")
    public ResponseEntity<CommonResponse<OfficeRecommendationCandidateListResponseDTO>> findOfficeReferencePlaceCandidates(
            @CurrentUser Long userId) {
        return GlobalResponseFactory.success(
                officeRecommendationService.findOfficeReferencePlaceCandidates(userId)
        );
    }

    @PostMapping
    public ResponseEntity<CommonResponse<RecommendationListResponseDTO<OfficeRecommendationResultItemResponseDTO>>> officeRecommendationCreate(
            @CurrentUser Long userId,
            @RequestBody OfficeRecommendationCreateRequestDTO request) {
        RecommendationListResponseDTO<OfficeRecommendationResultItemResponseDTO> response =
                officeRecommendationService.createOfficeRecommendation(userId, request);
        return GlobalResponseFactory.created(response);
    }

    @GetMapping
    public ResponseEntity<CommonResponse<RecommendationListResponseDTO<OfficeRecommendationResultItemResponseDTO>>> officeRecommendationGet(
            @CurrentUser Long userId,
            @RequestParam(value = "referenceMerchantId", required = false) Long referenceMerchantId,
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "size", defaultValue = "20") Integer size) {
        return GlobalResponseFactory.success(
                officeRecommendationService.getOfficeRecommendation(userId, referenceMerchantId, cursor, size)
        );
    }

    @PostMapping("/{recommendationRequestId}/recalculate")
    public ResponseEntity<CommonResponse<RecommendationListResponseDTO<OfficeRecommendationResultItemResponseDTO>>> officeRecommendationRecalculate(
            @CurrentUser Long userId,
            @PathVariable("recommendationRequestId") Long recommendationRequestId,
            @RequestBody OfficeRecommendationRecalculateRequestDTO request) {
        return GlobalResponseFactory.created(
                officeRecommendationService.recalculateOfficeRecommendation(userId, recommendationRequestId, request)
        );
    }
}
