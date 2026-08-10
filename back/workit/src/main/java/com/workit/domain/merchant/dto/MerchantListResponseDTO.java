package com.workit.domain.merchant.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MerchantListResponseDTO<T> {

    private final List<T> content;
    private final PageInfo pageInfo;

    private MerchantListResponseDTO(List<T> content, int size, boolean hasNext, String nextCursor) {
        this.content = content;
        int numberOfElements = content == null ? 0 : content.size();
        this.pageInfo = new PageInfo(size, numberOfElements, hasNext, nextCursor);
    }

    public static <T> MerchantListResponseDTO<T> of(List<T> content, String nextCursor, int size, boolean hasNext) {
        List<T> safeContent = content == null ? new ArrayList<>() : content;
        return new MerchantListResponseDTO<>(safeContent, size, hasNext, nextCursor);
    }

    public static <T> MerchantListResponseDTO<T> ofAll(List<T> content) {
        List<T> safeContent = content == null ? new ArrayList<>() : content;
        return MerchantListResponseDTO.of(safeContent, null, safeContent.size(), false);
    }

    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PageInfo {

        private final int size;
        private final int numberOfElements;
        private final boolean hasNext;
        private final String nextCursor;

        private PageInfo(int size, int numberOfElements, boolean hasNext, String nextCursor) {
            this.size = size;
            this.numberOfElements = numberOfElements;
            this.hasNext = hasNext;
            this.nextCursor = nextCursor;
        }
    }
}
