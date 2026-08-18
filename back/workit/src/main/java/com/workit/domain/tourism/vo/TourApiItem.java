package com.workit.domain.tourism.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class TourApiItem {
    private String contentId;
    private String title;
    private String address1;
    private String address2;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String phoneNumber;
    private String thumbnailUrl;
    private String modifiedTime;
    private String description;
    private String classification1;
    private String classification2;
    private String classification3;
    private String checkInTime;
    private String checkOutTime;
    private boolean detailFetched;
    private boolean active;
    private TourismCategory category;
    private TourismPlaceType placeType;
    private int contentTypeId;
    private Long syncId;
}
