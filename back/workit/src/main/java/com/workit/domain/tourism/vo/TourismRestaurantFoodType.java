package com.workit.domain.tourism.vo;

import org.springframework.util.StringUtils;

public enum TourismRestaurantFoodType {
    KOREAN, JAPANESE, CHINESE, WESTERN, CAFE, DESSERT, BAR;

    public static TourismRestaurantFoodType resolve(String classification2,
                                                    String classification3,
                                                    String title) {
        String name = StringUtils.hasText(title) ? title.replace(" ", "") : "";
        if (name.contains("카페") || name.contains("커피") || name.contains("다방")
                || name.contains("찻집") || name.contains("티하우스")) {
            return CAFE;
        }
        if (name.contains("베이커리") || name.contains("제과") || name.contains("디저트")) {
            return DESSERT;
        }
        if ("FD01".equals(classification2)) return KOREAN;
        if ("FD04".equals(classification2)) return BAR;
        if ("FD05".equals(classification2)) return CAFE;
        if ("FD020100".equals(classification3)) return CHINESE;
        if ("FD020200".equals(classification3)) return JAPANESE;
        if ("FD030100".equals(classification3)) return DESSERT;
        if ("FD02".equals(classification2) || "FD03".equals(classification2)) {
            return WESTERN;
        }
        return KOREAN;
    }
}
