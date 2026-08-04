package com.workit.domain.merchant.offices.service;

import com.workit.domain.merchant.offices.dto.response.MerchantOfficeListResponseDTO;
import com.workit.domain.merchant.offices.dto.response.MerchantOfficeDetailResponseDTO;
import com.workit.domain.merchant.offices.enums.MerchantOfficeSortType;
import java.time.LocalDate;

public interface OfficesService {

    MerchantOfficeListResponseDTO findOfficeList(
            String cursor,
            int size,
            Long regionId,
            LocalDate startDate,
            LocalDate endDate,
            MerchantOfficeSortType sort
    );

    MerchantOfficeDetailResponseDTO findOfficeDetail(Long merchantId);
}
