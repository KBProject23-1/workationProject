package com.workit.domain.recommendation.common.mapper;

import com.workit.domain.recommendation.common.vo.ReferencePlaceCandidateVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ReferencePlaceSearchMapper {

    // categories 가 비면 업종을 제한하지 않는다
    List<ReferencePlaceCandidateVO> selectReferencePlaceCandidatesByName(
            @Param("keyword") String keyword,
            @Param("userId") Long userId,
            @Param("workationId") Long workationId,
            @Param("regionId") Long regionId,
            @Param("categories") List<String> categories,
            @Param("size") int size);
}
