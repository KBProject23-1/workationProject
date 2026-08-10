package com.workit.domain.recommendation.common.mapper;

import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

public interface ReservationProductAvailabilityMapper {

    List<Long> selectAvailableMerchantIdsByPeriod(
            @Param("regionId") Long regionId,
            @Param("merchantCategory") String merchantCategory,
            @Param("productDetailType") String productDetailType,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("includeEndDate") boolean includeEndDate,
            @Param("requiredInventoryDateCount") Integer requiredInventoryDateCount
    );
}
