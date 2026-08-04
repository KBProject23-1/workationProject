package com.workit.domain.merchant.service;

import com.workit.domain.merchant.dto.request.AccommodationListRequestDTO;
import com.workit.domain.merchant.dto.request.RestaurantListRequestDTO;
import com.workit.domain.merchant.dto.response.AccommodationListResponseDTO;
import com.workit.domain.merchant.dto.response.AccommodationDetailResponseDTO;
import com.workit.domain.merchant.dto.response.RestaurantListResponseDTO;
import com.workit.domain.merchant.dto.response.RestaurantDetailResponseDTO;
import com.workit.global.dto.PageResponseDTO;

public interface MerchantService {

    PageResponseDTO<AccommodationListResponseDTO> findAccommodationList(
            AccommodationListRequestDTO request
    );

    AccommodationDetailResponseDTO findAccommodationDetails(Long merchantId);

    PageResponseDTO<RestaurantListResponseDTO> findRestaurantList(RestaurantListRequestDTO request);

    RestaurantDetailResponseDTO findRestaurantDetails(Long merchantId);
}
