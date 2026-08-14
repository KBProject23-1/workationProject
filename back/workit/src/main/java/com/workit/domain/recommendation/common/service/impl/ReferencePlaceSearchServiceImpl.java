package com.workit.domain.recommendation.common.service.impl;

import com.workit.domain.recommendation.common.mapper.ReferencePlaceSearchMapper;
import com.workit.domain.recommendation.common.service.ReferencePlaceSearchService;
import com.workit.domain.recommendation.common.vo.ReferencePlaceCandidateVO;
import com.workit.domain.workation.mapper.WorkationMapper;
import com.workit.domain.workation.vo.WorkationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReferencePlaceSearchServiceImpl implements ReferencePlaceSearchService {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    // 추천 유형별로 기준 장소가 될 수 있는 업종.
    // 각 추천 서비스가 기준 장소를 다시 검증하므로 여기서 미리 맞춰야
    // 고를 수는 있는데 고르면 실패하는 상태가 생기지 않는다
    private static final Map<String, List<String>> REFERENCE_CATEGORIES = Map.of(
            "ACCOMMODATION", List.of("OFFICE"),
            "OFFICE", List.of("ACCOMMODATION"),
            "RESTAURANT", List.of("ACCOMMODATION", "OFFICE", "ACTIVITY"),
            "ACTIVITY", List.of("ACCOMMODATION", "OFFICE")
    );

    private final ReferencePlaceSearchMapper referencePlaceSearchMapper;
    private final WorkationMapper workationMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ReferencePlaceCandidateVO> searchReferencePlaceCandidates(Long userId,
                                                                          String keyword,
                                                                          String recommendationType,
                                                                          Integer size) {
        if (!StringUtils.hasText(keyword)) {
            return Collections.emptyList();
        }

        // 지역을 걸지 않으면 다른 지역 가맹점이 검색된다
        WorkationVO workation = workationMapper.selectActiveWorkation(userId);
        if (workation == null) {
            return Collections.emptyList();
        }

        List<String> categories = recommendationType == null
                ? null
                : REFERENCE_CATEGORIES.get(recommendationType.trim().toUpperCase(Locale.ROOT));

        return referencePlaceSearchMapper.selectReferencePlaceCandidatesByName(
                keyword.trim(),
                userId,
                workation.getId(),
                workation.getRegionId(),
                categories,
                normalizeSize(size));
    }

    private int normalizeSize(Integer size) {
        if (size == null || size < 1 || size > MAX_SIZE) {
            return DEFAULT_SIZE;
        }
        return size;
    }
}
