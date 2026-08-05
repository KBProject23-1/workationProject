package com.workit.domain.bookmark.mapper;

import com.workit.domain.bookmark.vo.BookmarkCategory;
import com.workit.domain.bookmark.vo.BookmarkVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BookmarkMapper {

    boolean selectMerchantExists(@Param("merchantId") Long merchantId);

    boolean selectActiveBookmarkExistsByUserAndMerchant(@Param("userId") Long userId,
                                                       @Param("merchantId") Long merchantId);

    void insertBookmark(BookmarkVO bookmark);

    BookmarkVO selectActiveBookmarkById(@Param("bookmarkId") Long bookmarkId);

    void updateBookmarkDeletedAt(@Param("bookmarkId") Long bookmarkId);

    BookmarkVO selectBookmarkDetail(@Param("bookmarkId") Long bookmarkId);

    List<BookmarkVO> selectBookmarks(@Param("userId") Long userId,
                                    @Param("category") BookmarkCategory category,
                                    @Param("cursor") Long cursor,
                                    @Param("size") int size);
}
