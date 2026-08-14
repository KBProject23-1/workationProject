package com.workit.domain.merchant.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workit.domain.merchant.mapper.MerchantMapper;
import com.workit.domain.merchant.dto.MerchantItemResponseDTO;
import com.workit.domain.merchant.dto.MerchantListResponseDTO;
import com.workit.domain.merchant.dto.MerchantDetailResponseDTO;
import com.workit.domain.merchant.dto.MerchantDetailCommonResponseDTO;
import com.workit.domain.merchant.dto.MerchantDetailReviewResponseDTO;
import com.workit.domain.merchant.vo.MerchantErrorCode;
import com.workit.domain.merchant.vo.MerchantVO;
import com.workit.domain.merchant.vo.MerchantSearchCondition;
import com.workit.domain.merchant.vo.MerchantSortType;
import com.workit.domain.merchant.vo.MerchantDetailVO;
import com.workit.domain.merchant.vo.MerchantProductVO;
import com.workit.domain.review.mapper.ReviewMapper;
import com.workit.domain.review.vo.MerchantReviewVO;
import com.workit.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.exceptions.PersistenceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MerchantServiceImpl implements MerchantService {

    private static final int MAX_SIZE = 50;
    private static final int DETAIL_REVIEW_PREVIEW_SIZE = 5;
    private static final String CURSOR_SORT_KEY = "sort";
    private static final String CURSOR_MERCHANT_ID_KEY = "merchantId";
    private static final String CURSOR_RATING_KEY = "rating";
    private static final String CURSOR_PRICE_KEY = "price";
    private static final ObjectMapper RESERVATION_MERCHANT_CURSOR_OBJECT_MAPPER = new ObjectMapper();

    private final MerchantMapper merchantMapper;
    private final ReviewMapper reviewMapper;

    @Override
    @Transactional(readOnly = true)
    public MerchantListResponseDTO<MerchantItemResponseDTO> findMerchants(
            Long userId,
            MerchantSearchCondition condition
    ) {
        String category = condition.getCategory();
        LocalDate startDate = condition.getStartDate();
        LocalDate endDate = condition.getEndDate();
        Integer headcount = condition.getHeadcount();
        Long minPrice = condition.getMinPrice();
        Long maxPrice = condition.getMaxPrice();
        String cursor = condition.getCursor();
        Long regionId = condition.getRegionId();

        String parsedCategory = parseMerchantCategory(category);

        int safeSize = normalizeSize(condition.getSize());
        validatePriceRange(minPrice, maxPrice);
        validateHeadcount(headcount);
        validateRoomCount(condition.getRoomCount());

        // 음식점·여가는 재고가 없어 날짜를 받지 않는다
        boolean reservable = parsedCategory == null
                || "ACCOMMODATION".equals(parsedCategory)
                || "OFFICE".equals(parsedCategory);
        if (reservable) {
            validatePeriod(startDate, endDate);
        }

        int safeRoomCount = condition.getRoomCount() == null ? 1 : condition.getRoomCount();

        MerchantSortType safeSort = condition.getSort() == null
                ? MerchantSortType.RATING_DESC
                : condition.getSort();
        int querySize = safeSize + 1;

        try {
            ReservationMerchantCursor decodedCursor = decodeCursor(cursor, safeSort);

            Integer requiredDateCount = null;
            if (startDate != null && endDate != null) {
                requiredDateCount = (int) ChronoUnit.DAYS.between(startDate, endDate);
            }

            List<MerchantVO> merchants = merchantMapper.selectReservationMerchantsByCursor(
                    userId,
                    parsedCategory,
                    decodedCursor.cursorValue,
                    decodedCursor.cursorMerchantId,
                    querySize,
                    regionId,
                    startDate,
                    endDate,
                    requiredDateCount,
                    headcount,
                    safeRoomCount,
                    minPrice,
                    maxPrice,
                    condition.getAccommodationType(),
                    condition.getNoiseLevel(),
                    condition.getFoodType(),
                    condition.getPriceLevel(),
                    condition.getActivityType(),
                    safeSort.name()
            );

            if (merchants == null) {
                merchants = Collections.emptyList();
            }

            List<MerchantItemResponseDTO> content = merchants.stream()
                    .limit(safeSize)
                    .map(MerchantItemResponseDTO::from)
                    .collect(Collectors.toList());

            boolean hasNext = merchants.size() > safeSize;
            String nextCursor = hasNext
                    && !content.isEmpty()
                    ? encodeCursor(content.get(content.size() - 1), safeSort)
                    : null;

            return MerchantListResponseDTO.of(content, nextCursor, safeSize, hasNext);
        } catch (BusinessException e) {
            throw e;
        } catch (PersistenceException e) {
            log.error("Reservation merchant list 조회 중 MyBatis 오류 - category={}", category, e);
            throw new BusinessException(MerchantErrorCode.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            log.error("Reservation merchant list 조회 중 처리 오류 - category={}", category, e);
            throw new BusinessException(MerchantErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public MerchantDetailResponseDTO findAccommodationProducts(
            Long userId,
            Long merchantId,
            LocalDate startDate,
            LocalDate endDate,
            Integer roomCount,
            Integer guestCount
    ) {
        try {
            if (merchantId == null || merchantId < 1) {
                throw new IllegalArgumentException("가맹점 번호는 1 이상이어야 합니다.");
            }

            MerchantDetailVO accommodation = merchantMapper.selectMerchantAccommodationDetails(userId, merchantId);
            if (accommodation == null) {
                throw new BusinessException(MerchantErrorCode.ACCOMMODATION_NOT_FOUND);
            }

            validatePeriod(startDate, endDate);
            validateRoomCount(roomCount);
            validateGuestCount(guestCount);

            Integer requiredDateCount = null;
            Integer requiredQuantity = null;
            if (startDate != null && endDate != null) {
                requiredDateCount = (int) ChronoUnit.DAYS.between(startDate, endDate);
                requiredQuantity = roomCount == null ? 1 : roomCount;
            }

            List<MerchantProductVO> reservationProducts =
                    (startDate == null || endDate == null)
                            ? merchantMapper.selectMerchantAccommodationReservationProducts(merchantId)
                            : merchantMapper.selectMerchantAccommodationReservationProductsByPeriod(
                                    merchantId,
                                    startDate,
                                    endDate,
                                    guestCount,
                                    roomCount,
                                    requiredDateCount,
                                    requiredQuantity,
                                    false
                            );

            List<MerchantItemResponseDTO> productItems = reservationProducts == null
                    ? Collections.emptyList()
                    : reservationProducts.stream()
                            .map(MerchantItemResponseDTO::from)
                            .collect(Collectors.toList());
            MerchantListResponseDTO<MerchantItemResponseDTO> productList = MerchantListResponseDTO.ofAll(productItems);

            Integer reviewCount = merchantMapper.selectMerchantAccommodationReviewCount(merchantId);

            return MerchantDetailResponseDTO.of(
                    accommodation.getMerchantId(),
                    accommodation.getName(),
                    accommodation.getAddress(),
                    accommodation.getPhoneNumber(),
                    accommodation.getDescription(),
                    accommodation.getCheckInTime(),
                    accommodation.getCheckOutTime(),
                    accommodation.getRating(),
                    reviewCount == null ? 0 : reviewCount,
                    accommodation.getThumbnailUrl(),
                    accommodation.getBookmarked(),
                    productList.getContent()
            );
        } catch (BusinessException e) {
            throw e;
        } catch (PersistenceException e) {
            log.error("Accommodation reservation products 조회 중 MyBatis 오류 - merchantId={}", merchantId, e);
            throw new BusinessException(MerchantErrorCode.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            log.error("Accommodation reservation products 조회 중 처리 오류 - merchantId={}", merchantId, e);
            throw new BusinessException(MerchantErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public MerchantDetailResponseDTO findOfficeProducts(
            Long userId,
            Long merchantId,
            LocalDate startDate,
            LocalDate endDate,
            Integer guestCount
    ) {
        try {
            if (merchantId == null || merchantId < 1) {
                throw new BusinessException(MerchantErrorCode.INVALID_MERCHANT_ID);
            }

            if ((startDate == null && endDate != null) || (startDate != null && endDate == null)) {
                throw new BusinessException(MerchantErrorCode.INVALID_PERIOD);
            }
            validatePeriod(startDate, endDate);
            validateGuestCount(guestCount);

            Integer requiredDateCount = null;
            Integer requiredQuantity = guestCount;
            if (startDate != null && endDate != null) {
                requiredDateCount = (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
            }

            MerchantDetailVO office = merchantMapper.selectMerchantOfficeDetails(userId, merchantId);
            if (office == null) {
                throw new BusinessException(MerchantErrorCode.OFFICE_NOT_FOUND);
            }

            List<MerchantProductVO> officeProducts = merchantMapper.selectMerchantOfficeProductsByPeriod(
                    merchantId,
                    startDate,
                    endDate,
                    requiredDateCount,
                    requiredQuantity,
                    guestCount
            );
            List<MerchantItemResponseDTO> productItems = officeProducts == null
                    ? Collections.emptyList()
                    : officeProducts.stream()
                            .map(MerchantItemResponseDTO::from)
                            .collect(Collectors.toList());
            MerchantListResponseDTO<MerchantItemResponseDTO> productList = MerchantListResponseDTO.ofAll(productItems);

            return MerchantDetailResponseDTO.of(
                    office.getMerchantId(),
                    office.getName(),
                    office.getAddress(),
                    office.getPhoneNumber(),
                    office.getDescription(),
                    null,
                    null,
                    office.getRating(),
                    office.getReviewCount() == null ? 0 : office.getReviewCount(),
                    office.getThumbnailUrl(),
                    office.getBookmarked(),
                    productList.getContent()
            );
        } catch (BusinessException e) {
            throw e;
        } catch (PersistenceException e) {
            log.error("Merchant office detail 조회 중 MyBatis 오류 - merchantId={}", merchantId, e);
            throw new BusinessException(MerchantErrorCode.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            log.error("Merchant office detail 조회 중 처리 오류 - merchantId={}", merchantId, e);
            throw new BusinessException(MerchantErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public MerchantDetailCommonResponseDTO findRestaurantProducts(Long userId, Long merchantId) {
        try {
            if (merchantId == null || merchantId < 1) {
                throw new BusinessException(MerchantErrorCode.INVALID_MERCHANT_ID);
            }

            MerchantDetailVO restaurant = merchantMapper.selectMerchantRestaurantDetails(
                    userId,
                    merchantId
            );
            if (restaurant == null) {
                throw new BusinessException(MerchantErrorCode.RESTAURANT_NOT_FOUND);
            }

            return MerchantDetailCommonResponseDTO.of(
                    restaurant.getMerchantId(),
                    restaurant.getName(),
                    restaurant.getDescription(),
                    restaurant.getThumbnailUrl(),
                    restaurant.getAddress(),
                    restaurant.getPrice() == null ? null : BigDecimal.valueOf(restaurant.getPrice()),
                    restaurant.getRating(),
                    restaurant.getReviewCount(),
                    restaurant.getBookmarked(),
                    findMerchantReviews(merchantId, userId, DETAIL_REVIEW_PREVIEW_SIZE)
            );
        } catch (BusinessException e) {
            throw e;
        } catch (PersistenceException e) {
            log.error("Merchant restaurant detail 조회 중 MyBatis 오류 - merchantId={}", merchantId, e);
            throw new BusinessException(MerchantErrorCode.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            log.error("Merchant restaurant detail 조회 중 처리 오류 - merchantId={}", merchantId, e);
            throw new BusinessException(MerchantErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public MerchantDetailCommonResponseDTO findActivityProducts(Long userId, Long merchantId) {
        try {
            if (merchantId == null || merchantId < 1) {
                throw new BusinessException(MerchantErrorCode.INVALID_MERCHANT_ID);
            }

            MerchantDetailVO activity = merchantMapper.selectMerchantActivityDetails(
                    userId,
                    merchantId
            );
            if (activity == null) {
                throw new BusinessException(MerchantErrorCode.ACTIVITY_NOT_FOUND);
            }

            return MerchantDetailCommonResponseDTO.of(
                    activity.getMerchantId(),
                    activity.getName(),
                    activity.getDescription(),
                    activity.getThumbnailUrl(),
                    activity.getAddress(),
                    activity.getPrice() == null ? null : BigDecimal.valueOf(activity.getPrice()),
                    activity.getRating(),
                    activity.getReviewCount(),
                    activity.getBookmarked(),
                    findMerchantReviews(merchantId, userId, DETAIL_REVIEW_PREVIEW_SIZE)
            );
        } catch (BusinessException e) {
            throw e;
        } catch (PersistenceException e) {
            log.error("Merchant activity detail 조회 중 MyBatis 오류 - merchantId={}", merchantId, e);
            throw new BusinessException(MerchantErrorCode.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            log.error("Merchant activity detail 조회 중 처리 오류 - merchantId={}", merchantId, e);
            throw new BusinessException(MerchantErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    // 예약 유형 4탭을 모두 받는다. ReservationCategory 는 숙소·공유오피스뿐이라 쓰지 않는다
    private static final Set<String> SEARCHABLE_CATEGORIES =
            Set.of("ACCOMMODATION", "OFFICE", "RESTAURANT", "ACTIVITY");

    private String parseMerchantCategory(String category) {
        if (!StringUtils.hasText(category)) {
            return null;
        }

        String normalized = category.trim().toUpperCase(Locale.ROOT);
        if (!SEARCHABLE_CATEGORIES.contains(normalized)) {
            throw new BusinessException(MerchantErrorCode.INVALID_CATEGORY);
        }
        return normalized;
    }

    private int normalizeSize(int size) {
        if (size < 1 || size > MAX_SIZE) {
            throw new BusinessException(MerchantErrorCode.INVALID_SORT);
        }
        return size;
    }

    private void validatePriceRange(Long minPrice, Long maxPrice) {
        if (minPrice != null && minPrice < 0) {
            throw new IllegalArgumentException("최소 가격은 0 이상이어야 합니다.");
        }

        if (maxPrice != null && maxPrice < 0) {
            throw new IllegalArgumentException("최대 가격은 0 이상이어야 합니다.");
        }

        if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
            throw new IllegalArgumentException("최소 가격은 최대 가격보다 클 수 없습니다.");
        }
    }

    private void validateHeadcount(Integer headcount) {
        if (headcount != null && headcount < 1) {
            throw new IllegalArgumentException("인원 수는 1 이상이어야 합니다.");
        }
    }


    private void validatePeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new BusinessException(MerchantErrorCode.INVALID_PERIOD);
        }

        if (startDate.isAfter(endDate)) {
            throw new BusinessException(MerchantErrorCode.INVALID_PERIOD);
        }
    }

    private void validateRoomCount(Integer roomCount) {
        if (roomCount != null && roomCount < 1) {
            throw new IllegalArgumentException("방 개수는 1 이상이어야 합니다.");
        }
    }

    private void validateGuestCount(Integer guestCount) {
        if (guestCount != null && guestCount < 1) {
            throw new IllegalArgumentException("인원 수는 1 이상이어야 합니다.");
        }
    }

    private List<MerchantDetailReviewResponseDTO> findMerchantReviews(Long merchantId, Long userId, int size) {
        if (merchantId == null) {
            return Collections.emptyList();
        }

        int safeSize = Math.max(1, Math.min(size, DETAIL_REVIEW_PREVIEW_SIZE));
        List<MerchantReviewVO> reviewVOS = reviewMapper.selectMerchantReviewListWithMine(
                merchantId,
                userId,
                0,
                safeSize
        );
        if (reviewVOS == null) {
            return Collections.emptyList();
        }

        return reviewVOS.stream()
                .map(MerchantDetailReviewResponseDTO::from)
                .collect(Collectors.toList());
    }

    private ReservationMerchantCursor decodeCursor(String cursor, MerchantSortType sort) {
        if (!StringUtils.hasText(cursor)) {
            return new ReservationMerchantCursor(sort, null, null, null);
        }

        try {
            String normalizedCursor = URLDecoder.decode(cursor, StandardCharsets.UTF_8.toString());
            byte[] decoded;

            try {
                decoded = Base64.getDecoder().decode(normalizedCursor);
            } catch (IllegalArgumentException e) {
                decoded = Base64.getUrlDecoder().decode(normalizedCursor);
            }

            String payload = new String(decoded, StandardCharsets.UTF_8);
            Map<String, Object> map = RESERVATION_MERCHANT_CURSOR_OBJECT_MAPPER.readValue(payload,
                    new TypeReference<Map<String, Object>>() {
                    }
            );

            Object sortRaw = map.get(CURSOR_SORT_KEY);
            if (!(sortRaw instanceof String) || !sort.name().equals(sortRaw.toString())) {
                throw new IllegalArgumentException();
            }

            Object merchantIdRaw = map.get(CURSOR_MERCHANT_ID_KEY);
            if (merchantIdRaw == null || Long.parseLong(merchantIdRaw.toString()) < 1) {
                throw new IllegalArgumentException();
            }
            Long cursorMerchantId = Long.parseLong(merchantIdRaw.toString());

            if (sort == MerchantSortType.RATING_DESC) {
                Object ratingRaw = map.get(CURSOR_RATING_KEY);
                if (ratingRaw == null) {
                    throw new IllegalArgumentException();
                }
                return new ReservationMerchantCursor(sort, new BigDecimal(ratingRaw.toString()), cursorMerchantId, null);
            }

            Object priceRaw = map.get(CURSOR_PRICE_KEY);
            if (priceRaw == null) {
                throw new IllegalArgumentException();
            }
            return new ReservationMerchantCursor(sort, null,
                    cursorMerchantId,
                    new BigDecimal(priceRaw.toString())
            );
        } catch (IllegalArgumentException | NullPointerException | JsonProcessingException | UnsupportedEncodingException e) {
            throw new BusinessException(MerchantErrorCode.INVALID_CURSOR);
        }
    }

    private String encodeCursor(MerchantItemResponseDTO item, MerchantSortType sort) {
        try {
            if (item == null) {
                return null;
            }

            Map<String, Object> cursorMap = new LinkedHashMap<>();
            cursorMap.put(CURSOR_SORT_KEY, sort.name());
            cursorMap.put(CURSOR_MERCHANT_ID_KEY, item.getMerchantId());

            if (sort == MerchantSortType.RATING_DESC) {
                if (item.getRating() == null || item.getMerchantId() == null) {
                    return null;
                }
                cursorMap.put(CURSOR_RATING_KEY, item.getRating());
            } else {
                if (item.getPrice() == null || item.getMerchantId() == null) {
                    return null;
                }
                cursorMap.put(CURSOR_PRICE_KEY, item.getPrice());
            }

            String cursorPayload = RESERVATION_MERCHANT_CURSOR_OBJECT_MAPPER.writeValueAsString(cursorMap);
            return Base64.getUrlEncoder().encodeToString(
                    cursorPayload.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            return null;
        }
    }

    private static class ReservationMerchantCursor {

        private final MerchantSortType sort;
        private final BigDecimal cursorValue;
        private final Long cursorMerchantId;

        private ReservationMerchantCursor(MerchantSortType sort, BigDecimal cursorValue, Long cursorMerchantId,
                                          BigDecimal unused) {
            this.sort = sort;
            this.cursorValue = cursorValue;
            this.cursorMerchantId = cursorMerchantId;
        }
    }
}
