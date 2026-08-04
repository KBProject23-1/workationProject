package com.workit.domain.merchant.controller;

import com.workit.domain.merchant.dto.request.AccommodationListRequestDTO;
import com.workit.domain.merchant.dto.request.RestaurantListRequestDTO;
import com.workit.domain.merchant.dto.response.AccommodationListResponseDTO;
import com.workit.domain.merchant.dto.response.AccommodationDetailResponseDTO;
import com.workit.domain.merchant.dto.response.RestaurantListResponseDTO;
import com.workit.domain.merchant.dto.response.RestaurantDetailResponseDTO;
import com.workit.domain.merchant.service.MerchantService;
import com.workit.global.dto.PageResponseDTO;
import com.workit.global.dto.CommonResponse;
import com.workit.global.response.GlobalResponseFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/merchants")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;

    // 지역과 선택 조회 조건에 맞는 숙소 목록 조회
    @GetMapping("/accommodations")
    public ResponseEntity<CommonResponse<PageResponseDTO<AccommodationListResponseDTO>>> accommodationList(
            @ModelAttribute AccommodationListRequestDTO request) {

        return GlobalResponseFactory.success(merchantService.findAccommodationList(request));
    }

    // 숙소의 기본 정보와 위치, 이용 정보, 태그 조회
    @GetMapping("/accommodations/{merchantId}")
    public ResponseEntity<CommonResponse<AccommodationDetailResponseDTO>> accommodationDetails(
            @PathVariable Long merchantId) {

        return GlobalResponseFactory.success(merchantService.findAccommodationDetails(merchantId));
    }

    // 지역과 선택 조회 조건에 맞는 음식점 목록 조회
    @GetMapping("/restaurants")
    public ResponseEntity<CommonResponse<PageResponseDTO<RestaurantListResponseDTO>>> restaurantList(
            @ModelAttribute RestaurantListRequestDTO request) {

        return GlobalResponseFactory.success(merchantService.findRestaurantList(request));
    }

    // 음식점의 기본 정보와 위치, 음식 종류, 가격, 태그 조회
    @GetMapping("/restaurants/{merchantId}")
    public ResponseEntity<CommonResponse<RestaurantDetailResponseDTO>> restaurantDetails(
            @PathVariable Long merchantId) {

        return GlobalResponseFactory.success(merchantService.findRestaurantDetails(merchantId));
    }
}
