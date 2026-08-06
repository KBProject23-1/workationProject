package com.workit.domain.recommendation.offices.enums;

import java.util.Locale;

public enum OfficePriorityType {
    PRICE,
    ACCESSIBILITY,
    RATING,
    BALANCED;

    public static OfficePriorityType from(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        String value = raw.trim().toUpperCase(Locale.ROOT);
        for (OfficePriorityType type : values()) {
            if (type.name().equals(value)) {
                return type;
            }
        }
        return null;
    }
}

