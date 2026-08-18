package com.workit.domain.tourism.vo;

public enum TourismCategory {
    WATER_SPORTS("LS", "LS02", 28),
    LAND_SPORTS("LS", "LS01", 28),
    RURAL_EXPERIENCE("EX", "EX03", 12),
    WELLNESS_TOURISM("EX", "EX05", 12),
    CAFE_TEA_HOUSE("FD", "FD05", 39),
    NATURAL_PARK("NA", "NA04", 12),
    MOUNTAIN_SCENERY("NA", "NA01", 12),
    WATER_SENERY("NA", "NA02", 12),
    NATURAL_ECOLOGY("NA", "NA03", 12);

    private final String classification1;
    private final String classification2;
    private final int contentTypeId;

    TourismCategory(String classification1, String classification2, int contentTypeId) {
        this.classification1 = classification1;
        this.classification2 = classification2;
        this.contentTypeId = contentTypeId;
    }

    public String getClassification1() { return classification1; }
    public String getClassification2() { return classification2; }
    public int getContentTypeId() { return contentTypeId; }
}
