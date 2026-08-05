package com.workit.domain.recommendation.controller;

import com.workit.domain.recommendation.dto.request.RecommendationRecalculateRequestDTO;
import com.workit.domain.recommendation.dto.response.AccommodationRecommendationResponseDTO;
import com.workit.domain.recommendation.dto.response.RecommendationCandidateListResponseDTO;
import com.workit.domain.recommendation.dto.response.RecommendationReferenceResponseDTO;
import com.workit.domain.recommendation.enums.RecommendationType;
import com.workit.domain.recommendation.service.RecommendationService;
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

    @PostMapping("/accommodations")
    public ResponseEntity<CommonResponse<AccommodationRecommendationResponseDTO>> accommodationRecommendationAdd() {
        // 인증 기능이 연결되면 JWT에서 사용자 ID를 가져오도록 교체
        Long userId = 1L;
        return GlobalResponseFactory.created(recommendationService.addAccommodationRecommendation(userId));
    }

    @GetMapping("/accommodations")
    public ResponseEntity<CommonResponse<AccommodationRecommendationResponseDTO>> accommodationRecommendationList(
            @RequestParam(value = "referenceMerchantId", required = false) Long referenceMerchantId,
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        // 인증 기능이 연결되면 JWT에서 사용자 ID를 가져오도록 교체
        Long userId = 1L;
        return GlobalResponseFactory.success(recommendationService.findAccommodationRecommendation(
                userId, referenceMerchantId, cursor, size));
    }

    @GetMapping("/reference-place")
    public ResponseEntity<CommonResponse<RecommendationReferenceResponseDTO>> referencePlaceDetails(
            @RequestParam("recommendationType") RecommendationType recommendationType) {
        // 인증 기능이 연결되면 JWT에서 사용자 ID를 가져오도록 교체
        Long userId = 1L;
        return GlobalResponseFactory.success(recommendationService.findReferencePlace(userId, recommendationType));
    }

    @GetMapping("/reference-place-candidates")
    public ResponseEntity<CommonResponse<RecommendationCandidateListResponseDTO>> referencePlaceCandidateList() {
        // 인증 기능이 연결되면 JWT에서 사용자 ID를 가져오도록 교체
        Long userId = 1L;
        return GlobalResponseFactory.success(recommendationService.findReferencePlaceCandidates(userId));
    }

    @PostMapping("/{recommendationRequestId}/recalculate")
    public ResponseEntity<CommonResponse<AccommodationRecommendationResponseDTO>> accommodationRecommendationAdd(
            @PathVariable("recommendationRequestId") Long recommendationRequestId,
            @RequestBody RecommendationRecalculateRequestDTO request) {
        // 인증 기능이 연결되면 JWT에서 사용자 ID를 가져오도록 교체
        Long userId = 1L;
        return GlobalResponseFactory.created(recommendationService.recalculateAccommodation(
                userId, recommendationRequestId, request));
    }
}
