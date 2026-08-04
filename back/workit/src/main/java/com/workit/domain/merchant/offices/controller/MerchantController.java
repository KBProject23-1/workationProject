package com.workit.domain.merchant.offices.controller;

import com.workit.domain.merchant.offices.dto.response.MerchantOfficeListResponseDTO;
import com.workit.domain.merchant.offices.dto.response.MerchantOfficeDetailResponseDTO;
import com.workit.domain.merchant.offices.enums.MerchantOfficeSortType;
import com.workit.domain.merchant.offices.service.MerchantService;
import com.workit.global.dto.CommonResponse;
import com.workit.global.response.GlobalResponseFactory;
import com.workit.domain.workation.dto.response.WorkationCurrentResponseDTO;
import com.workit.domain.workation.service.WorkationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/merchants")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;
    private final WorkationService workationService;

    @GetMapping("/offices")
    public ResponseEntity<CommonResponse<MerchantOfficeListResponseDTO>> officeList(
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sort", defaultValue = "RATING_HIGH") String sort) {

        // JWT 토큰에서 userId 추출로 교체 필요(추후 삭제)
        Long userId = 9001L;
        WorkationCurrentResponseDTO current = workationService.getCurrentWorkation(userId);
        WorkationCurrentResponseDTO.WorkationSummary summary =
                current == null ? null : current.getWorkation();

        LocalDate regionStartDate = null;
        LocalDate regionEndDate = null;
        Long regionId = null;

        if (summary != null) {
            regionId = summary.getRegion() == null ? null : summary.getRegion().getId();
            regionStartDate = summary.getStartDate();
            regionEndDate = summary.getEndDate();
        }

        return GlobalResponseFactory.success(merchantService.findOfficeList(         
                        cursor,
                        size,
                        regionId,
                        regionStartDate,
                        regionEndDate,
                        MerchantOfficeSortType.from(sort)
                ));
    }

    @GetMapping("/offices/{merchantId}")
    public ResponseEntity<CommonResponse<MerchantOfficeDetailResponseDTO>> officeDetails(
            @PathVariable("merchantId") Long merchantId) {

        return GlobalResponseFactory.success(merchantService.findOfficeDetail(merchantId));
    }
}
