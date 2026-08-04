package com.workit.domain.workation.dto.response;

import com.workit.domain.workation.vo.Region;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

// 워케이션 등록·수정 화면의 지역 선택 목록
@Getter
@Builder
public class RegionListResponseDTO {

    private final List<RegionItem> regions;

    public static RegionListResponseDTO from(List<Region> regions) {
        return RegionListResponseDTO.builder()
                .regions(regions.stream()
                        .map(RegionItem::from)
                        .collect(Collectors.toList()))
                .build();
    }

    @Getter
    @Builder
    public static class RegionItem {

        private final Long id;
        private final String name;

        private static RegionItem from(Region region) {
            return RegionItem.builder()
                    .id(region.getId())
                    .name(region.getName())
                    .build();
        }
    }
}
