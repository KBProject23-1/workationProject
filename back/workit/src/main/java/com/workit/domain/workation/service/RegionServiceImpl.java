package com.workit.domain.workation.service;

import com.workit.domain.workation.dto.response.RegionListResponseDTO;
import com.workit.domain.workation.mapper.RegionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegionServiceImpl implements RegionService {

    private final RegionMapper regionMapper;

    @Override
    @Transactional(readOnly = true)
    public RegionListResponseDTO getRegionList() {
        return RegionListResponseDTO.from(regionMapper.selectRegionList());
    }
}
