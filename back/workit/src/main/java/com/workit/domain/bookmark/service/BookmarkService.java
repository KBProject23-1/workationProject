package com.workit.domain.bookmark.service;

import com.workit.domain.bookmark.dto.request.BookmarkCreateRequestDTO;
import com.workit.domain.bookmark.dto.response.BookmarkCreateResponseDTO;
import com.workit.domain.bookmark.dto.response.BookmarkListResponseDTO;

public interface BookmarkService {

    BookmarkCreateResponseDTO addBookmark(Long userId, BookmarkCreateRequestDTO request);

    void removeBookmark(Long userId, Long bookmarkId);

    BookmarkListResponseDTO findBookmarks(Long userId,
                                         String category,
                                         String cursor,
                                         String size,
                                         String sort);
}
