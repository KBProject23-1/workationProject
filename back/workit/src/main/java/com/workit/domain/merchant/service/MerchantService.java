package com.workit.domain.merchant.service;

import com.workit.domain.merchant.dto.MerchantItemResponseDTO;
import com.workit.domain.merchant.dto.MerchantListResponseDTO;
import com.workit.domain.merchant.dto.MerchantDetailResponseDTO;
import com.workit.domain.merchant.dto.MerchantDetailCommonResponseDTO;

import com.workit.domain.merchant.vo.MerchantSearchCondition;
import com.workit.domain.merchant.vo.MerchantSortType;
import java.time.LocalDate;

public interface MerchantService {

    MerchantListResponseDTO<MerchantItemResponseDTO> findMerchants(
            Long userId,
            MerchantSearchCondition condition
    );

    MerchantDetailResponseDTO findAccommodationProducts(
            Long userId,
            Long merchantId,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            Integer roomCount,
            Integer guestCount
    );

    MerchantDetailResponseDTO findOfficeProducts(
            Long userId,
            Long merchantId,
            LocalDate startDate,
            LocalDate endDate,
            Integer guestCount
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
