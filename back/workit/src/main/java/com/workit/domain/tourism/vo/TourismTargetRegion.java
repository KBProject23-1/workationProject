package com.workit.domain.tourism.vo;

public enum TourismTargetRegion {
    BUSAN("부산", "26", null),
    GANGNEUNG("강릉", "51", "150"),
    YEOSU("여수", "46", "130"),
    JEJU("제주", "50", null);

    private final String regionName;
    private final String legalDongRegionCode;
    private final String legalDongSigunguCode;

    TourismTargetRegion(String regionName, String legalDongRegionCode,
                        String legalDongSigunguCode) {
        this.regionName = regionName;
        this.legalDongRegionCode = legalDongRegionCode;
        this.legalDongSigunguCode = legalDongSigunguCode;
    }

    public String getRegionName() {
        return regionName;
    }

    public String getLegalDongRegionCode() {
        return legalDongRegionCode;
    }

    public String getLegalDongSigunguCode() {
        return legalDongSigunguCode;
    }
}
