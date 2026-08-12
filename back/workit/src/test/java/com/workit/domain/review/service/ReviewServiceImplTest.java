package com.workit.domain.review.service;

import com.workit.domain.review.dto.request.ReviewCreateRequestDTO;
import com.workit.domain.review.dto.request.ReviewUpdateRequestDTO;
import com.workit.domain.review.dto.response.MerchantReviewListResponseDTO;
import com.workit.domain.review.dto.response.MyReviewListResponseDTO;
import com.workit.domain.review.dto.response.ReviewDetailResponseDTO;
import com.workit.domain.review.exception.ReviewErrorCode;
import com.workit.domain.review.mapper.ReviewMapper;
import com.workit.domain.review.vo.MerchantReviewStatisticsVO;
import com.workit.domain.review.vo.MerchantReviewVO;
import com.workit.domain.review.vo.MyReviewListItemVO;
import com.workit.domain.review.vo.OwnedReviewVO;
import com.workit.domain.review.vo.ReservationReviewSourceVO;
import com.workit.domain.review.vo.ReviewDetailVO;
import com.workit.domain.review.vo.ReviewVO;
import com.workit.domain.review.vo.TransactionReviewSourceVO;
import com.workit.exception.BusinessException;
import com.workit.global.dto.PageResponseDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReviewServiceImplTest {

    @Test
    void 가맹점리뷰목록과평점통계를조회한다() {
        MerchantReviewVO review = new MerchantReviewVO();
        review.setReviewId(11L);
        review.setUserId(1L);
        review.setNickname("워케이션러");
        review.setRating(5);
        review.setContent("좋았어요.");

        MerchantReviewStatisticsVO statistics = createStatistics();
        ReviewService service = new ReviewServiceImpl(
                new StubReviewMapper(true, Collections.singletonList(review), statistics)
        );

        MerchantReviewListResponseDTO response = service.findMerchantReviewList(1L, 0, 10);

        assertEquals("워케이션러", response.getReviews().getContent().get(0).getNickname());
        assertEquals(2L, response.getReviews().getTotalElements());
        assertEquals(1, response.getReviews().getTotalPages());
        assertEquals(new BigDecimal("4.5"), response.getAverageRating());
        assertEquals(2L, response.getReviewCount());
        assertEquals(1L, response.getRatingDistribution().get(4));
        assertEquals(1L, response.getRatingDistribution().get(5));
    }

    @Test
    void 존재하지않는가맹점의리뷰는조회할수없다() {
        ReviewService service = new ReviewServiceImpl(
                new StubReviewMapper(false, Collections.emptyList(), null)
        );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.findMerchantReviewList(999L, 0, 10)
        );

        assertEquals(ReviewErrorCode.MERCHANT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void 가맹점번호는일이상이어야한다() {
        ReviewService service = new ReviewServiceImpl(
                new StubReviewMapper(true, Collections.emptyList(), createStatistics())
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.findMerchantReviewList(0L, 0, 10)
        );
    }

    @Test
    void 예약기반리뷰상세를조회한다() {
        ReviewDetailVO review = new ReviewDetailVO();
        review.setReviewId(11L);
        review.setUserId(1L);
        review.setNickname("워케이션러");
        review.setMerchantName("테스트 호텔");
        review.setReservationId(21L);
        review.setReservationCode("RES-20260804");

        ReviewService service = new ReviewServiceImpl(
                new StubReviewMapper(
                        true,
                        Collections.emptyList(),
                        createStatistics(),
                        review
                )
        );

        ReviewDetailResponseDTO response = service.findReviewDetails(1L, 11L);

        assertEquals("워케이션러", response.getNickname());
        assertEquals("테스트 호텔", response.getMerchant().getMerchantName());
        assertEquals("RES-20260804", response.getReservationCode());
        assertEquals(true, response.getIsMine());
    }

    @Test
    void 삭제되었거나존재하지않는리뷰는조회할수없다() {
        ReviewService service = new ReviewServiceImpl(
                new StubReviewMapper(
                        true,
                        Collections.emptyList(),
                        createStatistics(),
                        null
                )
        );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.findReviewDetails(1L, 999L)
        );

        assertEquals(ReviewErrorCode.REVIEW_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void 로그인사용자의리뷰목록을카테고리정보와조회한다() {
        MyReviewListItemVO review = new MyReviewListItemVO();
        review.setReviewId(31L);
        review.setMerchantName("테스트 공유오피스");
        review.setMerchantCategory("OFFICE");
        review.setRating(4);

        ReviewService service = new ReviewServiceImpl(
                new StubReviewMapper(
                        true,
                        Collections.emptyList(),
                        createStatistics(),
                        null,
                        Collections.singletonList(review)
                )
        );

        PageResponseDTO<MyReviewListResponseDTO> response =
                service.findMyReviewList(1L, "ALL", 0, 10);

        assertEquals(1, response.getContent().size());
        assertEquals(1L, response.getTotalElements());
        assertEquals("테스트 공유오피스", response.getContent().get(0).getMerchantName());
        assertEquals("OFFICE", response.getContent().get(0).getMerchantCategory());
    }

    @Test
    void 리뷰목록페이지크기는오십을초과할수없다() {
        ReviewService service = new ReviewServiceImpl(
                new StubReviewMapper(true, Collections.emptyList(), createStatistics())
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.findMyReviewList(1L, "ALL", 0, 51)
        );
    }

    @Test
    void 숙소는체크아웃당일부터리뷰를작성한다() {
        StubReviewMapper mapper = new StubReviewMapper(
                true,
                Collections.emptyList(),
                createStatistics()
        );
        ReservationReviewSourceVO source = new ReservationReviewSourceVO();
        source.setReservationId(10L);
        source.setUserId(1L);
        source.setMerchantId(20L);
        source.setMerchantCategory("ACCOMMODATION");
        source.setReservationStatus("COMPLETED");
        source.setEndDate(LocalDate.now());
        mapper.reservationSource = source;

        ReviewCreateRequestDTO request = new ReviewCreateRequestDTO();
        request.setRating(5);

        ReviewService service = new ReviewServiceImpl(mapper);
        service.addReservationReview(1L, 10L, request);

        assertEquals(10L, mapper.insertedReview.getReservationId());
        assertEquals(5, mapper.insertedReview.getRating());
    }

    @Test
    void 공유오피스는종료다음날부터분위기태그와리뷰를작성한다() {
        StubReviewMapper mapper = new StubReviewMapper(
                true,
                Collections.emptyList(),
                createStatistics()
        );
        ReservationReviewSourceVO source = new ReservationReviewSourceVO();
        source.setReservationId(11L);
        source.setUserId(1L);
        source.setMerchantId(21L);
        source.setMerchantCategory("OFFICE");
        source.setReservationStatus("COMPLETED");
        source.setEndDate(LocalDate.now().minusDays(1));
        mapper.reservationSource = source;

        ReviewCreateRequestDTO request = new ReviewCreateRequestDTO();
        request.setRating(4);

        ReviewService service = new ReviewServiceImpl(mapper);
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.addReservationReview(1L, 11L, request)
        );

        assertEquals(ReviewErrorCode.ATMOSPHERE_REQUIRED, exception.getErrorCode());
    }

    @Test
    void 삭제된리뷰도같은예약으로재작성할수없다() {
        StubReviewMapper mapper = new StubReviewMapper(
                true,
                Collections.emptyList(),
                createStatistics()
        );
        ReservationReviewSourceVO source = new ReservationReviewSourceVO();
        source.setReservationId(12L);
        source.setReviewId(99L);
        mapper.reservationSource = source;

        ReviewCreateRequestDTO request = new ReviewCreateRequestDTO();
        request.setRating(5);

        ReviewService service = new ReviewServiceImpl(mapper);
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.addReservationReview(1L, 12L, request)
        );

        assertEquals(ReviewErrorCode.DUPLICATE_REVIEW, exception.getErrorCode());
        assertEquals("이미 작성했거나 삭제된 내역입니다.", exception.getMessage());
    }

    @Test
    void 결제완료된식당거래는즉시리뷰를작성한다() {
        StubReviewMapper mapper = new StubReviewMapper(
                true,
                Collections.emptyList(),
                createStatistics()
        );
        TransactionReviewSourceVO source = new TransactionReviewSourceVO();
        source.setTransactionId(30L);
        source.setUserId(1L);
        source.setMerchantId(40L);
        source.setMerchantCategory("RESTAURANT");
        source.setTransactionType("PAYMENT");
        source.setTransactionStatus("PAID");
        source.setApprovedAt(LocalDateTime.now());
        mapper.transactionSource = source;

        ReviewCreateRequestDTO request = new ReviewCreateRequestDTO();
        request.setRating(3);

        ReviewService service = new ReviewServiceImpl(mapper);
        service.addTransactionReview(1L, 30L, request);

        assertEquals(30L, mapper.insertedReview.getTransactionId());
    }

    @Test
    void 이용종료후삼십일이지나면리뷰를수정할수없다() {
        StubReviewMapper mapper = new StubReviewMapper(
                true,
                Collections.emptyList(),
                createStatistics()
        );
        OwnedReviewVO review = new OwnedReviewVO();
        review.setReviewId(50L);
        review.setUserId(1L);
        review.setReservationId(10L);
        review.setStatus("ACTIVE");
        review.setMerchantCategory("ACCOMMODATION");
        review.setReservationEndDate(LocalDate.now().minusDays(31));
        mapper.ownedReview = review;

        ReviewUpdateRequestDTO request = new ReviewUpdateRequestDTO();
        request.setRating(4);

        ReviewService service = new ReviewServiceImpl(mapper);
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.modifyReview(1L, 50L, request)
        );

        assertEquals(ReviewErrorCode.REVIEW_PERIOD_EXPIRED, exception.getErrorCode());
    }

    @Test
    void 수정기한이지나도리뷰는소프트삭제할수있다() {
        StubReviewMapper mapper = new StubReviewMapper(
                true,
                Collections.emptyList(),
                createStatistics()
        );
        OwnedReviewVO review = new OwnedReviewVO();
        review.setReviewId(51L);
        review.setUserId(1L);
        review.setReservationId(10L);
        review.setStatus("ACTIVE");
        review.setReservationEndDate(LocalDate.now().minusDays(31));
        mapper.ownedReview = review;

        ReviewService service = new ReviewServiceImpl(mapper);
        service.removeReview(1L, 51L);

        assertEquals(1, mapper.softDeleteCount);
    }

    @Test
    void 작성근거가없는기존리뷰도널예외없이수정한다() {
        StubReviewMapper mapper = new StubReviewMapper(
                true,
                Collections.emptyList(),
                createStatistics()
        );
        OwnedReviewVO review = new OwnedReviewVO();
        review.setReviewId(52L);
        review.setUserId(1L);
        review.setStatus("ACTIVE");
        review.setMerchantCategory("RESTAURANT");
        review.setCreatedAt(LocalDateTime.now().minusDays(1));
        mapper.ownedReview = review;

        ReviewUpdateRequestDTO request = new ReviewUpdateRequestDTO();
        request.setRating(5);

        ReviewService service = new ReviewServiceImpl(mapper);
        service.modifyReview(1L, 52L, request);
    }

    private MerchantReviewStatisticsVO createStatistics() {
        MerchantReviewStatisticsVO statistics = new MerchantReviewStatisticsVO();
        statistics.setAverageRating(new BigDecimal("4.5"));
        statistics.setReviewCount(2L);
        statistics.setOneStarCount(0L);
        statistics.setTwoStarCount(0L);
        statistics.setThreeStarCount(0L);
        statistics.setFourStarCount(1L);
        statistics.setFiveStarCount(1L);
        return statistics;
    }

    private static class StubReviewMapper implements ReviewMapper {

        private final boolean merchantExists;
        private final List<MerchantReviewVO> reviews;
        private final MerchantReviewStatisticsVO statistics;
        private final ReviewDetailVO reviewDetail;
        private final List<MyReviewListItemVO> myReviews;
        private ReservationReviewSourceVO reservationSource;
        private TransactionReviewSourceVO transactionSource;
        private OwnedReviewVO ownedReview;
        private ReviewVO insertedReview;
        private int softDeleteCount;

        private StubReviewMapper(
                boolean merchantExists,
                List<MerchantReviewVO> reviews,
                MerchantReviewStatisticsVO statistics) {
            this(merchantExists, reviews, statistics, null);
        }

        private StubReviewMapper(
                boolean merchantExists,
                List<MerchantReviewVO> reviews,
                MerchantReviewStatisticsVO statistics,
                ReviewDetailVO reviewDetail) {
            this(merchantExists, reviews, statistics, reviewDetail, Collections.emptyList());
        }

        private StubReviewMapper(
                boolean merchantExists,
                List<MerchantReviewVO> reviews,
                MerchantReviewStatisticsVO statistics,
                ReviewDetailVO reviewDetail,
                List<MyReviewListItemVO> myReviews) {
            this.merchantExists = merchantExists;
            this.reviews = reviews;
            this.statistics = statistics;
            this.reviewDetail = reviewDetail;
            this.myReviews = myReviews;
        }

        @Override
        public boolean selectMerchantExists(Long merchantId) {
            return merchantExists;
        }

        @Override
        public List<MerchantReviewVO> selectMerchantReviewList(
                Long merchantId,
                int offset,
                int size) {
            return reviews;
        }

        @Override
        public List<MerchantReviewVO> selectMerchantReviewListWithMine(
                Long merchantId,
                Long userId,
                int offset,
                int size) {
            return selectMerchantReviewList(merchantId, offset, size);
        }

        @Override
        public MerchantReviewStatisticsVO selectMerchantReviewStatistics(Long merchantId) {
            return statistics;
        }

        @Override
        public String selectMerchantName(Long merchantId) {
            return "테스트 가맹점";
        }

        @Override
        public ReviewDetailVO selectReviewDetails(Long reviewId) {
            return reviewDetail;
        }

        @Override
        public List<MyReviewListItemVO> selectMyReviewList(
                Long userId,
                String category,
                int offset,
                int size) {
            return myReviews;
        }

        @Override
        public long countMyReviewList(Long userId, String category) {
            return myReviews.size();
        }

        @Override
        public ReservationReviewSourceVO selectReservationReviewSource(
                Long userId,
                Long reservationId) {
            return reservationSource;
        }

        @Override
        public TransactionReviewSourceVO selectTransactionReviewSource(
                Long userId,
                Long transactionId) {
            return transactionSource;
        }

        @Override
        public void insertReview(ReviewVO review) {
            review.setReviewId(100L);
            insertedReview = review;
        }

        @Override
        public OwnedReviewVO selectOwnedReview(Long userId, Long reviewId) {
            return ownedReview;
        }

        @Override
        public int updateReview(
                Long reviewId,
                com.workit.domain.review.dto.request.ReviewUpdateRequestDTO request) {
            return 1;
        }

        @Override
        public int updateReviewStatusDeleted(Long userId, Long reviewId) {
            softDeleteCount++;
            return 1;
        }
    }
}
