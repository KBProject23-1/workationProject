package com.workit.domain.recommendation.restaurant.service;

import com.workit.domain.recommendation.restaurant.dto.request.RecommendationRecalculateRequestDTO;
import com.workit.domain.recommendation.restaurant.dto.request.RestaurantRecommendationCreateRequestDTO;
import com.workit.domain.recommendation.common.dto.response.RecommendationListResponseDTO;
import com.workit.domain.recommendation.restaurant.dto.response.RestaurantRecommendationResponseDTO;
import com.workit.domain.recommendation.restaurant.dto.response.RecommendationCandidateListResponseDTO;
import com.workit.domain.recommendation.restaurant.dto.response.RecommendationCandidateResponseDTO;
import com.workit.domain.recommendation.restaurant.dto.response.RestaurantReferenceResponseDTO;
import com.workit.domain.recommendation.restaurant.exception.RestaurantRecommendationErrorCode;
import com.workit.domain.recommendation.restaurant.mapper.RestaurantRecommendationMapper;
import com.workit.domain.recommendation.restaurant.vo.MealType;
import com.workit.domain.recommendation.restaurant.vo.RecommendationMerchantVO;
import com.workit.domain.recommendation.restaurant.vo.RecommendationRequestVO;
import com.workit.domain.recommendation.restaurant.vo.RecommendationResultVO;
import com.workit.domain.recommendation.restaurant.vo.RecommendationType;
import com.workit.domain.recommendation.restaurant.vo.ReferenceType;
import com.workit.domain.recommendation.restaurant.vo.RestaurantConditionVO;
import com.workit.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RestaurantRecommendationServiceImpl implements RestaurantRecommendationService {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    private final RestaurantRecommendationMapper recommendationMapper;
    private final RestaurantScoreCalculator restaurantScoreCalculator;

    @Override
    @Transactional
    public RecommendationListResponseDTO<RestaurantRecommendationResponseDTO.Item> addRestaurantRecommendation(Long userId,
                                                                          RestaurantRecommendationCreateRequestDTO request) {
        if (request == null || request.getMealType() == null) {
            throw new BusinessException(RestaurantRecommendationErrorCode.INVALID_MEAL_TYPE);
        }
        RestaurantConditionVO condition = getReadyRestaurantCondition(userId, request.getMealType());
        RestaurantReference reference = resolveRestaurantReferenceForCreate(userId, condition, request.getMealType());
        return toCommon(createRestaurantRecommendation(userId, condition, request.getMealType(), reference));
    }

    @Override
    @Transactional
    public RecommendationListResponseDTO<RestaurantRecommendationResponseDTO.Item> findRestaurantRecommendation(Long userId,
                                                                           Long referenceMerchantId,
                                                                           MealType mealType,
                                                                           String cursor,
                                                                           int size) {
        if (mealType == null) {
            throw new BusinessException(RestaurantRecommendationErrorCode.INVALID_MEAL_TYPE);
        }
        RecommendationRequestVO request = recommendationMapper.selectLatestRestaurantRequest(
                userId, referenceMerchantId, mealType);
        if (request == null) {
            RestaurantConditionVO condition = getReadyRestaurantCondition(userId, mealType);
            RestaurantReference reference;

            if (referenceMerchantId == null) {
                reference = resolveAutomaticRestaurantReference(userId, condition, mealType);
            } else {
                RecommendationMerchantVO selected = recommendationMapper.selectRestaurantReferenceMerchant(
                        referenceMerchantId, condition.getRegionId());
                if (selected == null) {
                    throw new BusinessException(RestaurantRecommendationErrorCode.REFERENCE_MERCHANT_NOT_FOUND);
                }
                reference = new RestaurantReference(ReferenceType.USER_SELECTED, selected, null,
                        selected.getLatitude(), selected.getLongitude());
            }

            return toCommon(createRestaurantRecommendation(userId, condition, mealType, reference));
        }
        return toCommon(findRestaurantPage(request, cursor, normalizeSize(size)));
    }

    @Override
    @Transactional(readOnly = true)
    public RestaurantReferenceResponseDTO findRestaurantReferencePlace(Long userId, MealType mealType) {
        if (mealType == null) {
            throw new BusinessException(RestaurantRecommendationErrorCode.INVALID_MEAL_TYPE);
        }
        RestaurantConditionVO condition = getRestaurantCondition(userId);
        RestaurantReference reference = resolveDisplayRestaurantReference(userId, condition, mealType);
        if (reference.type == ReferenceType.REGION_ONLY) {
            return RestaurantReferenceResponseDTO.regionOnly(mealType);
        }
        if (reference.type == ReferenceType.USER_SELECTED) {
            return RestaurantReferenceResponseDTO.userSelected(mealType, reference.primary,
                    reference.latitude, reference.longitude);
        }
        if (reference.type == ReferenceType.AUTO_MIDPOINT) {
            return RestaurantReferenceResponseDTO.midpoint(reference.primary, reference.secondary,
                    reference.latitude, reference.longitude);
        }
        return RestaurantReferenceResponseDTO.merchant(mealType, reference.primary);
    }

    @Override
    @Transactional(readOnly = true)
    public RecommendationCandidateListResponseDTO findRestaurantReferencePlaceCandidates(Long userId) {
        RestaurantConditionVO condition = getRestaurantCondition(userId);
        return new RecommendationCandidateListResponseDTO(recommendationMapper
                .selectRestaurantReferenceCandidates(userId, condition.getWorkationId(), condition.getRegionId())
                .stream().map(RecommendationCandidateResponseDTO::from)
                .collect(Collectors.toList()));
    }

    @Override
    @Transactional
    public RecommendationListResponseDTO<RestaurantRecommendationResponseDTO.Item> recalculateRestaurant(Long userId,
                                                                   Long recommendationRequestId,
                                                                   RecommendationRecalculateRequestDTO request) {
        if (request == null || request.getMealType() == null || request.getReferenceMerchantId() == null) {
            throw new BusinessException(RestaurantRecommendationErrorCode.INVALID_RECOMMENDATION_REQUEST);
        }
        RecommendationRequestVO previous = recommendationMapper.selectRecommendationRequest(recommendationRequestId, userId);
        if (previous == null || previous.getRecommendationType() != RecommendationType.RESTAURANT
                || !previous.getMealType().equals(request.getMealType())) {
            throw new BusinessException(RestaurantRecommendationErrorCode.INVALID_RECOMMENDATION_REQUEST);
        }
        RestaurantConditionVO condition = getReadyRestaurantCondition(userId, request.getMealType());
        if (!condition.getWorkationId().equals(previous.getWorkationId())) {
            throw new BusinessException(RestaurantRecommendationErrorCode.INVALID_RECOMMENDATION_REQUEST);
        }
        RecommendationMerchantVO selected = recommendationMapper.selectRestaurantReferenceMerchant(
                request.getReferenceMerchantId(), condition.getRegionId());
        if (selected == null) {
            throw new BusinessException(RestaurantRecommendationErrorCode.REFERENCE_MERCHANT_NOT_FOUND);
        }
        RestaurantReference reference = new RestaurantReference(ReferenceType.USER_SELECTED, selected, null,
                selected.getLatitude(), selected.getLongitude());
        return toCommon(createRestaurantRecommendation(userId, condition, request.getMealType(), reference));
    }

    private RecommendationListResponseDTO<RestaurantRecommendationResponseDTO.Item> toCommon(
            RestaurantRecommendationResponseDTO response) {
        if (response == null) {
            return RecommendationListResponseDTO.<RestaurantRecommendationResponseDTO.Item>of(
                    null, null, null, null, null, 0, false, null);
        }

        RecommendationListResponseDTO.RecommendationReference reference =
                new RecommendationListResponseDTO.RecommendationReference(
                        response.getReference().getReferenceType() == null ? null : response.getReference().getReferenceType().name(),
                        response.getReference().getMerchantId(),
                        response.getReference().getMerchantName(),
                        response.getReference().getSecondaryMerchantId(),
                        response.getReference().getSecondaryMerchantName(),
                        response.getReference().getLatitude() == null ? null : response.getReference().getLatitude().toPlainString(),
                        response.getReference().getLongitude() == null ? null : response.getReference().getLongitude().toPlainString(),
                        null
                );

        return RecommendationListResponseDTO.of(
                response.getRecommendationRequestId(),
                response.getRecommendationType() == null ? null : response.getRecommendationType().name(),
                response.getMealType() == null ? null : response.getMealType().name(),
                reference,
                response.getContent(),
                response.getPageInfo().getSize(),
                response.getPageInfo().isHasNext(),
                response.getPageInfo().getNextCursor()
        );
    }

    private RestaurantRecommendationResponseDTO createRestaurantRecommendation(Long userId,
                                                                                RestaurantConditionVO condition,
                                                                                MealType mealType,
                                                                                RestaurantReference reference) {
        List<RecommendationMerchantVO> candidates = recommendationMapper.selectRestaurants(condition.getRegionId());
        if (candidates.isEmpty()) {
            throw new BusinessException(RestaurantRecommendationErrorCode.RESTAURANT_CANDIDATE_NOT_FOUND);
        }
        long days = ChronoUnit.DAYS.between(condition.getStartDate(), condition.getEndDate());
        BigDecimal dailyBudget = condition.getFoodBudget().divide(BigDecimal.valueOf(days), 8, RoundingMode.HALF_UP);
        BigDecimal mealBudget = dailyBudget.multiply(BigDecimal.valueOf(resolveMealRatio(condition, mealType)));
        String priority = condition.getPriorityOptionCode() + " " + condition.getPriorityOptionName();
        List<RecommendationResultVO> results = restaurantScoreCalculator.calculate(candidates, mealBudget, priority,
                reference.type, reference.latitude, reference.longitude);

        RecommendationRequestVO request = new RecommendationRequestVO();
        request.setUserId(userId);
        request.setWorkationId(condition.getWorkationId());
        request.setRecommendationType(RecommendationType.RESTAURANT);
        request.setMealType(mealType);
        request.setReferenceType(reference.type);
        request.setReferenceLatitude(reference.latitude);
        request.setReferenceLongitude(reference.longitude);
        if (reference.primary != null) {
            request.setReferenceMerchantId(reference.primary.getMerchantId());
        }
        if (reference.secondary != null) {
            request.setSecondaryReferenceMerchantId(reference.secondary.getMerchantId());
        }
        recommendationMapper.insertRecommendationRequest(request);
        recommendationMapper.insertRecommendationResults(request.getId(), results);
        RecommendationRequestVO saved = recommendationMapper.selectRecommendationRequest(request.getId(), userId);
        return findRestaurantPage(saved, null, DEFAULT_SIZE);
    }

    private RestaurantRecommendationResponseDTO findRestaurantPage(RecommendationRequestVO request,
                                                                    String cursor,
                                                                    int size) {
        Cursor decoded = decodeCursor(cursor);
        List<RecommendationResultVO> fetched = recommendationMapper.selectRecommendationResults(request.getId(),
                decoded.ranking, decoded.id, size + 1);
        boolean hasNext = fetched.size() > size;
        List<RecommendationResultVO> content = hasNext
                ? new ArrayList<>(fetched.subList(0, size)) : fetched;
        String nextCursor = hasNext ? encodeCursor(content.get(content.size() - 1)) : null;
        return RestaurantRecommendationResponseDTO.of(
                request, content, size, hasNext, nextCursor);
    }

    private RestaurantReference resolveAutomaticRestaurantReference(Long userId,
                                                                    RestaurantConditionVO condition,
                                                                    MealType mealType) {
        RecommendationMerchantVO stay = recommendationMapper.selectConfirmedAccommodation(
                userId, condition.getWorkationId());
        RecommendationMerchantVO office = recommendationMapper.selectConfirmedOffice(
                userId, condition.getWorkationId());
        if (mealType == MealType.BREAKFAST) {
            return singleOrRegion(stay);
        }
        if (mealType == MealType.LUNCH) {
            return singleOrRegion(office);
        }
        if (stay != null && office != null) {
            BigDecimal latitude = stay.getLatitude().add(office.getLatitude())
                    .divide(BigDecimal.valueOf(2), 8, RoundingMode.HALF_UP);
            BigDecimal longitude = stay.getLongitude().add(office.getLongitude())
                    .divide(BigDecimal.valueOf(2), 8, RoundingMode.HALF_UP);
            return new RestaurantReference(ReferenceType.AUTO_MIDPOINT, stay, office, latitude, longitude);
        }
        return singleOrRegion(stay != null ? stay : office);
    }

    private RestaurantReference resolveDisplayRestaurantReference(Long userId,
                                                                 RestaurantConditionVO condition,
                                                                 MealType mealType) {
        RecommendationRequestVO latest = recommendationMapper.selectLatestRestaurantRequest(userId, null, null);
        if (latest != null && latest.getReferenceType() == ReferenceType.USER_SELECTED
                && latest.getReferenceMerchantId() != null) {
            RecommendationMerchantVO selected = recommendationMapper.selectRestaurantReferenceMerchant(
                    latest.getReferenceMerchantId(), condition.getRegionId());
            if (selected != null) {
                BigDecimal latitude = latest.getReferenceLatitude() != null
                        ? latest.getReferenceLatitude()
                        : selected.getLatitude();
                BigDecimal longitude = latest.getReferenceLongitude() != null
                    ? latest.getReferenceLongitude()
                    : selected.getLongitude();
                return new RestaurantReference(ReferenceType.USER_SELECTED, selected, null, latitude, longitude);
            }
        }
        if (mealType != null) {
            RecommendationRequestVO mealLatest = recommendationMapper.selectLatestRestaurantRequest(userId, null, mealType);
            if (mealLatest != null && mealLatest.getReferenceType() == ReferenceType.USER_SELECTED
                    && mealLatest.getReferenceMerchantId() != null) {
                RecommendationMerchantVO selected = recommendationMapper.selectRestaurantReferenceMerchant(
                        mealLatest.getReferenceMerchantId(), condition.getRegionId());
                if (selected != null) {
                    BigDecimal latitude = mealLatest.getReferenceLatitude() != null
                            ? mealLatest.getReferenceLatitude()
                            : selected.getLatitude();
                    BigDecimal longitude = mealLatest.getReferenceLongitude() != null
                            ? mealLatest.getReferenceLongitude()
                            : selected.getLongitude();
                    return new RestaurantReference(ReferenceType.USER_SELECTED, selected, null, latitude, longitude);
                }
            }
        }
        return resolveAutomaticRestaurantReference(userId, condition, mealType);
    }

    private RestaurantReference resolveRestaurantReferenceForCreate(Long userId,
                                                                   RestaurantConditionVO condition,
                                                                   MealType mealType) {
        RecommendationRequestVO latest = recommendationMapper.selectLatestRestaurantRequest(userId, null, null);
        if (latest != null && latest.getReferenceType() == ReferenceType.USER_SELECTED
                && latest.getReferenceMerchantId() != null) {
            RecommendationMerchantVO selected = recommendationMapper.selectRestaurantReferenceMerchant(
                    latest.getReferenceMerchantId(), condition.getRegionId());
            if (selected != null) {
                BigDecimal latitude = latest.getReferenceLatitude() != null
                        ? latest.getReferenceLatitude()
                        : selected.getLatitude();
                BigDecimal longitude = latest.getReferenceLongitude() != null
                        ? latest.getReferenceLongitude()
                        : selected.getLongitude();
                return new RestaurantReference(ReferenceType.USER_SELECTED, selected, null, latitude, longitude);
            }
        }
        return resolveAutomaticRestaurantReference(userId, condition, mealType);
    }

    private RestaurantReference singleOrRegion(RecommendationMerchantVO merchant) {
        return merchant == null
                ? new RestaurantReference(ReferenceType.REGION_ONLY, null, null, null, null)
                : new RestaurantReference(ReferenceType.AUTO_MERCHANT, merchant, null,
                merchant.getLatitude(), merchant.getLongitude());
    }

    private RestaurantConditionVO getReadyRestaurantCondition(Long userId, MealType mealType) {
        RestaurantConditionVO condition = getRestaurantCondition(userId);
        long days = ChronoUnit.DAYS.between(condition.getStartDate(), condition.getEndDate());
        if (days <= 0 || condition.getFoodBudget() == null
                || condition.getFoodBudget().compareTo(BigDecimal.ZERO) <= 0
                || (condition.getPriorityOptionCode() == null && condition.getPriorityOptionName() == null)
                || (condition.getMealStyleOptionCode() == null && condition.getMealStyleOptionName() == null)) {
            throw new BusinessException(RestaurantRecommendationErrorCode.RECOMMENDATION_CONDITION_NOT_READY);
        }
        if (mealType == MealType.BREAKFAST && isBreakfastExcluded(condition)) {
            throw new BusinessException(RestaurantRecommendationErrorCode.BREAKFAST_RECOMMENDATION_DISABLED);
        }
        return condition;
    }

    private RestaurantConditionVO getRestaurantCondition(Long userId) {
        RestaurantConditionVO condition = recommendationMapper.selectRestaurantCondition(userId);
        if (condition == null) {
            throw new BusinessException(RestaurantRecommendationErrorCode.WORKATION_NOT_FOUND);
        }
        return condition;
    }

    private double resolveMealRatio(RestaurantConditionVO condition, MealType mealType) {
        String style = mealStyle(condition);
        if (style.contains("EXCLUDE") || style.contains("NO_BREAKFAST") || style.contains("아침 제외")) {
            return mealType == MealType.LUNCH ? 0.40 : 0.60;
        }
        if (style.contains("DINNER") || style.contains("저녁 집중")) {
            if (mealType == MealType.BREAKFAST) return 0.15;
            return mealType == MealType.LUNCH ? 0.30 : 0.55;
        }
        if (mealType == MealType.BREAKFAST) return 0.25;
        return mealType == MealType.LUNCH ? 0.35 : 0.40;
    }

    private boolean isBreakfastExcluded(RestaurantConditionVO condition) {
        String style = mealStyle(condition);
        return style.contains("EXCLUDE") || style.contains("NO_BREAKFAST") || style.contains("아침 제외");
    }

    private String mealStyle(RestaurantConditionVO condition) {
        return (condition.getMealStyleOptionCode() + " " + condition.getMealStyleOptionName()).toUpperCase();
    }

    private int normalizeSize(int size) {
        return size < 1 || size > MAX_SIZE ? DEFAULT_SIZE : size;
    }

    private String encodeCursor(RecommendationResultVO result) {
        String value = result.getRanking() + ":" + result.getRecommendationResultId();
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private Cursor decodeCursor(String cursor) {
        if (cursor == null || cursor.trim().isEmpty()) {
            return new Cursor(null, null);
        }
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] values = decoded.split(":", -1);
            if (values.length != 2) {
                throw new IllegalArgumentException();
            }
            return new Cursor(Integer.valueOf(values[0]), Long.valueOf(values[1]));
        } catch (RuntimeException exception) {
            throw new BusinessException(RestaurantRecommendationErrorCode.INVALID_RECOMMENDATION_REQUEST);
        }
    }

    private static class RestaurantReference {
        private final ReferenceType type;
        private final RecommendationMerchantVO primary;
        private final RecommendationMerchantVO secondary;
        private final BigDecimal latitude;
        private final BigDecimal longitude;

        private RestaurantReference(ReferenceType type, RecommendationMerchantVO primary,
                                   RecommendationMerchantVO secondary, BigDecimal latitude,
                                   BigDecimal longitude) {
            this.type = type;
            this.primary = primary;
            this.secondary = secondary;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }

    private static class Cursor {
        private final Integer ranking;
        private final Long id;

        private Cursor(Integer ranking, Long id) {
            this.ranking = ranking;
            this.id = id;
        }
    }
}
