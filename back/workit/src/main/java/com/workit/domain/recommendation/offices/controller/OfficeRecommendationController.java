package com.workit.domain.recommendation.offices.controller;

import com.workit.domain.recommendation.offices.dto.request.OfficeRecommendationCreateRequestDTO;
import com.workit.domain.recommendation.offices.dto.response.OfficeRecommendationResultItemResponseDTO;
import com.workit.domain.recommendation.common.dto.RecommendationListResponseDTO;
import com.workit.domain.recommendation.offices.service.OfficeRecommendationService;
import com.workit.global.dto.CommonResponse;
import com.workit.global.response.GlobalResponseFactory;
import com.workit.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
public class OfficeRecommendationController {

    private final OfficeRecommendationService officeRecommendationService;

    @PostMapping("/offices")
    public ResponseEntity<CommonResponse<RecommendationListResponseDTO<OfficeRecommendationResultItemResponseDTO>>> officeRecommendationCreate(
            @CurrentUser Long userId,
            @RequestBody OfficeRecommendationCreateRequestDTO request) {
        RecommendationListResponseDTO<OfficeRecommendationResultItemResponseDTO> response =
                officeRecommendationService.createOfficeRecommendation(userId, request);
        return GlobalResponseFactory.created(response);
    }

    @GetMapping("/offices")
    public ResponseEntity<CommonResponse<RecommendationListResponseDTO<OfficeRecommendationResultItemResponseDTO>>> officeRecommendationGet(
            @CurrentUser Long userId,
            @RequestParam(value = "referenceMerchantId", required = false) Long referenceMerchantId,
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "size", defaultValue = "20") Integer size) {
        return GlobalResponseFactory.success(
                officeRecommendationService.getOfficeRecommendation(userId, referenceMerchantId, cursor, size)
        );
    }

    @GetMapping("/{recommendationRequestId}")
    public ResponseEntity<CommonResponse<RecommendationListResponseDTO<OfficeRecommendationResultItemResponseDTO>>> officeRecommendationGetByRequestId(
            @CurrentUser Long userId,
            @PathVariable("recommendationRequestId") Long recommendationRequestId,
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "size", defaultValue = "20") Integer size) {
        return GlobalResponseFactory.success(
                officeRecommendationService.getOfficeRecommendationByRequestId(userId, recommendationRequestId, cursor, size)
        );
    }
}
