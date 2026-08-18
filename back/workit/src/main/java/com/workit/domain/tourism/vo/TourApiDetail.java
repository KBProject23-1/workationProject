package com.workit.domain.tourism.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TourApiDetail {
    private final String description;
    private final String phoneNumber;
    private final String checkInTime;
    private final String checkOutTime;
}
