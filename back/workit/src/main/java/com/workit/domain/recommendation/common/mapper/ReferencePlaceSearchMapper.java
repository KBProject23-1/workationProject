package com.workit.domain.recommendation.common.mapper;

import com.workit.domain.recommendation.common.vo.ReferencePlaceCandidateVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ReferencePlaceSearchMapper {

    // 진행 중인 워케이션 지역의 모든 업종을 이름으로 검색한다
    List<ReferencePlaceCandidateVO> selectReferencePlaceCandidatesByName(
            @Param("keyword") String keyword,
            @Param("userId") Long userId,
            @Param("workationId") Long workationId,
            @Param("regionId") Long regionId,
            @Param("size") int size);
}
