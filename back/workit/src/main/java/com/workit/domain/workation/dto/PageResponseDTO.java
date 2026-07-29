package com.workit.domain.workation.dto;

import lombok.Getter;

import java.util.List;

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
        this.totalPages = (size == 0) ? 0 : (int) Math.ceil((double) totalElements / size);
    }

    public static <T> PageResponseDTO<T> of(List<T> content, int page, int size, long totalElements) {
        return new PageResponseDTO<>(content, page, size, totalElements);
    }
}