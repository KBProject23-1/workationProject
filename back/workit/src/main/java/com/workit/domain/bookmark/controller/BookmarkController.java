package com.workit.domain.bookmark.controller;

import com.workit.domain.bookmark.dto.request.BookmarkCreateRequestDTO;
import com.workit.domain.bookmark.dto.response.BookmarkCreateResponseDTO;
import com.workit.domain.bookmark.dto.response.BookmarkListResponseDTO;
import com.workit.domain.bookmark.service.BookmarkService;
import com.workit.global.dto.CommonResponse;
import com.workit.global.response.GlobalResponseFactory;
import com.workit.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkService bookmarkService;

    @PostMapping
    public ResponseEntity<CommonResponse<BookmarkCreateResponseDTO>> bookmarkAdd(
            //@CurrentUser Long userId,
            @RequestBody BookmarkCreateRequestDTO request
    ) {
        Long userId = 9001L;
        return GlobalResponseFactory.created(bookmarkService.addBookmark(userId, request));
    }

    @DeleteMapping("/{bookmarkId}")
    public ResponseEntity<Void> bookmarkRemove(
            //@CurrentUser Long userId,
            @PathVariable Long bookmarkId
    ) {
        Long userId = 9001L;
        bookmarkService.removeBookmark(userId, bookmarkId);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<CommonResponse<BookmarkListResponseDTO>> bookmarkList(
            //@CurrentUser Long userId,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "size", required = false) String size,
            @RequestParam(value = "sort", required = false) String sort
    ) {
        Long userId = 9001L;
        return GlobalResponseFactory.success(
                bookmarkService.findBookmarks(userId, category, cursor, size, sort)
        );
    }
}
