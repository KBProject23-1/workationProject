package com.workit.domain.review.service;

import com.workit.domain.review.dto.response.MerchantReviewListResponseDTO;
import com.workit.domain.review.dto.response.MyReviewListResponseDTO;
import com.workit.domain.review.dto.response.ReviewDetailResponseDTO;
import com.workit.domain.review.exception.ReviewErrorCode;
import com.workit.domain.review.mapper.ReviewMapper;
import com.workit.domain.review.vo.MerchantReviewStatisticsVO;
import com.workit.domain.review.vo.MerchantReviewVO;
import com.workit.domain.review.vo.MyReviewListItemVO;
import com.workit.domain.review.vo.ReviewDetailVO;
import com.workit.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReviewServiceImplTest {

    @Test
    void 가맹점리뷰목록과평점통계를조회한다() {
        MerchantReviewVO review = new MerchantReviewVO();
        review.setReviewId(11L);
        review.setNickname("워케이션러");
        review.setRating(5);
        review.setContent("좋았어요.");

        MerchantReviewStatisticsVO statistics = createStatistics();
        ReviewService service = new ReviewServiceImpl(
                new StubReviewMapper(true, Collections.singletonList(review), statistics)
        );

        MerchantReviewListResponseDTO response = service.findMerchantReviewList(1L);

        assertEquals("워케이션러", response.getReviews().get(0).getNickname());
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
                () -> service.findMerchantReviewList(999L)
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
                () -> service.findMerchantReviewList(0L)
        );
    }

    @Test
    void 예약기반리뷰상세를조회한다() {
        ReviewDetailVO review = new ReviewDetailVO();
        review.setReviewId(11L);
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

        ReviewDetailResponseDTO response = service.findReviewDetails(11L);

        assertEquals("워케이션러", response.getNickname());
        assertEquals("테스트 호텔", response.getMerchantName());
        assertEquals("RES-20260804", response.getReservationCode());
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
                () -> service.findReviewDetails(999L)
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

        List<MyReviewListResponseDTO> response = service.findMyReviewList(1L);

        assertEquals(1, response.size());
        assertEquals("테스트 공유오피스", response.get(0).getMerchantName());
        assertEquals("OFFICE", response.get(0).getMerchantCategory());
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
        public List<MerchantReviewVO> selectMerchantReviewList(Long merchantId) {
            return reviews;
        }

        @Override
        public MerchantReviewStatisticsVO selectMerchantReviewStatistics(Long merchantId) {
            return statistics;
        }

        @Override
        public ReviewDetailVO selectReviewDetails(Long reviewId) {
            return reviewDetail;
        }

        @Override
        public List<MyReviewListItemVO> selectMyReviewList(Long userId) {
            return myReviews;
        }
    }
}
