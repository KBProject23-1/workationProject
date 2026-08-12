package com.workit.domain.recommendation.common.service.impl;

import com.workit.domain.recommendation.common.mapper.ReferencePlaceSearchMapper;
import com.workit.domain.recommendation.common.service.ReferencePlaceSearchService;
import com.workit.domain.recommendation.common.vo.ReferencePlaceCandidateVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReferencePlaceSearchServiceImpl implements ReferencePlaceSearchService {

    private final ReferencePlaceSearchMapper referencePlaceSearchMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ReferencePlaceCandidateVO> searchReferencePlaceCandidates(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return Collections.emptyList();
        }
        return referencePlaceSearchMapper.selectReferencePlaceCandidatesByName(keyword.trim());
    }
}

