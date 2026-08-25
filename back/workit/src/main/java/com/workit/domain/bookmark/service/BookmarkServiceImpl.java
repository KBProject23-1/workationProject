package com.workit.domain.bookmark.service;

import com.workit.domain.bookmark.dto.request.BookmarkCreateRequestDTO;
import com.workit.domain.bookmark.dto.response.BookmarkCreateResponseDTO;
import com.workit.domain.bookmark.dto.response.BookmarkListItemResponseDTO;
import com.workit.domain.bookmark.dto.response.BookmarkListResponseDTO;
import com.workit.domain.bookmark.exception.BookmarkErrorCode;
import com.workit.domain.bookmark.mapper.BookmarkMapper;
import com.workit.domain.bookmark.vo.BookmarkCategory;
import com.workit.domain.bookmark.vo.BookmarkSortType;
import com.workit.domain.bookmark.vo.BookmarkVO;
import com.workit.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookmarkServiceImpl implements BookmarkService {

    private static final int DEFAULT_SIZE = 20;
    private static final int MIN_SIZE = 1;
    private static final int MAX_SIZE = 100;

    private final BookmarkMapper bookmarkMapper;

    @Override
    @Transactional
    public BookmarkCreateResponseDTO addBookmark(Long userId, BookmarkCreateRequestDTO request) {
        validateUserId(userId);
        validateCreateRequest(request);

        Long merchantId = request.getMerchantId();
        if (!bookmarkMapper.selectMerchantExists(merchantId)) {
            throw new BusinessException(BookmarkErrorCode.MERCHANT_NOT_FOUND);
        }

        if (bookmarkMapper.selectActiveBookmarkExistsByUserAndMerchant(userId, merchantId)) {
            throw new BusinessException(BookmarkErrorCode.BOOKMARK_ALREADY_EXISTS);
        }

        BookmarkVO bookmark = new BookmarkVO();
        bookmark.setUserId(userId);
        bookmark.setMerchantId(merchantId);
        bookmarkMapper.insertBookmark(bookmark);

        BookmarkVO saved = bookmarkMapper.selectBookmarkDetail(bookmark.getBookmarkId());
        if (saved == null) {
            throw new BusinessException(BookmarkErrorCode.BOOKMARK_NOT_FOUND);
        }

        return BookmarkCreateResponseDTO.of(saved.getBookmarkId(), saved.getMerchantId(), saved.getCreatedAt());
    }

    @Override
    @Transactional
    public void removeBookmark(Long userId, Long bookmarkId) {
        validateUserId(userId);
        validateBookmarkId(bookmarkId);

        BookmarkVO bookmark = bookmarkMapper.selectActiveBookmarkById(bookmarkId);
        if (bookmark == null) {
            throw new BusinessException(BookmarkErrorCode.BOOKMARK_NOT_FOUND);
        }

        if (!userId.equals(bookmark.getUserId())) {
            throw new BusinessException(BookmarkErrorCode.BOOKMARK_ACCESS_DENIED);
        }

        bookmarkMapper.updateBookmarkDeletedAt(bookmarkId);
    }

    @Override
    @Transactional(readOnly = true)
    public BookmarkListResponseDTO findBookmarks(Long userId,
                                                String category,
                                                String cursor,
                                                String size,
                                                String sort) {
        validateUserId(userId);

        BookmarkCategory parsedCategory = parseCategory(category);
        Long parsedCursor = parseCursor(cursor);
        int parsedSize = parseSize(size);
        parseSort(sort);

        List<BookmarkVO> fetchedBookmarks = bookmarkMapper.selectBookmarks(
                userId,
                parsedCategory,
                parsedCursor,
                parsedSize + 1
        );

        boolean hasNext = fetchedBookmarks.size() > parsedSize;
        List<BookmarkVO> pagedBookmarks = hasNext
                ? fetchedBookmarks.subList(0, parsedSize)
                : fetchedBookmarks;

        List<BookmarkListItemResponseDTO> content = pagedBookmarks.stream()
                .map(BookmarkListItemResponseDTO::from)
                .collect(Collectors.toList());

        String nextCursor = hasNext && !content.isEmpty()
                ? String.valueOf(content.get(content.size() - 1).getBookmarkId())
                : null;

        return BookmarkListResponseDTO.of(content, nextCursor, parsedSize, hasNext);
    }

    private void validateUserId(Long userId) {
        if (userId == null || userId < 1) {
            throw new IllegalArgumentException("사용자 번호는 1 이상이어야 합니다.");
        }
    }

    private void validateCreateRequest(BookmarkCreateRequestDTO request) {
        if (request == null || request.getMerchantId() == null || request.getMerchantId() < 1) {
            throw new IllegalArgumentException("가맹점 번호는 1 이상이어야 합니다.");
        }
    }

    private void validateBookmarkId(Long bookmarkId) {
        if (bookmarkId == null || bookmarkId < 1) {
            throw new BusinessException(BookmarkErrorCode.INVALID_BOOKMARK_ID);
        }
    }

    private BookmarkCategory parseCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return null;
        }

        try {
            return BookmarkCategory.valueOf(category.toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(BookmarkErrorCode.INVALID_BOOKMARK_SEARCH_CONDITION);
        }
    }

    private Long parseCursor(String cursor) {
        if (cursor == null || cursor.trim().isEmpty()) {
            return null;
        }

        try {
            Long parsedCursor = Long.parseLong(cursor);
            if (parsedCursor < 1) {
                throw new BusinessException(BookmarkErrorCode.INVALID_BOOKMARK_CURSOR);
            }
            return parsedCursor;
        } catch (NumberFormatException exception) {
            throw new BusinessException(BookmarkErrorCode.INVALID_BOOKMARK_CURSOR);
        }
    }

    private int parseSize(String size) {
        if (size == null || size.trim().isEmpty()) {
            return DEFAULT_SIZE;
        }

        try {
            int parsedSize = Integer.parseInt(size);
            if (parsedSize < MIN_SIZE || parsedSize > MAX_SIZE) {
                throw new BusinessException(BookmarkErrorCode.INVALID_BOOKMARK_SEARCH_CONDITION);
            }
            return parsedSize;
        } catch (NumberFormatException exception) {
            throw new BusinessException(BookmarkErrorCode.INVALID_BOOKMARK_SEARCH_CONDITION);
        }
    }

    private BookmarkSortType parseSort(String sort) {
        if (sort == null || sort.trim().isEmpty()) {
            return BookmarkSortType.RECENT;
        }

        String normalizedSort = sort.trim().toUpperCase();
        try {
            return BookmarkSortType.valueOf(normalizedSort);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(BookmarkErrorCode.INVALID_BOOKMARK_SEARCH_CONDITION);
        }
    }
}
