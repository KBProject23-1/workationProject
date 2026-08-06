package com.workit.domain.recommendation.offices.enums;

import java.util.Locale;

public enum OfficeAtmosphereType {
    QUIET,
    OPEN,
    COLLAB;

    public static OfficeAtmosphereType from(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        String value = raw.trim().toUpperCase(Locale.ROOT);
        if ("COLLABORATION".equals(value)) {
            return COLLAB;
        }
        for (OfficeAtmosphereType type : values()) {
            if (type.name().equals(value)) {
                return type;
            }
        }
        return null;
    }

    public String toResponseValue() {
        return this == COLLAB ? "COLLABORATION" : this.name();
    }
}

