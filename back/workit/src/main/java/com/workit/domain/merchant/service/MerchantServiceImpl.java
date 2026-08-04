package com.workit.domain.merchant.service;

import com.workit.domain.merchant.dto.request.AccommodationListRequestDTO;
import com.workit.domain.merchant.dto.response.AccommodationListResponseDTO;
import com.workit.domain.merchant.dto.response.AccommodationDetailResponseDTO;
import com.workit.domain.merchant.exception.MerchantErrorCode;
import com.workit.domain.merchant.mapper.MerchantMapper;
import com.workit.domain.merchant.vo.AccommodationDetailVO;
import com.workit.exception.BusinessException;
import com.workit.global.dto.PageResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MerchantServiceImpl implements MerchantService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final BigDecimal MAX_RATING = new BigDecimal("5.0");

    private final MerchantMapper merchantMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<AccommodationListResponseDTO> findAccommodationList(
            AccommodationListRequestDTO request) {

        validateRequest(request);

        long totalElements = merchantMapper.countAccommodationList(
                request.getRegionId(),
                request.getAccommodationType(),
                request.getMinPrice(),
                request.getMaxPrice(),
                request.getMinRating()
        );

        if (totalElements == 0) {
            return PageResponseDTO.of(
                    Collections.emptyList(),
                    request.getPage(),
                    request.getSize(),
                    0
            );
        }

        int offset = request.getPage() * request.getSize();
        List<AccommodationListResponseDTO> content = merchantMapper.selectAccommodationList(
                        request.getRegionId(),
                        request.getAccommodationType(),
                        request.getMinPrice(),
                        request.getMaxPrice(),
                        request.getMinRating(),
                        request.getSort().name(),
                        offset,
                        request.getSize()
                )
                .stream()
                .map(AccommodationListResponseDTO::from)
                .collect(Collectors.toList());

        return PageResponseDTO.of(
                content,
                request.getPage(),
                request.getSize(),
                totalElements
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AccommodationDetailResponseDTO findAccommodationDetails(Long merchantId) {
        if (merchantId == null || merchantId < 1) {
            throw new IllegalArgumentException("가맹점 번호는 1 이상이어야 합니다.");
        }

        AccommodationDetailVO accommodation = merchantMapper.selectAccommodationDetails(merchantId);
        if (accommodation == null) {
            throw new BusinessException(MerchantErrorCode.ACCOMMODATION_NOT_FOUND);
        }

        return AccommodationDetailResponseDTO.from(
                accommodation,
                merchantMapper.selectMerchantTagNames(merchantId)
        );
    }

    // 필수 지역과 가격·평점·페이징 조건의 유효성 검증
    private void validateRequest(AccommodationListRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("조회 조건이 필요합니다.");
        }

        if (request.getRegionId() == null || request.getRegionId() < 1) {
            throw new IllegalArgumentException("지역 번호는 1 이상이어야 합니다.");
        }

        if (request.getMinPrice() != null && request.getMinPrice() < 0) {
            throw new IllegalArgumentException("최소 가격은 0 이상이어야 합니다.");
        }

        if (request.getMaxPrice() != null && request.getMaxPrice() < 0) {
            throw new IllegalArgumentException("최대 가격은 0 이상이어야 합니다.");
        }

        if (request.getMinPrice() != null
                && request.getMaxPrice() != null
                && request.getMinPrice() > request.getMaxPrice()) {
            throw new IllegalArgumentException("최소 가격은 최대 가격보다 클 수 없습니다.");
        }

        if (request.getMinRating() != null
                && (request.getMinRating().compareTo(BigDecimal.ZERO) < 0
                || request.getMinRating().compareTo(MAX_RATING) > 0)) {
            throw new IllegalArgumentException("최소 평점은 0.0 이상 5.0 이하여야 합니다.");
        }

        if (request.getSort() == null) {
            throw new IllegalArgumentException("정렬 조건이 필요합니다.");
        }

        if (request.getPage() < 0) {
            throw new IllegalArgumentException("페이지 번호는 0 이상이어야 합니다.");
        }

        if (request.getSize() < 1 || request.getSize() > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("페이지 크기는 1 이상 50 이하여야 합니다.");
        }

        if (request.getPage() > Integer.MAX_VALUE / request.getSize()) {
            throw new IllegalArgumentException("요청한 페이지 범위가 너무 큽니다.");
        }
    }
}
