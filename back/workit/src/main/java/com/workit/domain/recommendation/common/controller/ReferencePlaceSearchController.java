package com.workit.domain.recommendation.common.controller;

import com.workit.domain.recommendation.common.dto.response.ReferencePlaceCandidateListResponseDTO;
import com.workit.domain.recommendation.common.dto.response.ReferencePlaceCandidateResponseDTO;
import com.workit.domain.recommendation.common.service.ReferencePlaceSearchService;
import com.workit.domain.recommendation.common.vo.ReferencePlaceCandidateVO;
import com.workit.global.dto.CommonResponse;
import com.workit.global.response.GlobalResponseFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
public class ReferencePlaceSearchController {

    private final ReferencePlaceSearchService referencePlaceSearchService;

    @GetMapping("/reference-place-search")
    public ResponseEntity<CommonResponse<ReferencePlaceCandidateListResponseDTO>> search(
            @RequestParam("keyword") String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return GlobalResponseFactory.success(new ReferencePlaceCandidateListResponseDTO(Collections.emptyList()));
        }

        List<ReferencePlaceCandidateVO> candidates = referencePlaceSearchService.searchReferencePlaceCandidates(keyword);
        return GlobalResponseFactory.success(
                new ReferencePlaceCandidateListResponseDTO(
                        candidates.stream()
                                .map(ReferencePlaceCandidateResponseDTO::from)
                                .collect(Collectors.toList())
                )
        );
    }
}
