package com.workit.domain.merchant.service;

import com.workit.domain.merchant.dto.MerchantItemResponseDTO;
import com.workit.domain.merchant.dto.MerchantListResponseDTO;
import com.workit.domain.merchant.dto.MerchantDetailResponseDTO;
import com.workit.domain.merchant.dto.MerchantDetailCommonResponseDTO;

import com.workit.domain.merchant.vo.MerchantSortType;
import java.time.LocalDate;

public interface MerchantService {

    MerchantListResponseDTO<MerchantItemResponseDTO> findMerchants(
            String category,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            Integer headcount,
            Long minPrice,
            Long maxPrice,
            MerchantSortType sort,
            String cursor,
            int size,
            Long regionId
    );

    MerchantDetailResponseDTO findAccommodationProducts(
            Long merchantId,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            Integer roomCount,
            Integer guestCount
    );

    MerchantDetailResponseDTO findOfficeProducts(
            Long merchantId
    );

    MerchantDetailCommonResponseDTO findRestaurantProducts(
            Long userId,
            Long merchantId
    );

    MerchantDetailCommonResponseDTO findActivityProducts(
            Long userId,
            Long merchantId
    );
}
