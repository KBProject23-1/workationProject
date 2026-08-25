package com.workit.domain.workation.service;

import com.workit.domain.workation.dto.response.RegionListResponseDTO;

public interface RegionService {

    // 1.7 지역 목록 조회
    RegionListResponseDTO getRegionList();
}
