package com.workit.domain.bookmark.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class BookmarkListResponseDTO {

    private final List<BookmarkListItemResponseDTO> content;
    private final String nextCursor;
    private final int size;
    private final boolean hasNext;

    public static BookmarkListResponseDTO of(List<BookmarkListItemResponseDTO> content,
                                            String nextCursor,
                                            int size,
                                            boolean hasNext) {
        return BookmarkListResponseDTO.builder()
                .content(content)
                .nextCursor(nextCursor)
                .size(size)
                .hasNext(hasNext)
                .build();
    }
}
