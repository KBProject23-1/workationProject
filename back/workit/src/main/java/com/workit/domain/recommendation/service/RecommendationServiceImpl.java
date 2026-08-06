package com.workit.domain.recommendation.service;

import com.workit.domain.recommendation.dto.request.RecommendationRecalculateRequestDTO;
import com.workit.domain.recommendation.dto.response.AccommodationRecommendationResponseDTO;
import com.workit.domain.recommendation.dto.response.RecommendationCandidateListResponseDTO;
import com.workit.domain.recommendation.dto.response.RecommendationCandidateResponseDTO;
import com.workit.domain.recommendation.dto.response.RecommendationReferenceResponseDTO;
import com.workit.domain.recommendation.dto.request.RestaurantRecommendationCreateRequestDTO;
import com.workit.domain.recommendation.dto.response.RestaurantRecommendationResponseDTO;
import com.workit.domain.recommendation.dto.response.RestaurantReferenceResponseDTO;
import com.workit.domain.recommendation.enums.MealType;
import com.workit.domain.recommendation.enums.RecommendationType;
import com.workit.domain.recommendation.enums.ReferenceType;
import com.workit.domain.recommendation.exception.RecommendationErrorCode;
import com.workit.domain.recommendation.mapper.RecommendationMapper;
import com.workit.domain.recommendation.vo.AccommodationConditionVO;
import com.workit.domain.recommendation.vo.RecommendationMerchantVO;
import com.workit.domain.recommendation.vo.RecommendationRequestVO;
import com.workit.domain.recommendation.vo.RecommendationResultVO;
import com.workit.domain.recommendation.vo.RestaurantConditionVO;
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
public class RecommendationServiceImpl implements RecommendationService {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    private final RecommendationMapper recommendationMapper;
    private final AccommodationScoreCalculator scoreCalculator;
    private final RestaurantScoreCalculator restaurantScoreCalculator;

    @Override
    @Transactional
    public AccommodationRecommendationResponseDTO addAccommodationRecommendation(Long userId) {
        AccommodationConditionVO condition = getReadyCondition(userId);
        RecommendationMerchantVO reference = recommendationMapper.selectConfirmedOffice(userId,
                condition.getWorkationId());
        ReferenceType referenceType = reference == null ? ReferenceType.REGION_ONLY : ReferenceType.AUTO_MERCHANT;
        return createRecommendation(userId, condition, reference, referenceType);
    }

    @Override
    @Transactional(readOnly = true)
    public AccommodationRecommendationResponseDTO findAccommodationRecommendation(Long userId,
                                                                                    Long referenceMerchantId,
                                                                                    String cursor,
                                                                                    int size) {
        RecommendationRequestVO request = recommendationMapper.selectLatestAccommodationRequest(userId,
                referenceMerchantId);
        if (request == null) {
            throw new BusinessException(RecommendationErrorCode.INVALID_RECOMMENDATION_REQUEST);
        }
        return findPage(request, cursor, normalizeSize(size));
    }

    @Override
    @Transactional(readOnly = true)
    public RecommendationReferenceResponseDTO findReferencePlace(Long userId,
                                                                  RecommendationType recommendationType) {
        if (recommendationType != RecommendationType.ACCOMMODATION) {
            throw new BusinessException(RecommendationErrorCode.INVALID_RECOMMENDATION_REQUEST);
        }
        AccommodationConditionVO condition = getCondition(userId);
        RecommendationMerchantVO reference = recommendationMapper.selectConfirmedOffice(userId,
                condition.getWorkationId());
        return RecommendationReferenceResponseDTO.automatic(reference);
    }

    @Override
    @Transactional(readOnly = true)
    public RecommendationCandidateListResponseDTO findReferencePlaceCandidates(Long userId) {
        AccommodationConditionVO condition = getCondition(userId);
        List<RecommendationCandidateResponseDTO> content = recommendationMapper
                .selectReferenceCandidates(userId, condition.getWorkationId(), condition.getRegionId())
                .stream().map(RecommendationCandidateResponseDTO::from).collect(Collectors.toList());
        return new RecommendationCandidateListResponseDTO(content);
    }

