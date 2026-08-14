package com.workit.domain.merchant.controller;

import com.workit.domain.merchant.dto.MerchantDetailCommonResponseDTO;
import com.workit.domain.merchant.service.MerchantService;
import com.workit.domain.merchant.dto.MerchantDetailResponseDTO;
import com.workit.domain.merchant.dto.MerchantItemResponseDTO;
import com.workit.domain.merchant.dto.MerchantListResponseDTO;
import com.workit.domain.merchant.vo.MerchantSearchCondition;
import com.workit.domain.merchant.vo.MerchantSortType;
import com.workit.global.dto.CommonResponse;
import com.workit.global.response.GlobalResponseFactory;
import com.workit.security.CurrentUser;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
@RestController
@RequestMapping("/api/v1/merchants")
public class MerchantController {

    private final MerchantService merchantService;

    public MerchantController(
            MerchantService merchantService
    ) {
        this.merchantService = merchantService;
    }

    @GetMapping
    public ResponseEntity<CommonResponse<MerchantListResponseDTO<MerchantItemResponseDTO>>> findMerchants(
            @CurrentUser Long userId,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(value = "headcount", required = false) Integer headcount,
            @RequestParam(value = "roomCount", required = false) Integer roomCount,
            @RequestParam(value = "minPrice", required = false) Long minPrice,
            @RequestParam(value = "maxPrice", required = false) Long maxPrice,
            @RequestParam(value = "accommodationType", required = false) String accommodationType,
            @RequestParam(value = "noiseLevel", required = false) String noiseLevel,
            @RequestParam(value = "foodType", required = false) String foodType,
            @RequestParam(value = "priceLevel", required = false) Integer priceLevel,
            @RequestParam(value = "activityType", required = false) String activityType,
            @RequestParam(value = "sort", defaultValue = "RATING_DESC") String sort,
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "regionId", required = false) Long regionId
    ) {
        MerchantSearchCondition condition = MerchantSearchCondition.builder()
                .category(category)
                .startDate(startDate)
                .endDate(endDate)
                .headcount(headcount)
                .roomCount(roomCount)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .accommodationType(accommodationType)
                .noiseLevel(noiseLevel)
                .foodType(foodType)
                .priceLevel(priceLevel)
                .activityType(activityType)
                .sort(MerchantSortType.from(sort))
                .cursor(cursor)
                .size(size)
                .regionId(regionId)
                .build();

        return GlobalResponseFactory.success(merchantService.findMerchants(userId, condition));
    }

    @GetMapping("/{merchantId}/accommodations")
    public ResponseEntity<CommonResponse<MerchantDetailResponseDTO>> findAccommodationProducts(
            @CurrentUser Long userId,
            @PathVariable("merchantId") Long merchantId,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(value = "roomCount", required = false) Integer roomCount,
            @RequestParam(value = "guestCount", required = false) Integer guestCount
    ) {
        return GlobalResponseFactory.success(
                merchantService.findAccommodationProducts(
                        userId,
                        merchantId,
                        startDate,
                        endDate,
                        roomCount,
                        guestCount
                )
        );
    }

    @GetMapping("/{merchantId}/offices")
    public ResponseEntity<CommonResponse<MerchantDetailResponseDTO>> findOfficeProducts(
            @CurrentUser Long userId,
            @PathVariable("merchantId") Long merchantId,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(value = "guestCount", required = false) Integer guestCount
    ) {
        return GlobalResponseFactory.success(
                merchantService.findOfficeProducts(
                        userId,
                        merchantId,
                        startDate,
                        endDate,
                        guestCount
                )
        );
    }

    @GetMapping("/{merchantId}/restaurants")
    public ResponseEntity<CommonResponse<MerchantDetailCommonResponseDTO>> findRestaurantDetail(
            @CurrentUser Long userId,
            @PathVariable("merchantId") Long merchantId
    ) {
        return GlobalResponseFactory.success(merchantService.findRestaurantProducts(userId, merchantId));
    }


    @GetMapping("/{merchantId}/activities")
    public ResponseEntity<CommonResponse<MerchantDetailCommonResponseDTO>> findActivityDetail(
            @CurrentUser Long userId,
            @PathVariable("merchantId") Long merchantId
    ) {
        return GlobalResponseFactory.success(merchantService.findActivityProducts(userId, merchantId));
    }
}
