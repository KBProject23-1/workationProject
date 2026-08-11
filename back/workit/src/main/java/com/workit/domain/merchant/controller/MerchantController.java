package com.workit.domain.merchant.controller;

import com.workit.domain.merchant.dto.MerchantDetailCommonResponseDTO;
import com.workit.domain.merchant.service.MerchantService;
import com.workit.domain.merchant.dto.MerchantDetailResponseDTO;
import com.workit.domain.merchant.dto.MerchantItemResponseDTO;
import com.workit.domain.merchant.dto.MerchantListResponseDTO;
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
import java.util.List;

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
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "checkInDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate checkInDate,
            @RequestParam(value = "checkOutDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate checkOutDate,
            @RequestParam(value = "headcount", required = false) Integer headcount,
            @RequestParam(value = "minPrice", required = false) Long minPrice,
            @RequestParam(value = "maxPrice", required = false) Long maxPrice,
            @RequestParam(value = "sort", defaultValue = "RATING_DESC") String sort,
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "regionId", required = false) Long regionId
    ) {
        return GlobalResponseFactory.success(
                merchantService.findMerchants(
                        category,
                        checkInDate,
                        checkOutDate,
                        headcount,
                        minPrice,
                        maxPrice,
                        MerchantSortType.from(sort),
                        cursor,
                        size,
                        regionId
                )
        );
    }

    @GetMapping("/{merchantId}/accommodations")
    public ResponseEntity<CommonResponse<MerchantDetailResponseDTO>> findAccommodationProducts(
            @PathVariable("merchantId") Long merchantId,
            @RequestParam(value = "checkInDate", required = false) LocalDate checkInDate,
            @RequestParam(value = "checkOutDate", required = false) LocalDate checkOutDate,
            @RequestParam(value = "roomCount", required = false) Integer roomCount,
            @RequestParam(value = "guestCount", required = false) Integer guestCount
    ) {
        return GlobalResponseFactory.success(
                merchantService.findAccommodationProducts(
                        merchantId,
                        checkInDate,
                        checkOutDate,
                        roomCount,
                        guestCount
                )
        );
    }

    @GetMapping("/{merchantId}/offices")
    public ResponseEntity<CommonResponse<MerchantDetailResponseDTO>> findOfficeProducts(
            @PathVariable("merchantId") Long merchantId
    ) {
        return GlobalResponseFactory.success(
                merchantService.findOfficeProducts(merchantId)
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
