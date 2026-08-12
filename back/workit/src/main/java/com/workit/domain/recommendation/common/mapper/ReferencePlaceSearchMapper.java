package com.workit.domain.recommendation.common.mapper;

import com.workit.domain.recommendation.common.vo.ReferencePlaceCandidateVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ReferencePlaceSearchMapper {
    List<ReferencePlaceCandidateVO> selectReferencePlaceCandidatesByName(@Param("keyword") String keyword);
}

