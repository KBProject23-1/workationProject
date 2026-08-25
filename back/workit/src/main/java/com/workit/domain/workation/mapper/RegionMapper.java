package com.workit.domain.workation.mapper;

import com.workit.domain.workation.vo.Region;

import java.util.List;

public interface RegionMapper {

    // 지역 마스터 전체 조회
    List<Region> selectRegionList();
}
