package com.workit.domain.bookmark.dto.response;

import com.workit.domain.bookmark.vo.BookmarkVO;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class BookmarkListItemResponseDTO {

    private final Long bookmarkId;
    private final Long merchantId;
    private final String category;
    private final String name;
    private final String thumbnailUrl;
    private final String address;
    private final Long price;
    private final BigDecimal rating;
    private final LocalDateTime createdAt;

    public BookmarkListItemResponseDTO(Long bookmarkId,
                                      Long merchantId,
                                      String category,
                                      String name,
                                      String thumbnailUrl,
                                      String address,
                                      Long price,
                                      BigDecimal rating,
                                      LocalDateTime createdAt) {
        this.bookmarkId = bookmarkId;
        this.merchantId = merchantId;
        this.category = category;
        this.name = name;
        this.thumbnailUrl = thumbnailUrl;
        this.address = address;
        this.price = price;
        this.rating = rating;
        this.createdAt = createdAt;
    }

    public static BookmarkListItemResponseDTO from(BookmarkVO vo) {
        return new BookmarkListItemResponseDTO(
                vo.getBookmarkId(),
                vo.getMerchantId(),
                vo.getCategory(),
                vo.getName(),
                vo.getThumbnailUrl(),
                vo.getAddress(),
                vo.getPrice(),
                vo.getRating(),
                vo.getCreatedAt()
        );
    }
}
