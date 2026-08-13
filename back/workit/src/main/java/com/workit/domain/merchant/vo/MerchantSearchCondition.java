package com.workit.domain.merchant.vo;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

// 예약하기 화면의 검색 조건.
// 예약 유형별로 쓰는 필터가 달라 파라미터가 계속 늘어나므로 한 객체로 묶는다
@Getter
@Builder
public class MerchantSearchCondition {

    private final String category;

    // 숙소·공유오피스만 쓴다. 음식점·여가는 재고가 없어 날짜를 보지 않는다
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final Integer headcount;
    private final Integer roomCount;

    private final Long minPrice;
    private final Long maxPrice;

    // 유형별 상세 필터
    private final String accommodationType;
    private final String noiseLevel;
    private final String foodType;
    private final Integer priceLevel;
    private final String activityType;

    private final MerchantSortType sort;
    private final String cursor;
    private final int size;
    private final Long regionId;
}
