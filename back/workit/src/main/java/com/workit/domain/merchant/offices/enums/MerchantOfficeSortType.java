package com.workit.domain.merchant.offices.enums;

import com.workit.domain.merchant.offices.exception.MerchantOfficesErrorCode;
import com.workit.exception.BusinessException;

import java.util.Locale;

public enum MerchantOfficeSortType {

    RATING_HIGH,
    RATING_LOW,
    PRICE_HIGH,
    PRICE_LOW;

    public static MerchantOfficeSortType from(String value) {
        if (value == null || value.trim().isEmpty()) {
            return RATING_HIGH;
        }

        String normalized = value.trim().toUpperCase(Locale.ENGLISH);

        if (!normalized.equals(RATING_HIGH.name())
                && !normalized.equals(RATING_LOW.name())
                && !normalized.equals(PRICE_HIGH.name())
                && !normalized.equals(PRICE_LOW.name())) {
            throw new BusinessException(MerchantOfficesErrorCode.INVALID_SEARCH_CONDITION);
        }

        return MerchantOfficeSortType.valueOf(normalized);
    }

    public boolean isRatingSort() {
        return this == RATING_HIGH || this == RATING_LOW;
    }
}
