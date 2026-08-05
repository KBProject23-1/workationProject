package com.workit.domain.merchant.offices.mapper;

import com.workit.domain.merchant.offices.vo.MerchantOfficeItemVO;
import com.workit.domain.merchant.offices.vo.MerchantOfficeDetailTagVO;
import com.workit.domain.merchant.offices.vo.MerchantOfficeDetailVO;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface OfficesMapper {

    List<MerchantOfficeItemVO> selectOfficeListByCursor(
            @Param("cursorRating") BigDecimal cursorRating,
            @Param("cursorMerchantId") Long cursorMerchantId,
            @Param("cursorPrice") Long cursorPrice,
            @Param("size") int size,
            @Param("regionId") Long regionId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("sort") String sort
    );

    MerchantOfficeDetailVO selectOfficeDetailById(
            @Param("merchantId") Long merchantId
    );

    List<MerchantOfficeDetailTagVO> selectOfficeTagsByMerchantId(
            @Param("merchantId") Long merchantId
    );
}
