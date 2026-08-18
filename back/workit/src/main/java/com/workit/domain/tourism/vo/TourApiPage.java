package com.workit.domain.tourism.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class TourApiPage {
    private final List<TourApiItem> items;
    private final int numOfRows;
    private final int totalCount;
}
