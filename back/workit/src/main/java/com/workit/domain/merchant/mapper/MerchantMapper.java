package com.workit.domain.merchant.mapper;

import com.workit.domain.merchant.vo.AccommodationListItemVO;
import com.workit.domain.merchant.vo.AccommodationDetailVO;
import com.workit.domain.merchant.vo.AccommodationType;
import com.workit.domain.merchant.vo.FoodType;
import com.workit.domain.merchant.vo.RestaurantListItemVO;
import com.workit.domain.merchant.vo.RestaurantDetailVO;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

public interface MerchantMapper {

    AccommodationDetailVO selectAccommodationDetails(@Param("merchantId") Long merchantId);

    List<String> selectMerchantTagNames(@Param("merchantId") Long merchantId);

    RestaurantDetailVO selectRestaurantDetails(@Param("merchantId") Long merchantId);

    List<RestaurantListItemVO> selectRestaurantList(
            @Param("regionId") Long regionId,
            @Param("foodType") FoodType foodType,
            @Param("minPrice") Long minPrice,
            @Param("maxPrice") Long maxPrice,
            @Param("minRating") BigDecimal minRating,
            @Param("sort") String sort,
            @Param("offset") int offset,
            @Param("size") int size
    );

    long countRestaurantList(
            @Param("regionId") Long regionId,
            @Param("foodType") FoodType foodType,
            @Param("minPrice") Long minPrice,
            @Param("maxPrice") Long maxPrice,
            @Param("minRating") BigDecimal minRating
    );

    // 지역과 조회 조건에 맞는 숙소 목록 한 페이지 조회
    List<AccommodationListItemVO> selectAccommodationList(
            @Param("regionId") Long regionId,
            @Param("accommodationType") AccommodationType accommodationType,
            @Param("minPrice") Long minPrice,
            @Param("maxPrice") Long maxPrice,
            @Param("minRating") BigDecimal minRating,
            @Param("sort") String sort,
            @Param("offset") int offset,
            @Param("size") int size
    );

    // 목록 조회와 같은 조건으로 전체 숙소 수 조회
    long countAccommodationList(
            @Param("regionId") Long regionId,
            @Param("accommodationType") AccommodationType accommodationType,
            @Param("minPrice") Long minPrice,
            @Param("maxPrice") Long maxPrice,
            @Param("minRating") BigDecimal minRating
    );
}
