package com.workit.domain.tourism.service;

import com.workit.domain.tourism.mapper.TourismMapper;
import com.workit.domain.tourism.vo.TourApiItem;
import com.workit.domain.tourism.vo.TourismCategory;
import com.workit.domain.tourism.vo.TourismMerchantVO;
import com.workit.domain.tourism.vo.TourismAccommodationType;
import com.workit.domain.tourism.vo.TourismPlaceType;
import com.workit.domain.tourism.vo.TourismRestaurantFoodType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TourismMerchantPersistenceServiceTest {
    @Mock TourismMapper mapper;
    private TourismMerchantPersistenceService service;

    @BeforeEach
    void setUp() {
        service = new TourismMerchantPersistenceService(mapper);
    }

    @Test
    void insertsMerchantAndSourceWhenContentIsNew() {
        TourApiItem item = activeItem();
        when(mapper.selectMerchantIdByContentId("100")).thenReturn(null);
        doAnswer(invocation -> {
            ((TourismMerchantVO) invocation.getArgument(0)).setMerchantId(501L);
            return null;
        }).when(mapper).insertMerchant(any(TourismMerchantVO.class));

        assertTrue(service.save(item, 1L));

        ArgumentCaptor<TourismMerchantVO> captor =
                ArgumentCaptor.forClass(TourismMerchantVO.class);
        verify(mapper).insertMerchant(captor.capture());
        assertEquals(1L, captor.getValue().getRegionId());
        assertEquals("부산광역시 수영구 광안동", captor.getValue().getAddress());
        verify(mapper).upsertTourismSource(
                501L, "100", "20260818120000", 10L, null);
        verify(mapper).upsertActivity(501L, TourismCategory.WATER_SPORTS, null);
    }

    @Test
    void updatesExistingMerchantAndReactivatesSource() {
        TourApiItem item = activeItem();
        when(mapper.selectMerchantIdByContentId("100")).thenReturn(501L);
        when(mapper.updateMerchant(any(TourismMerchantVO.class))).thenReturn(1);

        assertTrue(service.save(item, 1L));

        verify(mapper, never()).insertMerchant(any(TourismMerchantVO.class));
        verify(mapper).updateMerchant(any(TourismMerchantVO.class));
        verify(mapper).upsertTourismSource(
                501L, "100", "20260818120000", 10L, null);
    }

    @Test
    void skipsItemWithoutCoordinatesBecauseMerchantColumnsAreNotNull() {
        TourApiItem item = activeItem();
        item.setLatitude(null);

        assertFalse(service.save(item, 1L));

        verifyNoInteractions(mapper);
    }

    @Test
    void deactivatesOnlySourceMetadataForDeletedTourApiItem() {
        TourApiItem item = activeItem();
        item.setActive(false);
        when(mapper.deactivateMerchant("100")).thenReturn(1);

        assertTrue(service.save(item, 1L));

        verify(mapper).deactivateMerchant("100");
        verify(mapper, never()).updateMerchant(any(TourismMerchantVO.class));
    }

    @Test
    void storesJejuRestaurantWithClassificationAndDetailDescription() {
        TourApiItem item = activeItem();
        item.setPlaceType(TourismPlaceType.RESTAURANT);
        item.setCategory(null);
        item.setClassification2("FD02");
        item.setClassification3("FD020200");
        item.setDescription("[대표메뉴]\n고등어 초밥");
        item.setPhoneNumber("064-123-4567");
        when(mapper.selectMerchantIdByContentId("100")).thenReturn(501L);
        when(mapper.updateMerchant(any(TourismMerchantVO.class))).thenReturn(1);

        assertTrue(service.save(item, 4L));

        ArgumentCaptor<TourismMerchantVO> merchantCaptor =
                ArgumentCaptor.forClass(TourismMerchantVO.class);
        verify(mapper).updateMerchant(merchantCaptor.capture());
        assertEquals(TourismPlaceType.RESTAURANT, merchantCaptor.getValue().getCategory());
        assertEquals("064-123-4567", merchantCaptor.getValue().getPhoneNumber());
        verify(mapper).upsertRestaurant(501L, TourismRestaurantFoodType.JAPANESE,
                2, "[대표메뉴]\n고등어 초밥");
    }

    @Test
    void storesJejuAccommodationWithCheckTimes() {
        TourApiItem item = activeItem();
        item.setPlaceType(TourismPlaceType.ACCOMMODATION);
        item.setCategory(null);
        item.setTitle("제주 바다 풀빌라");
        item.setClassification2("AC03");
        item.setDescription("[소개]\n바다 전망 숙소");
        item.setCheckInTime("15:00:00");
        item.setCheckOutTime("11:00:00");
        when(mapper.selectMerchantIdByContentId("100")).thenReturn(501L);
        when(mapper.updateMerchant(any(TourismMerchantVO.class))).thenReturn(1);

        assertTrue(service.save(item, 4L));

        verify(mapper).upsertAccommodation(501L, TourismAccommodationType.POOL_VILLA,
                "[소개]\n바다 전망 숙소", "15:00:00", "11:00:00");
    }

    private TourApiItem activeItem() {
        TourApiItem item = new TourApiItem();
        item.setContentId("100");
        item.setTitle("광안리 수상레저");
        item.setAddress1("부산광역시 수영구");
        item.setAddress2("광안동");
        item.setLatitude(new BigDecimal("35.1"));
        item.setLongitude(new BigDecimal("129.1"));
        item.setModifiedTime("20260818120000");
        item.setSyncId(10L);
        item.setCategory(TourismCategory.WATER_SPORTS);
        item.setActive(true);
        return item;
    }
}
