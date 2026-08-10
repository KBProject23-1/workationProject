package com.workit.domain.merchant.mapper;

import com.workit.domain.merchant.vo.MerchantDetailVO;
import com.workit.domain.merchant.vo.MerchantVO;
import com.workit.domain.merchant.vo.MerchantProductVO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

public interface MerchantMapper {

    List<MerchantVO> selectReservationMerchantsByCursor(
            @Param("category") String category,
            @Param("cursorValue") Object cursorValue,
            @Param("cursorMerchantId") Long cursorMerchantId,
            @Param("size") int size,
            @Param("regionId") Long regionId,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate,
            @Param("requiredDateCount") Integer requiredDateCount,
            @Param("headcount") Integer headcount,
            @Param("minPrice") Long minPrice,
            @Param("maxPrice") Long maxPrice,
            @Param("sort") String sort
    );

    MerchantDetailVO selectMerchantAccommodationDetails(@Param("merchantId") Long merchantId);

    List<MerchantProductVO> selectMerchantAccommodationReservationProducts(
            @Param("merchantId") Long merchantId
    );

    List<MerchantProductVO> selectMerchantAccommodationReservationProductsByPeriod(
            @Param("merchantId") Long merchantId,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate,
            @Param("headcount") Integer headcount,
            @Param("roomCount") Integer roomCount,
            @Param("requiredDateCount") Integer requiredDateCount,
            @Param("requiredQuantity") Integer requiredQuantity,
            @Param("includeEndDate") boolean includeEndDate
    );

    int selectMerchantAccommodationReviewCount(@Param("merchantId") Long merchantId);

    List<MerchantProductVO> selectMerchantOfficeProducts(
            @Param("merchantId") Long merchantId
    );

    MerchantDetailVO selectMerchantOfficeDetails(@Param("merchantId") Long merchantId);

    MerchantDetailVO selectMerchantRestaurantDetails(@Param("merchantId") Long merchantId);

    MerchantDetailVO selectMerchantActivityDetails(@Param("merchantId") Long merchantId);
}
