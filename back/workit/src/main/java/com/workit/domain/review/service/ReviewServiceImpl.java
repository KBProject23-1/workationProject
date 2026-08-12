package com.workit.domain.review.service;

import com.workit.domain.review.dto.request.ReviewCreateRequestDTO;
import com.workit.domain.review.dto.request.ReviewUpdateRequestDTO;
import com.workit.domain.review.dto.response.MerchantReviewItemResponseDTO;
import com.workit.domain.review.dto.response.MerchantReviewListResponseDTO;
import com.workit.domain.review.dto.response.MyReviewListResponseDTO;
import com.workit.domain.review.dto.response.ReviewCreateResponseDTO;
import com.workit.domain.review.dto.response.ReviewDetailResponseDTO;
import com.workit.domain.review.dto.response.ReviewUpdateResponseDTO;
import com.workit.domain.review.exception.ReviewErrorCode;
import com.workit.domain.review.mapper.ReviewMapper;
import com.workit.domain.review.vo.MerchantReviewStatisticsVO;
import com.workit.domain.review.vo.OwnedReviewVO;
import com.workit.domain.review.vo.ReservationReviewSourceVO;
import com.workit.domain.review.vo.ReviewAtmosphere;
import com.workit.domain.review.vo.ReviewDetailVO;
import com.workit.domain.review.vo.ReviewVO;
import com.workit.domain.review.vo.TransactionReviewSourceVO;
import com.workit.exception.BusinessException;
import com.workit.global.dto.PageResponseDTO;
import com.workit.global.util.UploadFiles;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private static final int MAX_PAGE_SIZE = 50;

    private final ReviewMapper reviewMapper;

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

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
        String merchantName = reviewMapper.selectMerchantName(merchantId);
        Map<Integer, Long> ratingDistribution = createRatingDistribution(statistics);
        int offset = page * size;
        List<MerchantReviewItemResponseDTO> reviews =
                reviewMapper.selectMerchantReviewList(merchantId, offset, size)
                        .stream()
                        .map(MerchantReviewItemResponseDTO::from)
                        .collect(Collectors.toList());

        return MerchantReviewListResponseDTO.of(
                PageResponseDTO.of(reviews, page, size, statistics.getReviewCount()),
                merchantId,
                merchantName,
                statistics,
                ratingDistribution
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewDetailResponseDTO findReviewDetails(Long userId, Long reviewId) {
        if (reviewId == null || reviewId < 1) {
            throw new IllegalArgumentException("리뷰 번호는 1 이상이어야 합니다.");
        }

        ReviewDetailVO review = reviewMapper.selectReviewDetails(reviewId);
        if (review == null) {
            throw new BusinessException(ReviewErrorCode.REVIEW_NOT_FOUND);
        }

        return ReviewDetailResponseDTO.from(review, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<MyReviewListResponseDTO> findMyReviewList(
            Long userId,
            String category,
            int page,
            int size) {
        if (userId == null || userId < 1) {
            throw new IllegalArgumentException("사용자 번호는 1 이상이어야 합니다.");
        }
        validatePageRequest(page, size);
        validateCategory(category);

        long totalElements = reviewMapper.countMyReviewList(userId, category);
        int offset = page * size;
        List<MyReviewListResponseDTO> content = reviewMapper.selectMyReviewList(
                        userId,
                        category,
                        offset,
                        size
                )
                .stream()
                .map(MyReviewListResponseDTO::from)
                .collect(Collectors.toList());

        return PageResponseDTO.of(content, page, size, totalElements);
    }

    @Override
    @Transactional
    public ReviewCreateResponseDTO addReservationReview(
            Long userId,
            Long reservationId,
            ReviewCreateRequestDTO request) {
        validateUserAndSourceId(userId, reservationId, "예약 번호");
        validateCreateRequest(request);

        ReservationReviewSourceVO source =
                reviewMapper.selectReservationReviewSource(userId, reservationId);
        if (source == null) {
            throw new BusinessException(ReviewErrorCode.RESERVATION_NOT_FOUND);
        }
        if (source.getReviewId() != null) {
            throw new BusinessException(ReviewErrorCode.DUPLICATE_REVIEW);
        }
        if (!"ACCOMMODATION".equals(source.getMerchantCategory())
                && !"OFFICE".equals(source.getMerchantCategory())) {
            throw new BusinessException(ReviewErrorCode.UNSUPPORTED_RESERVATION_CATEGORY);
        }
        if ("CANCELED".equals(source.getReservationStatus())) {
            throw new BusinessException(ReviewErrorCode.CANCELED_RESERVATION);
        }

        validateReservationPeriod(source);
        validateAtmosphere(source.getMerchantCategory(), request.getAtmosphere(), true);
        request.setImageUrl(storeReviewImage(request.getImage()));

        ReviewVO review = createReview(userId, source.getMerchantId(), request);
        review.setReservationId(reservationId);
        insertReview(review);
        return ReviewCreateResponseDTO.from(review);
    }

    @Override
    @Transactional
    public ReviewCreateResponseDTO addTransactionReview(
            Long userId,
            Long transactionId,
            ReviewCreateRequestDTO request) {
        validateUserAndSourceId(userId, transactionId, "결제 번호");
        validateCreateRequest(request);

        TransactionReviewSourceVO source =
                reviewMapper.selectTransactionReviewSource(userId, transactionId);
        if (source == null) {
            throw new BusinessException(ReviewErrorCode.TRANSACTION_NOT_FOUND);
        }
        if (source.getReviewId() != null) {
            throw new BusinessException(ReviewErrorCode.DUPLICATE_REVIEW);
        }
        if (!"RESTAURANT".equals(source.getMerchantCategory())
                && !"ACTIVITY".equals(source.getMerchantCategory())) {
            throw new BusinessException(ReviewErrorCode.UNSUPPORTED_TRANSACTION_CATEGORY);
        }
        if (!"PAYMENT".equals(source.getTransactionType())
                || !"PAID".equals(source.getTransactionStatus())) {
            throw new BusinessException(ReviewErrorCode.PAYMENT_NOT_COMPLETED);
        }
        if (LocalDateTime.now().isAfter(source.getApprovedAt().plusDays(30))) {
            throw new BusinessException(ReviewErrorCode.REVIEW_PERIOD_EXPIRED);
        }
        validateAtmosphere(source.getMerchantCategory(), request.getAtmosphere(), true);
        request.setImageUrl(storeReviewImage(request.getImage()));

        ReviewVO review = createReview(userId, source.getMerchantId(), request);
        review.setTransactionId(transactionId);
        insertReview(review);
        return ReviewCreateResponseDTO.from(review);
    }

    @Override
    @Transactional
    public ReviewUpdateResponseDTO modifyReview(
            Long userId,
            Long reviewId,
            ReviewUpdateRequestDTO request) {
        validateUserAndSourceId(userId, reviewId, "리뷰 번호");
        if (request == null || !request.hasAnyField()) {
            throw new BusinessException(ReviewErrorCode.UPDATE_FIELD_REQUIRED);
        }
        if (request.isRatingProvided()) {
            validateRating(request.getRating());
        }

        OwnedReviewVO review = reviewMapper.selectOwnedReview(userId, reviewId);
        if (review == null) {
            throw new BusinessException(ReviewErrorCode.REVIEW_NOT_FOUND);
        }
        if (!"ACTIVE".equals(review.getStatus())) {
            throw new BusinessException(ReviewErrorCode.REVIEW_NOT_ACTIVE);
        }
        validateModificationPeriod(review);

        if (request.isAtmosphereProvided()) {
            validateAtmosphere(
                    review.getMerchantCategory(),
                    request.getAtmosphere(),
                    false
            );
        }

        if (request.getImage() != null && !request.getImage().isEmpty()) {
            request.setImageUrl(storeReviewImage(request.getImage()));
        }

        reviewMapper.updateReview(reviewId, request);
        return ReviewUpdateResponseDTO.of(reviewId);
    }

    @Override
    @Transactional
    public void removeReview(Long userId, Long reviewId) {
        validateUserAndSourceId(userId, reviewId, "리뷰 번호");

        OwnedReviewVO review = reviewMapper.selectOwnedReview(userId, reviewId);
        if (review == null) {
            throw new BusinessException(ReviewErrorCode.REVIEW_NOT_FOUND);
        }
        if (!"ACTIVE".equals(review.getStatus())) {
            throw new BusinessException(ReviewErrorCode.REVIEW_NOT_ACTIVE);
        }

        reviewMapper.updateReviewStatusDeleted(userId, reviewId);
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

    private void validateCategory(String category) {
        if (category == null || "ALL".equals(category)) {
            return;
        }

        if (!List.of("ACCOMMODATION", "OFFICE", "RESTAURANT", "ACTIVITY").contains(category)) {
            throw new IllegalArgumentException("지원하지 않는 가맹점 카테고리입니다.");
        }
    }

    private void validateCreateRequest(ReviewCreateRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("리뷰 작성 정보가 필요합니다.");
        }
        validateRating(request.getRating());
    }

    private void validateRating(Integer rating) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new BusinessException(ReviewErrorCode.INVALID_RATING);
        }
    }

    private void validateUserAndSourceId(Long userId, Long sourceId, String sourceName) {
        if (userId == null || userId < 1) {
            throw new IllegalArgumentException("사용자 번호는 1 이상이어야 합니다.");
        }
        if (sourceId == null || sourceId < 1) {
            throw new IllegalArgumentException(sourceName + "는 1 이상이어야 합니다.");
        }
    }

    private void validateReservationPeriod(ReservationReviewSourceVO source) {
        LocalDate today = LocalDate.now();
        LocalDate availableDate = "OFFICE".equals(source.getMerchantCategory())
                ? source.getEndDate().plusDays(1)
                : source.getEndDate();

        if (today.isBefore(availableDate)) {
            throw new BusinessException(ReviewErrorCode.REVIEW_NOT_AVAILABLE_YET);
        }
        if (today.isAfter(source.getEndDate().plusDays(30))) {
            throw new BusinessException(ReviewErrorCode.REVIEW_PERIOD_EXPIRED);
        }
    }

    private void validateModificationPeriod(OwnedReviewVO review) {
        boolean expired;
        if (review.getReservationId() != null && review.getReservationEndDate() != null) {
            expired = LocalDate.now().isAfter(review.getReservationEndDate().plusDays(30));
        } else if (review.getTransactionId() != null
                && review.getTransactionApprovedAt() != null) {
            expired = LocalDateTime.now().isAfter(
                    review.getTransactionApprovedAt().plusDays(30)
            );
        } else {
            expired = review.getCreatedAt() != null
                    && LocalDateTime.now().isAfter(review.getCreatedAt().plusDays(30));
        }

        if (expired) {
            throw new BusinessException(ReviewErrorCode.REVIEW_PERIOD_EXPIRED);
        }
    }

    private void validateAtmosphere(
            String merchantCategory,
            ReviewAtmosphere atmosphere,
            boolean create) {
        if ("OFFICE".equals(merchantCategory)) {
            if (atmosphere == null && create) {
                throw new BusinessException(ReviewErrorCode.ATMOSPHERE_REQUIRED);
            }
            if (atmosphere == null && !create) {
                throw new BusinessException(ReviewErrorCode.ATMOSPHERE_REQUIRED);
            }
            return;
        }

        if (atmosphere != null) {
            throw new BusinessException(ReviewErrorCode.ATMOSPHERE_ONLY_OFFICE);
        }
    }

    private ReviewVO createReview(
            Long userId,
            Long merchantId,
            ReviewCreateRequestDTO request) {
        ReviewVO review = new ReviewVO();
        review.setUserId(userId);
        review.setMerchantId(merchantId);
        review.setRating(request.getRating());
        review.setContent(request.getContent());
        review.setAtmosphere(request.getAtmosphere());
        review.setImageUrl(request.getImageUrl());
        return review;
    }

    private void insertReview(ReviewVO review) {
        try {
            reviewMapper.insertReview(review);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ReviewErrorCode.DUPLICATE_REVIEW);
        }
    }

    // 업로드된 리뷰 이미지를 정적 리소스 경로로 변환
    private String storeReviewImage(org.springframework.web.multipart.MultipartFile image) {
        if (image == null || image.isEmpty()) {
            return null;
        }
        try {
            String storedPath = UploadFiles.upload(uploadDir, image);
            return "/uploads/" + Paths.get(storedPath).getFileName();
        } catch (IOException exception) {
            throw new UncheckedIOException("리뷰 이미지 저장에 실패했습니다.", exception);
        }
    }
}
