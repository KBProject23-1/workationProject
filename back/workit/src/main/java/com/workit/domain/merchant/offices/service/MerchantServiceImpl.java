package com.workit.domain.merchant.offices.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.workit.domain.merchant.offices.enums.MerchantOfficeSortType;
import com.workit.domain.merchant.offices.dto.response.MerchantOfficeItemResponseDTO;
import com.workit.domain.merchant.offices.dto.response.MerchantOfficeListResponseDTO;
import com.workit.domain.merchant.offices.dto.response.MerchantOfficeDetailResponseDTO;
import com.workit.domain.merchant.offices.exception.MerchantErrorCode;
import com.workit.domain.merchant.offices.mapper.MerchantMapper;
import com.workit.domain.merchant.offices.vo.MerchantOfficeItemVO;
import com.workit.domain.merchant.offices.vo.MerchantOfficeDetailVO;
import com.workit.domain.merchant.offices.vo.MerchantOfficeDetailTagVO;
import com.workit.exception.BusinessException;
import org.mybatis.spring.MyBatisSystemException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantServiceImpl implements MerchantService {

    private static final int MAX_SIZE = 50;
    private static final ObjectMapper OFFICE_CURSOR_OBJECT_MAPPER = new ObjectMapper();

    private final MerchantMapper merchantMapper;

    @Override
    @Transactional(readOnly = true)
    public MerchantOfficeDetailResponseDTO findOfficeDetail(Long merchantId) {
        try {
            if (merchantId == null || merchantId < 1) {
                throw new BusinessException(MerchantErrorCode.INVALID_MERCHANT_ID);
            }

            MerchantOfficeDetailVO office = merchantMapper.selectOfficeDetailById(merchantId);
            if (office == null) {
                throw new BusinessException(MerchantErrorCode.OFFICE_NOT_FOUND);
            }

            List<MerchantOfficeDetailTagVO> tags = merchantMapper.selectOfficeTagsByMerchantId(merchantId);

            return MerchantOfficeDetailResponseDTO.of(office, tags);
        } catch (BusinessException e) {
            throw e;
        } catch (MyBatisSystemException e) {
            log.error("Merchant office detail 조회 중 MyBatis 오류 - merchantId={}", merchantId, e);
            throw new BusinessException(MerchantErrorCode.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            log.error("Merchant office detail 조회 중 처리 오류 - merchantId={}", merchantId, e);
            throw new BusinessException(MerchantErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public MerchantOfficeListResponseDTO findOfficeList(
            String cursor,
            int size,
            Long regionId,
            LocalDate startDate,
            LocalDate endDate,
            MerchantOfficeSortType sort) {
        validateOfficeListCondition(regionId, startDate, endDate);
        try {
            int safeSize = normalizeSize(size);
            OfficeCursor decodedCursor = decodeCursor(cursor, sort);
            int querySize = safeSize + 1;

            List<MerchantOfficeItemVO> offices = merchantMapper.selectOfficeListByCursor(
                    decodedCursor.getRating(),
                    decodedCursor.getMerchantId(),
                    decodedCursor.getPrice(),
                    querySize,
                    regionId,
                    startDate,
                    endDate,
                    sort.name()
            );

            if (offices == null) {
                offices = java.util.Collections.emptyList();
            }

            boolean hasNext = offices.size() > safeSize;
            List<MerchantOfficeItemVO> contentSource = offices.stream()
                    .limit(safeSize)
                    .collect(Collectors.toList());

            String nextCursor = hasNext
                    ? encodeCursor(contentSource.get(contentSource.size() - 1), sort)
                    : null;

            return MerchantOfficeListResponseDTO.of(
                    contentSource.stream()
                            .map(MerchantOfficeItemResponseDTO::from)
                            .collect(Collectors.toList()),
                    nextCursor,
                    safeSize,
                    hasNext
            );
        } catch (BusinessException e) {
            throw e;
        } catch (MyBatisSystemException e) {
            log.error("Merchant office list 조회 중 MyBatis 오류", e);
            throw new BusinessException(MerchantErrorCode.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            log.error("Merchant office list 조회 중 처리 오류", e);
            throw new BusinessException(MerchantErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private void validateOfficeListCondition(Long regionId, LocalDate startDate, LocalDate endDate) {
        if (regionId == null || startDate == null || endDate == null) {
            throw new BusinessException(MerchantErrorCode.INVALID_SEARCH_CONDITION);
        }

        if (regionId < 1 || endDate.isBefore(startDate)) {
            throw new BusinessException(MerchantErrorCode.INVALID_SEARCH_CONDITION);
        }
    }

    private int normalizeSize(int size) {
        if (size < 1 || size > MAX_SIZE) {
            throw new BusinessException(MerchantErrorCode.INVALID_SEARCH_CONDITION);
        }
        return size;
    }

    private OfficeCursor decodeCursor(String cursor, MerchantOfficeSortType sort) {
        if (!StringUtils.hasText(cursor)) {
            return new OfficeCursor(sort, null, null, null);
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
            Map<String, Object> map = OFFICE_CURSOR_OBJECT_MAPPER.readValue(
                    payload,
                    new TypeReference<Map<String, Object>>() {
                    }
            );

            Object sortRaw = map.get("sort");
            if (!(sortRaw instanceof String)) {
                throw new IllegalArgumentException();
            }

            if (!StringUtils.hasText((String) sortRaw)) {
                throw new IllegalArgumentException();
            }

            MerchantOfficeSortType cursorSort = MerchantOfficeSortType.from((String) sortRaw);
            if (sort != cursorSort) {
                throw new IllegalArgumentException();
            }

            Object merchantIdRaw = map.get("merchantId");
            if (merchantIdRaw == null) {
                merchantIdRaw = map.get("MerchantId");
            }

            if (merchantIdRaw == null) {
                throw new IllegalArgumentException();
            }

            long merchantId = Long.parseLong(merchantIdRaw.toString());
            if (merchantId < 1) {
                throw new IllegalArgumentException();
            }

            if (sort.isRatingSort()) {
                Object ratingRaw = map.get("rating");
                if (ratingRaw == null) {
                    throw new IllegalArgumentException();
                }
                BigDecimal rating = new BigDecimal(ratingRaw.toString());
                return new OfficeCursor(sort, rating, null, merchantId);
            }

            Object priceRaw = map.get("price");
            if (priceRaw == null) {
                throw new IllegalArgumentException();
            }

            Long price = Long.parseLong(priceRaw.toString());
            return new OfficeCursor(sort, null, price, merchantId);
        } catch (IllegalArgumentException | NullPointerException | JsonProcessingException | ClassCastException |
                UnsupportedEncodingException | BusinessException e) {
            log.warn("오피스 커서 디코딩 실패 - cursor={}, sort={}", cursor, sort);
            throw new BusinessException(MerchantErrorCode.INVALID_OFFICE_CURSOR);
        }
    }

    private String encodeCursor(MerchantOfficeItemVO office, MerchantOfficeSortType sort) {
        if (office == null || office.getMerchantId() == null) {
            return null;
        }

        Map<String, Object> cursorMap = new LinkedHashMap<>();
        cursorMap.put("sort", sort.name());
        cursorMap.put("merchantId", office.getMerchantId());
        if (sort.isRatingSort()) {
            if (office.getRating() == null) {
                return null;
            }
            cursorMap.put("rating", office.getRating());
        } else if (office.getPrice() == null) {
            return null;
        } else {
            cursorMap.put("price", office.getPrice());
        }

        try {
            String cursorPayload = OFFICE_CURSOR_OBJECT_MAPPER.writeValueAsString(cursorMap);
            return Base64.getUrlEncoder().encodeToString(
                    cursorPayload.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            return null;
        }
    }

    private static class OfficeCursor {

        private final MerchantOfficeSortType sort;
        private final BigDecimal rating;
        private final Long price;
        private final Long merchantId;

        private OfficeCursor(MerchantOfficeSortType sort, BigDecimal rating, Long price, Long merchantId) {
            this.sort = sort;
            this.rating = rating;
            this.price = price;
            this.merchantId = merchantId;
        }

        private BigDecimal getRating() {
            return rating;
        }

        private Long getPrice() {
            return price;
        }

        private Long getMerchantId() {
            return merchantId;
        }
    }
}
