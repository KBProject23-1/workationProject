package com.workit.domain.recommendation.common.service;

import com.workit.domain.recommendation.common.vo.ReferencePlaceCandidateVO;

import java.util.List;

public interface ReferencePlaceSearchService {

    // 진행 중인 워케이션의 지역 안에서 이름으로 기준 장소를 찾는다.
    // recommendationType 을 주면 그 추천의 기준이 될 수 있는 업종으로 좁힌다
    List<ReferencePlaceCandidateVO> searchReferencePlaceCandidates(Long userId,
                                                                   String keyword,
                                                                   String recommendationType,
                                                                   Integer size);
}