    @Override
    @Transactional
    public AccommodationRecommendationResponseDTO recalculateAccommodation(Long userId,
                                                                            Long recommendationRequestId,
                                                                            RecommendationRecalculateRequestDTO request) {
        if (request == null || request.getReferenceMerchantId() == null) {
            throw new BusinessException(RecommendationErrorCode.INVALID_RECOMMENDATION_REQUEST);
        }
        RecommendationRequestVO previous = recommendationMapper.selectRecommendationRequest(
                recommendationRequestId, userId);
        if (previous == null || previous.getRecommendationType() != RecommendationType.ACCOMMODATION) {
            throw new BusinessException(RecommendationErrorCode.INVALID_RECOMMENDATION_REQUEST);
        }
        AccommodationConditionVO condition = getReadyCondition(userId);
        if (!condition.getWorkationId().equals(previous.getWorkationId())) {
            throw new BusinessException(RecommendationErrorCode.INVALID_RECOMMENDATION_REQUEST);
        }
        RecommendationMerchantVO reference = recommendationMapper.selectOfficeInRegion(
                request.getReferenceMerchantId(), condition.getRegionId());
        if (reference == null) {
            throw new BusinessException(RecommendationErrorCode.REFERENCE_MERCHANT_NOT_FOUND);
        }
        return createRecommendation(userId, condition, reference, ReferenceType.USER_SELECTED);
    }

    @Override
    @Transactional
    public RestaurantRecommendationResponseDTO addRestaurantRecommendation(
            Long userId, RestaurantRecommendationCreateRequestDTO request) {
        if (request == null || request.getMealType() == null) {
            throw new BusinessException(RecommendationErrorCode.INVALID_MEAL_TYPE);
        }
        RestaurantConditionVO condition = getReadyRestaurantCondition(userId, request.getMealType());
        RestaurantReference reference = resolveAutomaticRestaurantReference(userId, condition, request.getMealType());
        return createRestaurantRecommendation(userId, condition, request.getMealType(), reference);
    }

    @Override
    @Transactional(readOnly = true)
    public RestaurantRecommendationResponseDTO findRestaurantRecommendation(Long userId,
                                                                             Long referenceMerchantId,
                                                                             MealType mealType,
                                                                             String cursor,
                                                                             int size) {
        if (mealType == null) {
            throw new BusinessException(RecommendationErrorCode.INVALID_MEAL_TYPE);
        }
        RecommendationRequestVO request = recommendationMapper.selectLatestRestaurantRequest(
                userId, referenceMerchantId, mealType);
        if (request == null) {
            throw new BusinessException(RecommendationErrorCode.INVALID_RECOMMENDATION_REQUEST);
        }
        return findRestaurantPage(request, cursor, normalizeSize(size));
    }

