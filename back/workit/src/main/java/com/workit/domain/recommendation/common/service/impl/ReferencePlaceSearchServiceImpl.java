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

@Service
@RequiredArgsConstructor
public class ReferencePlaceSearchServiceImpl implements ReferencePlaceSearchService {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

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

        return referencePlaceSearchMapper.selectReferencePlaceCandidatesByName(
                keyword.trim(),
                userId,
                workation.getId(),
                workation.getRegionId(),
                normalizeSize(size));
    }

    private int normalizeSize(Integer size) {
        if (size == null || size < 1 || size > MAX_SIZE) {
            return DEFAULT_SIZE;
        }
        return size;
    }
}
