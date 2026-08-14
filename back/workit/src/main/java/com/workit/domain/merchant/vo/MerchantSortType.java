package com.workit.domain.merchant.vo;

import com.workit.exception.BusinessException;

import java.util.Locale;

public enum MerchantSortType {

    RATING_DESC,
    PRICE_ASC,
    PRICE_DESC;

    public static MerchantSortType from(String value) {
        if (value == null || value.trim().isEmpty()) {
            return RATING_DESC;
        }

        String normalized = value.trim().toUpperCase(Locale.ENGLISH);

        switch (normalized) {
            case "RATING_DESC":
                return RATING_DESC;
            case "PRICE_ASC":
                return PRICE_ASC;
            case "PRICE_DESC":
                return PRICE_DESC;
            default:
                throw new BusinessException(MerchantErrorCode.INVALID_SORT);
        }
    }
}
