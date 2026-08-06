package com.workit.domain.bookmark.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class BookmarkCreateResponseDTO {

    private final Long bookmarkId;
    private final Long merchantId;
    private final LocalDateTime createdAt;

    public static BookmarkCreateResponseDTO of(Long bookmarkId, Long merchantId, LocalDateTime createdAt) {
        return new BookmarkCreateResponseDTO(bookmarkId, merchantId, createdAt);
    }
}
