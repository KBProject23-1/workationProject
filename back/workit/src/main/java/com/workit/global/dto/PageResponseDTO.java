package com.workit.global.dto;

import lombok.Getter;

import java.util.List;

// 목록 조회 API 공통 응답 포맷
// 워케이션 기록·지출 목록 등 페이징이 필요한 모든 도메인에서 사용함

@Getter
public class PageResponseDTO<T> {

    private final List<T> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;

    private PageResponseDTO(List<T> content, int page, int size, long totalElements) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        // 23건을 10개씩 나누면 3페이지. 나머지가 있으면 한 페이지 더 필요하므로 올림 처리
        this.totalPages = (size == 0) ? 0 : (int) Math.ceil((double) totalElements / size);
    }

    public static <T> PageResponseDTO<T> of(List<T> content, int page, int size, long totalElements) {
        return new PageResponseDTO<>(content, page, size, totalElements);
    }
}