package com.workit.domain.recommendation.offices.controller;

import com.workit.domain.recommendation.offices.dto.request.OfficeRecommendationCreateRequestDTO;
import com.workit.domain.recommendation.offices.dto.response.OfficeRecommendationResponseDTO;
import com.workit.domain.recommendation.offices.service.OfficeRecommendationService;
import com.workit.global.dto.CommonResponse;
import com.workit.global.response.GlobalResponseFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/recommendations/offices")
@RequiredArgsConstructor
public class OfficeRecommendationController {

    private final OfficeRecommendationService officeRecommendationService;

    @PostMapping
    public ResponseEntity<Void> officeRecommendationCreate(
            @RequestBody OfficeRecommendationCreateRequestDTO request) {

        // JWT 토큰에서 userId 추출로 교체 필요(추후 삭제)
        Long userId = 9001L;

        officeRecommendationService.createOfficeRecommendation(userId, request);
        return GlobalResponseFactory.noContent();
    }

    @GetMapping
    public ResponseEntity<CommonResponse<OfficeRecommendationResponseDTO>> officeRecommendationGet(
            @RequestParam(value = "referenceMerchantId", required = false) Long referenceMerchantId,
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "size", required = false) Integer size) {

        // JWT 토큰에서 userId 추출로 교체 필요(추후 삭제)
        Long userId = 9002L;

        return GlobalResponseFactory.success(
                officeRecommendationService.getOfficeRecommendation(userId, referenceMerchantId, cursor, size)
        );
    }
}
