package com.workit.domain.bookmark.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class BookmarkVO {

    private Long bookmarkId;
    private Long userId;
    private Long merchantId;
    private String category;
    private String name;
    private String address;
    private String thumbnailUrl;
    private Long price;
    private BigDecimal rating;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime isDeleted;
}
