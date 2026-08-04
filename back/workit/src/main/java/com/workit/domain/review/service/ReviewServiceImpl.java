package com.workit.domain.review.service;

import com.workit.domain.review.dto.response.MerchantReviewItemResponseDTO;
import com.workit.domain.review.dto.response.MerchantReviewListResponseDTO;
import com.workit.domain.review.dto.response.MyReviewListResponseDTO;
import com.workit.domain.review.dto.response.ReviewDetailResponseDTO;
import com.workit.domain.review.exception.ReviewErrorCode;
import com.workit.domain.review.mapper.ReviewMapper;
import com.workit.domain.review.vo.MerchantReviewStatisticsVO;
import com.workit.domain.review.vo.ReviewDetailVO;
import com.workit.exception.BusinessException;
import com.workit.global.dto.PageResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private static final int MAX_PAGE_SIZE = 50;

    private final ReviewMapper reviewMapper;

    @Override
    @Transactional(readOnly = true)
    public MerchantReviewListResponseDTO findMerchantReviewList(
            Long merchantId,
            int page,
            int size) {
        if (merchantId == null || merchantId < 1) {
            throw new IllegalArgumentException("가맹점 번호는 1 이상이어야 합니다.");
        }
        validatePageRequest(page, size);

        if (!reviewMapper.selectMerchantExists(merchantId)) {
            throw new BusinessException(ReviewErrorCode.MERCHANT_NOT_FOUND);
        }

        MerchantReviewStatisticsVO statistics =
                reviewMapper.selectMerchantReviewStatistics(merchantId);
        Map<Integer, Long> ratingDistribution = createRatingDistribution(statistics);
        int offset = page * size;
        List<MerchantReviewItemResponseDTO> reviews =
                reviewMapper.selectMerchantReviewList(merchantId, offset, size)
                        .stream()
                        .map(MerchantReviewItemResponseDTO::from)
                        .collect(Collectors.toList());

        return MerchantReviewListResponseDTO.of(
                PageResponseDTO.of(reviews, page, size, statistics.getReviewCount()),
                statistics,
                ratingDistribution
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewDetailResponseDTO findReviewDetails(Long reviewId) {
        if (reviewId == null || reviewId < 1) {
            throw new IllegalArgumentException("리뷰 번호는 1 이상이어야 합니다.");
        }

        ReviewDetailVO review = reviewMapper.selectReviewDetails(reviewId);
        if (review == null) {
            throw new BusinessException(ReviewErrorCode.REVIEW_NOT_FOUND);
        }

        return ReviewDetailResponseDTO.from(review);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<MyReviewListResponseDTO> findMyReviewList(
            Long userId,
            int page,
            int size) {
        if (userId == null || userId < 1) {
            throw new IllegalArgumentException("사용자 번호는 1 이상이어야 합니다.");
        }
        validatePageRequest(page, size);

        long totalElements = reviewMapper.countMyReviewList(userId);
        int offset = page * size;
        List<MyReviewListResponseDTO> content = reviewMapper.selectMyReviewList(
                        userId,
                        offset,
                        size
                )
                .stream()
                .map(MyReviewListResponseDTO::from)
                .collect(Collectors.toList());

        return PageResponseDTO.of(content, page, size, totalElements);
    }

    // 별점 1점부터 5점까지 누락 없이 분포 구성
    private Map<Integer, Long> createRatingDistribution(MerchantReviewStatisticsVO statistics) {
        Map<Integer, Long> ratingDistribution = new LinkedHashMap<>();
        ratingDistribution.put(1, statistics.getOneStarCount());
        ratingDistribution.put(2, statistics.getTwoStarCount());
        ratingDistribution.put(3, statistics.getThreeStarCount());
        ratingDistribution.put(4, statistics.getFourStarCount());
        ratingDistribution.put(5, statistics.getFiveStarCount());
        return ratingDistribution;
    }

    // Merchant 목록과 동일한 페이지 번호·크기 범위 검증
    private void validatePageRequest(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("페이지 번호는 0 이상이어야 합니다.");
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("페이지 크기는 1 이상 50 이하여야 합니다.");
        }

        if (page > Integer.MAX_VALUE / size) {
            throw new IllegalArgumentException("요청한 페이지 범위가 너무 큽니다.");
        }
    }
}
