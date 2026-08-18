package com.workit.domain.tourism.service;

import com.workit.domain.tourism.mapper.TourismMapper;
import com.workit.domain.tourism.vo.TourApiItem;
import com.workit.domain.tourism.vo.TourismMerchantVO;
import com.workit.domain.tourism.vo.TourismAccommodationType;
import com.workit.domain.tourism.vo.TourismPlaceType;
import com.workit.domain.tourism.vo.TourismRestaurantFoodType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class TourismMerchantPersistenceService {
    private final TourismMapper tourismMapper;

    @Transactional
    public boolean save(TourApiItem item, Long regionId) {
        if (!item.isActive()) {
            return tourismMapper.deactivateMerchant(item.getContentId()) > 0;
        }

        String address = join(item.getAddress1(), item.getAddress2());
        if (!StringUtils.hasText(address)
                || item.getLatitude() == null
                || item.getLongitude() == null) {
            return false;
        }

        TourismMerchantVO merchant = toMerchant(item, regionId, address);
        Long merchantId = tourismMapper.selectMerchantIdByContentId(item.getContentId());
        if (merchantId == null) {
            tourismMapper.insertMerchant(merchant);
            merchantId = merchant.getMerchantId();
            if (merchantId == null) {
                throw new IllegalStateException("merchant ID 생성 실패: " + item.getContentId());
            }
        } else {
            merchant.setMerchantId(merchantId);
            if (tourismMapper.updateMerchant(merchant) != 1) {
                throw new IllegalStateException("merchant 갱신 실패: " + item.getContentId());
            }
        }

        tourismMapper.upsertTourismSource(merchantId, item.getContentId(),
                item.getModifiedTime(), item.getSyncId(),
                item.isDetailFetched() ? item.getModifiedTime() : null);
        saveDetails(merchantId, item);
        return true;
    }

    private void saveDetails(Long merchantId, TourApiItem item) {
        if (item.getPlaceType() == TourismPlaceType.RESTAURANT) {
            tourismMapper.deleteActivity(merchantId);
            tourismMapper.deleteAccommodation(merchantId);
            tourismMapper.upsertRestaurant(merchantId,
                    TourismRestaurantFoodType.resolve(item.getClassification2(),
                            item.getClassification3(), item.getTitle()),
                    2, item.getDescription());
            return;
        }
        if (item.getPlaceType() == TourismPlaceType.ACCOMMODATION) {
            tourismMapper.deleteActivity(merchantId);
            tourismMapper.deleteRestaurant(merchantId);
            tourismMapper.upsertAccommodation(merchantId,
                    TourismAccommodationType.resolve(item.getClassification2(),
                            item.getClassification3(), item.getTitle()),
                    item.getDescription(), item.getCheckInTime(), item.getCheckOutTime());
            return;
        }
        tourismMapper.deleteRestaurant(merchantId);
        tourismMapper.deleteAccommodation(merchantId);
        tourismMapper.upsertActivity(merchantId, item.getCategory(), item.getDescription());
    }

    private TourismMerchantVO toMerchant(TourApiItem item, Long regionId, String address) {
        TourismMerchantVO merchant = new TourismMerchantVO();
        merchant.setRegionId(regionId);
        merchant.setName(item.getTitle());
        merchant.setAddress(address);
        merchant.setLatitude(item.getLatitude());
        merchant.setLongitude(item.getLongitude());
        merchant.setPhoneNumber(item.getPhoneNumber());
        merchant.setThumbnailUrl(item.getThumbnailUrl());
        merchant.setCategory(item.getPlaceType() == null
                ? TourismPlaceType.ACTIVITY : item.getPlaceType());
        return merchant;
    }

    private String join(String first, String second) {
        String a = StringUtils.hasText(first) ? first.trim() : "";
        String b = StringUtils.hasText(second) ? second.trim() : "";
        return a.isEmpty() ? b : b.isEmpty() ? a : a + " " + b;
    }
}