    @Override
    @Transactional(readOnly = true)
    public RestaurantReferenceResponseDTO findRestaurantReferencePlace(Long userId, MealType mealType) {
        if (mealType == null) {
            throw new BusinessException(RecommendationErrorCode.INVALID_MEAL_TYPE);
        }
        RestaurantConditionVO condition = getRestaurantCondition(userId);
        RestaurantReference reference = resolveAutomaticRestaurantReference(userId, condition, mealType);
        if (reference.type == ReferenceType.REGION_ONLY) {
            return RestaurantReferenceResponseDTO.regionOnly(mealType);
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
        List<RecommendationCandidateResponseDTO> content = recommendationMapper
                .selectRestaurantReferenceCandidates(userId, condition.getWorkationId(), condition.getRegionId())
                .stream().map(RecommendationCandidateResponseDTO::from).collect(Collectors.toList());
        return new RecommendationCandidateListResponseDTO(content);
    }

    @Override
    @Transactional
    public Object recalculateRecommendation(Long userId, Long recommendationRequestId,
                                            RecommendationRecalculateRequestDTO request) {
        RecommendationRequestVO previous = recommendationMapper.selectRecommendationRequest(
                recommendationRequestId, userId);
        if (previous == null) {
            throw new BusinessException(RecommendationErrorCode.INVALID_RECOMMENDATION_REQUEST);
        }
        if (previous.getRecommendationType() == RecommendationType.ACCOMMODATION) {
            return recalculateAccommodation(userId, recommendationRequestId, request);
        }
        if (previous.getRecommendationType() != RecommendationType.RESTAURANT
                || request == null || request.getReferenceMerchantId() == null || request.getMealType() == null) {
            throw new BusinessException(RecommendationErrorCode.INVALID_RECOMMENDATION_REQUEST);
        }
        RestaurantConditionVO condition = getReadyRestaurantCondition(userId, request.getMealType());
        if (!condition.getWorkationId().equals(previous.getWorkationId())) {
            throw new BusinessException(RecommendationErrorCode.INVALID_RECOMMENDATION_REQUEST);
        }
        RecommendationMerchantVO selected = recommendationMapper.selectRestaurantReferenceMerchant(
                request.getReferenceMerchantId(), condition.getRegionId());
        if (selected == null) {
            throw new BusinessException(RecommendationErrorCode.REFERENCE_MERCHANT_NOT_FOUND);
        }
        RestaurantReference reference = new RestaurantReference(ReferenceType.USER_SELECTED, selected, null,
                selected.getLatitude(), selected.getLongitude());
        return createRestaurantRecommendation(userId, condition, request.getMealType(), reference);
    }

    private RestaurantRecommendationResponseDTO createRestaurantRecommendation(Long userId,
                                                                                RestaurantConditionVO condition,
                                                                                MealType mealType,
                                                                                RestaurantReference reference) {
        List<RecommendationMerchantVO> candidates = recommendationMapper.selectRestaurants(condition.getRegionId());
        if (candidates.isEmpty()) {
            throw new BusinessException(RecommendationErrorCode.RESTAURANT_CANDIDATE_NOT_FOUND);
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
        return RestaurantRecommendationResponseDTO.of(request, content, size, hasNext, nextCursor);
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
            throw new BusinessException(RecommendationErrorCode.RECOMMENDATION_CONDITION_NOT_READY);
        }
        if (mealType == MealType.BREAKFAST && isBreakfastExcluded(condition)) {
            throw new BusinessException(RecommendationErrorCode.BREAKFAST_RECOMMENDATION_DISABLED);
        }
        return condition;
    }

    private RestaurantConditionVO getRestaurantCondition(Long userId) {
        RestaurantConditionVO condition = recommendationMapper.selectRestaurantCondition(userId);
        if (condition == null) {
            throw new BusinessException(RecommendationErrorCode.WORKATION_NOT_FOUND);
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

    private AccommodationRecommendationResponseDTO createRecommendation(Long userId,
                                                                         AccommodationConditionVO condition,
                                                                         RecommendationMerchantVO reference,
                                                                         ReferenceType referenceType) {
        List<RecommendationMerchantVO> candidates = recommendationMapper.selectAvailableAccommodations(condition);
        if (candidates.isEmpty()) {
            throw new BusinessException(RecommendationErrorCode.ACCOMMODATION_CANDIDATE_NOT_FOUND);
        }

        long nights = ChronoUnit.DAYS.between(condition.getStartDate(), condition.getEndDate());
        BigDecimal nightlyBudget = condition.getAccommodationBudget()
                .divide(BigDecimal.valueOf(nights), 8, RoundingMode.HALF_UP);
        String priority = condition.getPriorityOptionCode() + " " + condition.getPriorityOptionName();
        List<RecommendationResultVO> results = scoreCalculator.calculate(candidates, nightlyBudget, priority,
                referenceType, reference == null ? null : reference.getLatitude(),
                reference == null ? null : reference.getLongitude());

        RecommendationRequestVO request = new RecommendationRequestVO();
        request.setUserId(userId);
        request.setWorkationId(condition.getWorkationId());
        request.setRecommendationType(RecommendationType.ACCOMMODATION);
        request.setReferenceType(referenceType);
        if (reference != null) {
            request.setReferenceMerchantId(reference.getMerchantId());
            request.setReferenceLatitude(reference.getLatitude());
            request.setReferenceLongitude(reference.getLongitude());
        }
        recommendationMapper.insertRecommendationRequest(request);
        recommendationMapper.insertRecommendationResults(request.getId(), results);

        RecommendationRequestVO saved = recommendationMapper.selectRecommendationRequest(request.getId(), userId);
        return findPage(saved, null, DEFAULT_SIZE);
    }

    private AccommodationRecommendationResponseDTO findPage(RecommendationRequestVO request,
                                                              String cursor,
                                                              int size) {
        Cursor decoded = decodeCursor(cursor);
        List<RecommendationResultVO> fetched = recommendationMapper.selectRecommendationResults(request.getId(),
                decoded.ranking, decoded.id, size + 1);
        boolean hasNext = fetched.size() > size;
        List<RecommendationResultVO> content = hasNext
                ? new ArrayList<>(fetched.subList(0, size)) : fetched;
        String nextCursor = hasNext ? encodeCursor(content.get(content.size() - 1)) : null;
        return AccommodationRecommendationResponseDTO.of(request, content, size, hasNext, nextCursor);
    }

    private AccommodationConditionVO getReadyCondition(Long userId) {
        AccommodationConditionVO condition = getCondition(userId);
        long nights = ChronoUnit.DAYS.between(condition.getStartDate(), condition.getEndDate());
        if (nights <= 0 || condition.getAccommodationBudget() == null
                || condition.getAccommodationBudget().compareTo(BigDecimal.ZERO) <= 0
                || (condition.getPriorityOptionCode() == null && condition.getPriorityOptionName() == null)) {
            throw new BusinessException(RecommendationErrorCode.RECOMMENDATION_CONDITION_NOT_READY);
        }
        return condition;
    }

    private AccommodationConditionVO getCondition(Long userId) {
        AccommodationConditionVO condition = recommendationMapper.selectAccommodationCondition(userId);
        if (condition == null) {
            throw new BusinessException(RecommendationErrorCode.WORKATION_NOT_FOUND);
        }
        return condition;
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
            throw new BusinessException(RecommendationErrorCode.INVALID_RECOMMENDATION_REQUEST);
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
