package com.workit.domain.recommendation.common.service;

import com.workit.domain.recommendation.common.vo.ReferencePlaceCandidateVO;

import java.util.List;

public interface ReferencePlaceSearchService {
    List<ReferencePlaceCandidateVO> searchReferencePlaceCandidates(String keyword);
}

