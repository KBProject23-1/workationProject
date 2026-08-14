package com.workit.domain.recommendation.common.controller;

import com.workit.domain.recommendation.common.dto.response.ReferencePlaceCandidateListResponseDTO;
import com.workit.domain.recommendation.common.dto.response.ReferencePlaceCandidateResponseDTO;
import com.workit.domain.recommendation.common.service.ReferencePlaceSearchService;
import com.workit.domain.recommendation.common.vo.ReferencePlaceCandidateVO;
import com.workit.global.dto.CommonResponse;
import com.workit.global.response.GlobalResponseFactory;
import com.workit.security.CurrentUser;
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

    // type 은 기존 클라이언트 호환을 위해 받지만 검색 업종은 제한하지 않는다.
    // 지역은 진행 중인 워케이션에서 서버가 가져오므로 받지 않는다
    @GetMapping("/reference-place-search")
    public ResponseEntity<CommonResponse<ReferencePlaceCandidateListResponseDTO>> search(
            @CurrentUser Long userId,
            @RequestParam("keyword") String keyword,
            @RequestParam(value = "type", required = false) String recommendationType,
            @RequestParam(value = "size", required = false) Integer size) {
        if (!StringUtils.hasText(keyword)) {
            return GlobalResponseFactory.success(new ReferencePlaceCandidateListResponseDTO(Collections.emptyList()));
        }

        List<ReferencePlaceCandidateVO> candidates =
                referencePlaceSearchService.searchReferencePlaceCandidates(
                        userId, keyword, recommendationType, size);
        return GlobalResponseFactory.success(
                new ReferencePlaceCandidateListResponseDTO(
                        candidates.stream()
                                .map(ReferencePlaceCandidateResponseDTO::from)
                                .collect(Collectors.toList())
                )
        );
    }
}
