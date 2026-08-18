package com.workit.domain.tourism.mapper;

import com.workit.domain.tourism.vo.TourismCategory;
import com.workit.domain.tourism.vo.TourismMerchantVO;
import com.workit.domain.tourism.vo.TourismAccommodationType;
import com.workit.domain.tourism.vo.TourismPlaceType;
import com.workit.domain.tourism.vo.TourismRestaurantFoodType;
import com.workit.domain.tourism.vo.TourismRegionVO;
import com.workit.domain.tourism.vo.TourismSyncRunVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TourismMapper {
    List<TourismRegionVO> selectRegions();
    Long selectMerchantIdByContentId(@Param("contentId") String contentId);
    boolean shouldFetchDescription(@Param("contentId") String contentId,
                                   @Param("modifiedTime") String modifiedTime);
    void insertMerchant(TourismMerchantVO merchant);
    int updateMerchant(TourismMerchantVO merchant);
    void upsertTourismSource(@Param("merchantId") Long merchantId,
                             @Param("contentId") String contentId,
                             @Param("modifiedTime") String modifiedTime,
                             @Param("syncId") Long syncId,
                             @Param("detailModifiedTime") String detailModifiedTime);
    void upsertActivity(@Param("merchantId") Long merchantId,
                        @Param("activityType") TourismCategory activityType,
                        @Param("description") String description);
    void upsertRestaurant(@Param("merchantId") Long merchantId,
                          @Param("foodType") TourismRestaurantFoodType foodType,
                          @Param("priceLevel") int priceLevel,
                          @Param("description") String description);
    void upsertAccommodation(@Param("merchantId") Long merchantId,
                             @Param("accommodationType") TourismAccommodationType accommodationType,
                             @Param("description") String description,
                             @Param("checkInTime") String checkInTime,
                             @Param("checkOutTime") String checkOutTime);
    int deleteActivity(@Param("merchantId") Long merchantId);
    int deleteRestaurant(@Param("merchantId") Long merchantId);
    int deleteAccommodation(@Param("merchantId") Long merchantId);
    int deactivateMerchant(@Param("contentId") String contentId);
    int deactivateMissing(@Param("activityType") TourismCategory activityType,
                          @Param("syncId") Long syncId);
    int deactivateMissingPlaceType(@Param("placeType") TourismPlaceType placeType,
                                   @Param("syncId") Long syncId);
    void insertSyncRun(TourismSyncRunVO run);
    void completeSyncRun(@Param("id") Long id,
                         @Param("status") String status,
                         @Param("processedCount") int processedCount,
                         @Param("deactivatedCount") int deactivatedCount,
                         @Param("errorMessage") String errorMessage);
    TourismSyncRunVO selectSyncRun(@Param("id") Long id);
    TourismSyncRunVO selectLatestSyncRun();
}
