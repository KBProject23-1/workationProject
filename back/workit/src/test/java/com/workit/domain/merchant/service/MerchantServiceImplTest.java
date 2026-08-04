package com.workit.domain.merchant.service;

import com.workit.domain.merchant.dto.request.AccommodationListRequestDTO;
import com.workit.domain.merchant.dto.request.RestaurantListRequestDTO;
import com.workit.domain.merchant.dto.response.AccommodationListResponseDTO;
import com.workit.domain.merchant.dto.response.AccommodationDetailResponseDTO;
import com.workit.domain.merchant.dto.response.RestaurantListResponseDTO;
import com.workit.domain.merchant.dto.response.RestaurantDetailResponseDTO;
import com.workit.domain.merchant.exception.MerchantErrorCode;
import com.workit.domain.merchant.mapper.MerchantMapper;
import com.workit.domain.merchant.vo.AccommodationDetailVO;
import com.workit.domain.merchant.vo.AccommodationListItemVO;
import com.workit.domain.merchant.vo.AccommodationType;
import com.workit.domain.merchant.vo.FoodType;
import com.workit.domain.merchant.vo.RestaurantListItemVO;
import com.workit.domain.merchant.vo.RestaurantDetailVO;
import com.workit.global.dto.PageResponseDTO;
import com.workit.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MerchantServiceImplTest {

    @Test
    void 음식점상세를조회한다() {
        RestaurantDetailVO restaurant = new RestaurantDetailVO();
        restaurant.setMerchantId(20L);
        restaurant.setName("테스트 한식당");
        restaurant.setFoodType(FoodType.KOREAN);

        MerchantService service = new MerchantServiceImpl(
                new StubMerchantMapper(
                        Collections.emptyList(), 0L, null,
                        Collections.emptyList(), 0L, restaurant)
        );

        RestaurantDetailResponseDTO response = service.findRestaurantDetails(20L);

        assertEquals("테스트 한식당", response.getName());
        assertEquals(Collections.singletonList("오션뷰"), response.getTags());
    }

    @Test
    void 존재하지않는음식점은조회할수없다() {
        MerchantService service = new MerchantServiceImpl(
                new StubMerchantMapper(
                        Collections.emptyList(), 0L, null,
                        Collections.emptyList(), 0L, null)
        );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.findRestaurantDetails(999L)
        );

        assertEquals(MerchantErrorCode.RESTAURANT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void 지역과음식종류로음식점목록을조회한다() {
        RestaurantListItemVO restaurant = new RestaurantListItemVO();
        restaurant.setMerchantId(20L);
        restaurant.setName("테스트 한식당");
        restaurant.setFoodType(FoodType.KOREAN);
        restaurant.setPrice(15000L);

        MerchantService service = new MerchantServiceImpl(
                new StubMerchantMapper(
                        Collections.emptyList(),
                        0L,
                        null,
                        Collections.singletonList(restaurant),
                        1L,
                        null
                )
        );
        RestaurantListRequestDTO request = new RestaurantListRequestDTO();
        request.setRegionId(1L);
        request.setFoodType(FoodType.KOREAN);

        PageResponseDTO<RestaurantListResponseDTO> response = service.findRestaurantList(request);

        assertEquals(1L, response.getTotalElements());
        assertEquals("테스트 한식당", response.getContent().get(0).getName());
    }

    @Test
    void 숙소상세를조회한다() {
        AccommodationDetailVO accommodation = new AccommodationDetailVO();
        accommodation.setMerchantId(10L);
        accommodation.setName("테스트 호텔");
        accommodation.setAccommodationType(AccommodationType.HOTEL);

        MerchantService service = new MerchantServiceImpl(
                new StubMerchantMapper(
                        Collections.emptyList(), 0L, accommodation,
                        Collections.emptyList(), 0L, null)
        );

        AccommodationDetailResponseDTO response = service.findAccommodationDetails(10L);

        assertEquals("테스트 호텔", response.getName());
        assertEquals(Collections.singletonList("오션뷰"), response.getTags());
    }

    @Test
    void 존재하지않는숙소는조회할수없다() {
        MerchantService service = new MerchantServiceImpl(
                new StubMerchantMapper(
                        Collections.emptyList(), 0L, null,
                        Collections.emptyList(), 0L, null)
        );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.findAccommodationDetails(999L)
        );

        assertEquals(MerchantErrorCode.ACCOMMODATION_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void 지역조건으로숙소목록을조회한다() {
        AccommodationListItemVO accommodation = new AccommodationListItemVO();
        accommodation.setMerchantId(10L);
        accommodation.setName("테스트 호텔");
        accommodation.setAccommodationType(AccommodationType.HOTEL);
        accommodation.setRating(new BigDecimal("4.5"));
        accommodation.setPrice(120000L);

        MerchantService service = new MerchantServiceImpl(
                new StubMerchantMapper(
                        Collections.singletonList(accommodation), 1L, null,
                        Collections.emptyList(), 0L, null)
        );
        AccommodationListRequestDTO request = new AccommodationListRequestDTO();
        request.setRegionId(1L);

        PageResponseDTO<AccommodationListResponseDTO> response =
                service.findAccommodationList(request);

        assertEquals(1L, response.getTotalElements());
        assertEquals(1, response.getContent().size());
        assertEquals("테스트 호텔", response.getContent().get(0).getName());
    }

    @Test
    void 지역번호가없으면조회할수없다() {
        MerchantService service = new MerchantServiceImpl(
                new StubMerchantMapper(
                        Collections.emptyList(), 0L, null,
                        Collections.emptyList(), 0L, null)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.findAccommodationList(new AccommodationListRequestDTO())
        );
    }

    @Test
    void 최소가격이최대가격보다크면조회할수없다() {
        MerchantService service = new MerchantServiceImpl(
                new StubMerchantMapper(
                        Collections.emptyList(), 0L, null,
                        Collections.emptyList(), 0L, null)
        );
        AccommodationListRequestDTO request = new AccommodationListRequestDTO();
        request.setRegionId(1L);
        request.setMinPrice(200000L);
        request.setMaxPrice(100000L);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.findAccommodationList(request)
        );
    }

    private static class StubMerchantMapper implements MerchantMapper {

        private final List<AccommodationListItemVO> accommodations;
        private final long count;
        private final AccommodationDetailVO accommodationDetail;
        private final List<RestaurantListItemVO> restaurants;
        private final long restaurantCount;
        private final RestaurantDetailVO restaurantDetail;

        private StubMerchantMapper(
                List<AccommodationListItemVO> accommodations,
                long count,
                AccommodationDetailVO accommodationDetail,
                List<RestaurantListItemVO> restaurants,
                long restaurantCount,
                RestaurantDetailVO restaurantDetail) {
            this.accommodations = accommodations;
            this.count = count;
            this.accommodationDetail = accommodationDetail;
            this.restaurants = restaurants;
            this.restaurantCount = restaurantCount;
            this.restaurantDetail = restaurantDetail;
        }

        @Override
        public RestaurantDetailVO selectRestaurantDetails(Long merchantId) {
            return restaurantDetail;
        }

        @Override
        public List<RestaurantListItemVO> selectRestaurantList(
                Long regionId,
                FoodType foodType,
                Long minPrice,
                Long maxPrice,
                BigDecimal minRating,
                String sort,
                int offset,
                int size) {
            return restaurants;
        }

        @Override
        public long countRestaurantList(
                Long regionId,
                FoodType foodType,
                Long minPrice,
                Long maxPrice,
                BigDecimal minRating) {
            return restaurantCount;
        }

        @Override
        public AccommodationDetailVO selectAccommodationDetails(Long merchantId) {
            return accommodationDetail;
        }

        @Override
        public List<String> selectMerchantTagNames(Long merchantId) {
            return Collections.singletonList("오션뷰");
        }

        @Override
        public List<AccommodationListItemVO> selectAccommodationList(
                Long regionId,
                AccommodationType accommodationType,
                Long minPrice,
                Long maxPrice,
                BigDecimal minRating,
                String sort,
                int offset,
                int size) {
            return accommodations;
        }

        @Override
        public long countAccommodationList(
                Long regionId,
                AccommodationType accommodationType,
                Long minPrice,
                Long maxPrice,
                BigDecimal minRating) {
            return count;
        }
    }
}
