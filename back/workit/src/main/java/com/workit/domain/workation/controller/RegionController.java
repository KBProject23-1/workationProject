package com.workit.domain.workation.controller;

import com.workit.domain.workation.dto.response.RegionListResponseDTO;
import com.workit.domain.workation.service.RegionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/regions")
@RequiredArgsConstructor
@Slf4j
public class RegionController {

    private final RegionService regionService;

    // 1.7 지역 목록 조회
    // 워케이션 등록·수정 화면의 지역 선택에 사용한다. 사용자별로 달라지는 값이 없다
    @GetMapping
    public ResponseEntity<RegionListResponseDTO> regionListGet() {
        return ResponseEntity.ok(regionService.getRegionList());
    }
}
