package com.workit.domain.tourism.service;

import com.workit.domain.tourism.client.TourApiClient;
import com.workit.domain.tourism.mapper.TourismMapper;
import com.workit.domain.tourism.vo.TourApiItem;
import com.workit.domain.tourism.vo.TourApiDetail;
import com.workit.domain.tourism.vo.TourApiPage;
import com.workit.domain.tourism.vo.TourismCategory;
import com.workit.domain.tourism.vo.TourismPlaceType;
import com.workit.domain.tourism.vo.TourismRegionVO;
import com.workit.domain.tourism.vo.TourismSyncMode;
import com.workit.domain.tourism.vo.TourismSyncRunVO;
import com.workit.domain.tourism.vo.TourismTargetRegion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TourismSyncServiceTest {
    @Mock TourApiClient client;
    @Mock TourismMapper mapper;
    @Mock TourismMerchantPersistenceService persistenceService;
    private TourismSyncService service;

    @BeforeEach
    void setUp() {
        service = new TourismSyncService(client, mapper, persistenceService);
        when(client.isConfigured()).thenReturn(true);
        doAnswer(invocation -> {
            ((TourismSyncRunVO) invocation.getArgument(0)).setId(10L);
            return null;
        }).when(mapper).insertSyncRun(any(TourismSyncRunVO.class));
    }

    @Test
    void fullSyncStoresJejuMerchantAndActivity() {
        when(mapper.selectRegions()).thenReturn(targetRegions());
        when(client.fetch(any(TourismCategory.class), any(TourismTargetRegion.class),
                eq(1), eq(true), isNull(), eq(10L)))
                .thenAnswer(invocation -> {
                    TourismCategory category = invocation.getArgument(0);
                    TourismTargetRegion targetRegion = invocation.getArgument(1);
                    if (category != TourismCategory.WATER_SPORTS
                            || targetRegion != TourismTargetRegion.JEJU) {
                        return new TourApiPage(Collections.emptyList(), 100, 0);
                    }
                    TourApiItem item = new TourApiItem();
                    item.setContentId("100");
                    item.setTitle("제주 수상레저");
                    item.setAddress1("제주특별자치도 제주시");
                    item.setLatitude(new BigDecimal("33.4"));
                    item.setLongitude(new BigDecimal("126.5"));
                    item.setCategory(category);
                    item.setActive(true);
                    item.setSyncId(10L);
                    item.setModifiedTime("20260818120000");
                    return new TourApiPage(Collections.singletonList(item), 100, 1);
                });
        when(client.fetch(any(TourismPlaceType.class), eq(TourismTargetRegion.JEJU),
                eq(1), eq(true), isNull(), eq(10L)))
                .thenReturn(new TourApiPage(Collections.emptyList(), 100, 0));
        when(mapper.shouldFetchDescription("100", "20260818120000")).thenReturn(true);
        when(client.fetchDetail("100", 28))
                .thenReturn(new TourApiDetail("[소개]\n상세 설명", "064-123-4567",
                        null, null));
        when(persistenceService.save(any(TourApiItem.class), eq(4L))).thenReturn(true);
        TourismSyncRunVO completed = new TourismSyncRunVO();
        completed.setId(10L);
        completed.setStatus("SUCCESS");
        when(mapper.selectSyncRun(10L)).thenReturn(completed);

        service.sync(TourismSyncMode.FULL);

        ArgumentCaptor<TourApiItem> itemCaptor = ArgumentCaptor.forClass(TourApiItem.class);
        verify(persistenceService).save(itemCaptor.capture(), eq(4L));
        assertEquals("[소개]\n상세 설명", itemCaptor.getValue().getDescription());
        assertEquals("064-123-4567", itemCaptor.getValue().getPhoneNumber());
        verify(client, never()).fetch(any(TourismCategory.class),
                eq(TourismTargetRegion.BUSAN), anyInt(), eq(true), isNull(), eq(10L));
    }

    @Test
    void fullSyncStopsAfterShortLastPage() {
        when(mapper.selectRegions()).thenReturn(targetRegions());
        when(client.fetch(any(TourismCategory.class), any(TourismTargetRegion.class),
                anyInt(), eq(true), isNull(), eq(10L)))
                .thenAnswer(invocation -> {
                    TourismCategory category = invocation.getArgument(0);
                    TourismTargetRegion region = invocation.getArgument(1);
                    int pageNo = invocation.getArgument(2);
                    if (category != TourismCategory.CAFE_TEA_HOUSE
                            || region != TourismTargetRegion.JEJU) {
                        return new TourApiPage(Collections.emptyList(), 0, 0);
                    }
                    TourApiItem item = new TourApiItem();
                    item.setContentId("cafe-" + pageNo);
                    item.setTitle("제주 카페 " + pageNo);
                    item.setCategory(category);
                    item.setActive(true);
                    item.setSyncId(10L);
                    if (pageNo == 1) {
                        return new TourApiPage(Collections.singletonList(item), 100, 121);
                    }
                    return new TourApiPage(Collections.singletonList(item), 21, 121);
                });
        when(client.fetch(any(TourismPlaceType.class), eq(TourismTargetRegion.JEJU),
                eq(1), eq(true), isNull(), eq(10L)))
                .thenReturn(new TourApiPage(Collections.emptyList(), 100, 0));
        when(persistenceService.save(any(TourApiItem.class), eq(4L))).thenReturn(true);
        TourismSyncRunVO completed = new TourismSyncRunVO();
        completed.setId(10L);
        completed.setStatus("SUCCESS");
        when(mapper.selectSyncRun(10L)).thenReturn(completed);

        service.sync(TourismSyncMode.FULL);

        verify(client, never()).fetch(TourismCategory.CAFE_TEA_HOUSE,
                TourismTargetRegion.JEJU, 3, true, null, 10L);
    }

    @Test
    void fullSyncCollectsJejuRestaurantAndAppliesDetailPhone() {
        when(mapper.selectRegions()).thenReturn(targetRegions());
        when(client.fetch(any(TourismCategory.class), any(TourismTargetRegion.class),
                eq(1), eq(true), isNull(), eq(10L)))
                .thenReturn(new TourApiPage(Collections.emptyList(), 100, 0));
        when(client.fetch(any(TourismPlaceType.class), eq(TourismTargetRegion.JEJU),
                eq(1), eq(true), isNull(), eq(10L)))
                .thenAnswer(invocation -> {
                    TourismPlaceType placeType = invocation.getArgument(0);
                    if (placeType != TourismPlaceType.RESTAURANT) {
                        return new TourApiPage(Collections.emptyList(), 100, 0);
                    }
                    TourApiItem item = new TourApiItem();
                    item.setContentId("restaurant-1");
                    item.setTitle("제주 식당");
                    item.setAddress1("제주특별자치도 제주시");
                    item.setLatitude(new BigDecimal("33.4"));
                    item.setLongitude(new BigDecimal("126.5"));
                    item.setPlaceType(placeType);
                    item.setContentTypeId(39);
                    item.setActive(true);
                    item.setSyncId(10L);
                    item.setModifiedTime("20260818120000");
                    return new TourApiPage(Collections.singletonList(item), 100, 1);
                });
        when(mapper.shouldFetchDescription("restaurant-1", "20260818120000"))
                .thenReturn(true);
        when(client.fetchDetail("restaurant-1", 39))
                .thenReturn(new TourApiDetail("[대표메뉴]\n갈치조림",
                        "064-123-4567", null, null));
        when(persistenceService.save(any(TourApiItem.class), eq(4L))).thenReturn(true);
        TourismSyncRunVO completed = new TourismSyncRunVO();
        completed.setId(10L);
        completed.setStatus("SUCCESS");
        when(mapper.selectSyncRun(10L)).thenReturn(completed);

        service.sync(TourismSyncMode.FULL);

        ArgumentCaptor<TourApiItem> itemCaptor = ArgumentCaptor.forClass(TourApiItem.class);
        verify(persistenceService).save(itemCaptor.capture(), eq(4L));
        assertEquals(TourismPlaceType.RESTAURANT, itemCaptor.getValue().getPlaceType());
        assertEquals("064-123-4567", itemCaptor.getValue().getPhoneNumber());
        assertEquals("[대표메뉴]\n갈치조림", itemCaptor.getValue().getDescription());
    }

    @Test
    void restaurantSyncRequestsOnlyRestaurants() {
        when(mapper.selectRegions()).thenReturn(targetRegions());
        when(client.fetch(TourismPlaceType.RESTAURANT, TourismTargetRegion.JEJU,
                1, true, null, 10L))
                .thenReturn(new TourApiPage(Collections.emptyList(), 100, 0));
        TourismSyncRunVO completed = completedRun();
        when(mapper.selectSyncRun(10L)).thenReturn(completed);

        service.sync(TourismSyncMode.FULL, TourismPlaceType.RESTAURANT);

        verify(client).fetch(TourismPlaceType.RESTAURANT, TourismTargetRegion.JEJU,
                1, true, null, 10L);
        verify(client, never()).fetch(any(TourismCategory.class),
                any(TourismTargetRegion.class), anyInt(), eq(true), isNull(), eq(10L));
        verify(client, never()).fetch(eq(TourismPlaceType.ACCOMMODATION),
                any(TourismTargetRegion.class), anyInt(), eq(true), isNull(), eq(10L));
    }

    @Test
    void accommodationSyncRequestsOnlyAccommodations() {
        when(mapper.selectRegions()).thenReturn(targetRegions());
        when(client.fetch(TourismPlaceType.ACCOMMODATION, TourismTargetRegion.JEJU,
                1, true, null, 10L))
                .thenReturn(new TourApiPage(Collections.emptyList(), 100, 0));
        TourismSyncRunVO completed = completedRun();
        when(mapper.selectSyncRun(10L)).thenReturn(completed);

        service.sync(TourismSyncMode.FULL, TourismPlaceType.ACCOMMODATION);

        verify(client).fetch(TourismPlaceType.ACCOMMODATION, TourismTargetRegion.JEJU,
                1, true, null, 10L);
        verify(client, never()).fetch(any(TourismCategory.class),
                any(TourismTargetRegion.class), anyInt(), eq(true), isNull(), eq(10L));
        verify(client, never()).fetch(eq(TourismPlaceType.RESTAURANT),
                any(TourismTargetRegion.class), anyInt(), eq(true), isNull(), eq(10L));
    }

    @Test
    void activitySyncRequestsOnlyActivityCategories() {
        when(mapper.selectRegions()).thenReturn(targetRegions());
        when(client.fetch(any(TourismCategory.class), eq(TourismTargetRegion.JEJU),
                eq(1), eq(true), isNull(), eq(10L)))
                .thenReturn(new TourApiPage(Collections.emptyList(), 100, 0));
        TourismSyncRunVO completed = completedRun();
        when(mapper.selectSyncRun(10L)).thenReturn(completed);

        service.sync(TourismSyncMode.FULL, TourismPlaceType.ACTIVITY);

        verify(client).fetch(TourismCategory.WATER_SPORTS, TourismTargetRegion.JEJU,
                1, true, null, 10L);
        verify(client, never()).fetch(any(TourismPlaceType.class),
                any(TourismTargetRegion.class), anyInt(), eq(true), isNull(), eq(10L));
    }

    private TourismSyncRunVO completedRun() {
        TourismSyncRunVO completed = new TourismSyncRunVO();
        completed.setId(10L);
        completed.setStatus("SUCCESS");
        return completed;
    }

    private List<TourismRegionVO> targetRegions() {
        TourismRegionVO busan = region(1L, "부산");
        TourismRegionVO gangneung = region(2L, "강릉");
        TourismRegionVO yeosu = region(3L, "여수");
        TourismRegionVO jeju = region(4L, "제주");
        return Arrays.asList(busan, gangneung, yeosu, jeju);
    }

    private TourismRegionVO region(Long id, String name) {
        TourismRegionVO region = new TourismRegionVO();
        region.setId(id);
        region.setName(name);
        return region;
    }
}
