package com.workit.domain.review.service;

import com.workit.domain.review.dto.response.MerchantReviewItemResponseDTO;
import com.workit.domain.review.dto.response.MerchantReviewListResponseDTO;
import com.workit.domain.review.dto.response.ReviewDetailResponseDTO;
import com.workit.domain.review.exception.ReviewErrorCode;
import com.workit.domain.review.mapper.ReviewMapper;
import com.workit.domain.review.vo.MerchantReviewStatisticsVO;
import com.workit.domain.review.vo.ReviewDetailVO;
import com.workit.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewMapper reviewMapper;

    @Override
    @Transactional(readOnly = true)
    public MerchantReviewListResponseDTO findMerchantReviewList(Long merchantId) {
        if (merchantId == null || merchantId < 1) {
            throw new IllegalArgumentException("가맹점 번호는 1 이상이어야 합니다.");
        }

        if (!reviewMapper.selectMerchantExists(merchantId)) {
            throw new BusinessException(ReviewErrorCode.MERCHANT_NOT_FOUND);
        }

        MerchantReviewStatisticsVO statistics =
                reviewMapper.selectMerchantReviewStatistics(merchantId);
        Map<Integer, Long> ratingDistribution = createRatingDistribution(statistics);

        return MerchantReviewListResponseDTO.of(
                reviewMapper.selectMerchantReviewList(merchantId)
                        .stream()
                        .map(MerchantReviewItemResponseDTO::from)
                        .collect(Collectors.toList()),
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
}
