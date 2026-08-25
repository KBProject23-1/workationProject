package com.workit.domain.tourism.vo;

import org.springframework.util.StringUtils;

public enum TourismAccommodationType {
    HOTEL, PENSION, RESORT, GUESTHOUSE, POOL_VILLA;

    public static TourismAccommodationType resolve(String classification2,
                                                   String classification3,
                                                   String title) {
        String name = StringUtils.hasText(title) ? title.replace(" ", "") : "";
        if (name.contains("풀빌라")) return POOL_VILLA;
        if (name.contains("게스트하우스") || name.contains("호스텔")) return GUESTHOUSE;
        if (name.contains("리조트") || name.contains("콘도")) return RESORT;
        if (name.contains("펜션") || name.contains("민박") || name.contains("한옥")) {
            return PENSION;
        }
        if ("AC02".equals(classification2) || "AC020100".equals(classification3)
                || "AC020200".equals(classification3)
                || "VE050200".equals(classification3)) {
            return RESORT;
        }
        if ("AC03".equals(classification2)) return PENSION;
        if ("AC06".equals(classification2) || "AC060100".equals(classification3)
                || "AC060200".equals(classification3)) {
            return GUESTHOUSE;
        }
        return HOTEL;
    }
}
