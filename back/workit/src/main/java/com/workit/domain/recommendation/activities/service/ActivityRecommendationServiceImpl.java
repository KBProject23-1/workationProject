package com.workit.domain.recommendation.activities.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workit.domain.recommendation.activities.dto.request.ActivityRecommendationCreateRequestDTO;
import com.workit.domain.recommendation.activities.dto.response.ActivityRecommendationResponseDTO;
import com.workit.domain.recommendation.activities.vo.ActivityCandidateVO;
import com.workit.domain.recommendation.activities.vo.ActivityConditionVO;
import com.workit.domain.recommendation.activities.exception.ActivityRecommendationErrorCode;
import com.workit.domain.recommendation.activities.mapper.ActivityRecommendationMapper;
import com.workit.domain.recommendation.activities.vo.ActivityRecommendationResultVO;
import com.workit.domain.recommendation.dto.request.RecommendationRecalculateRequestDTO;
import com.workit.domain.recommendation.enums.RecommendationType;
import com.workit.domain.recommendation.enums.ReferenceType;
import com.workit.domain.recommendation.exception.RecommendationErrorCode;
import com.workit.domain.recommendation.mapper.RecommendationMapper;
import com.workit.domain.recommendation.vo.RecommendationMerchantVO;
import com.workit.domain.recommendation.vo.RecommendationRequestVO;
import com.workit.domain.workation.mapper.WorkationMapper;
import com.workit.domain.workation.vo.WorkationVO;
import com.workit.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ActivityRecommendationServiceImpl implements ActivityRecommendationService {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;
    private static final String ACTIVITY_CURSOR_RANKING_KEY = "ranking";
    private static final String ACTIVITY_CURSOR_RESULT_ID_KEY = "recommendationResultId";
    private static final String ACTIVITY_CURSOR_ID_KEY = "id";
    private static final ObjectMapper ACTIVITY_CURSOR_OBJECT_MAPPER = new ObjectMapper();

    private final RecommendationMapper recommendationMapper;
    private final ActivityRecommendationMapper activityRecommendationMapper;
    private final WorkationMapper workationMapper;
    private final ActivityScoreCalculator activityScoreCalculator;

    @Override
    @Transactional
    public ActivityRecommendationResponseDTO addActivityRecommendation(Long userId,
                                                                      ActivityRecommendationCreateRequestDTO request) {
        if (request == null || request.getWorkationId() == null) {
            throw new BusinessException(RecommendationErrorCode.INVALID_RECOMMENDATION_REQUEST);
        }
        ActivityConditionVO condition = getReadyActivityCondition(userId, request.getWorkationId());
        RecommendationMerchantVO reference = recommendationMapper.selectConfirmedAccommodation(
                userId, condition.getWorkationId());
        return generateActivityRecommendation(
                userId,
                condition,
                reference,
                parseSelectedActivityCodes(condition),
                normalizeSize(request.getSize() == null ? DEFAULT_SIZE : request.getSize())
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ActivityRecommendationResponseDTO findActivityRecommendation(Long userId,
                                                                       Long referenceMerchantId,
                                                                       String cursor,
                                                                       int size) {
        WorkationVO workation = workationMapper.selectActiveWorkation(userId);
        if (workation == null) {
            throw new BusinessException(RecommendationErrorCode.WORKATION_NOT_FOUND);
        }

        int safeSize = normalizeSize(size);
        ActivityConditionVO condition = getReadyActivityCondition(userId, workation.getId());

        if (referenceMerchantId != null) {
            RecommendationMerchantVO selected = activityRecommendationMapper.selectActivityReferenceMerchant(
                    referenceMerchantId, condition.getRegionId());
            if (selected == null) {
                throw new BusinessException(ActivityRecommendationErrorCode.ACTIVITY_REFERENCE_MERCHANT_NOT_FOUND);
            }
            RecommendationRequestVO request = activityRecommendationMapper.selectLatestActivityRequest(
                    userId, workation.getId(), selected.getMerchantId());
            if (request != null && request.getReferenceType() == ReferenceType.USER_SELECTED
                    && request.getReferenceMerchantId() != null) {
                return findActivityPage(request, cursor, safeSize);
            }
            return createActivityRecommendation(
                    userId,
                    condition,
                    ReferenceType.USER_SELECTED,
                    selected.getLatitude(),
                    selected.getLongitude(),
                    selected,
                    parseSelectedActivityCodes(condition),
                    safeSize
            );
        }

        RecommendationMerchantVO reference = recommendationMapper.selectConfirmedAccommodation(
                userId, condition.getWorkationId());
        if (reference != null) {
            RecommendationRequestVO request = activityRecommendationMapper.selectLatestActivityRequest(
                    userId, workation.getId(), reference.getMerchantId());
            if (request != null && request.getReferenceType() == ReferenceType.AUTO_MERCHANT
                    && reference.getMerchantId().equals(request.getReferenceMerchantId())) {
                return findActivityPage(request, cursor, safeSize);
            }
            return createActivityRecommendation(
                    userId,
                    condition,
                    ReferenceType.AUTO_MERCHANT,
                    reference.getLatitude(),
                    reference.getLongitude(),
                    reference,
                    parseSelectedActivityCodes(condition),
                    safeSize
            );
        }

        RecommendationRequestVO request = activityRecommendationMapper.selectLatestActivityRequest(
                userId, workation.getId(), null);
        if (request != null && request.getReferenceMerchantId() == null
                && request.getReferenceType() == ReferenceType.REGION_ONLY) {
            return findActivityPage(request, cursor, safeSize);
        }
        return generateActivityRecommendation(
                userId,
                condition,
                null,
                parseSelectedActivityCodes(condition),
                safeSize
        );
    }

    @Override
    @Transactional
    public ActivityRecommendationResponseDTO recalculateActivityRecommendation(Long userId,
                                                                              Long recommendationRequestId,
                                                                              RecommendationRecalculateRequestDTO request) {
        if (recommendationRequestId == null || request == null || request.getReferenceMerchantId() == null) {
            throw new BusinessException(RecommendationErrorCode.INVALID_RECOMMENDATION_REQUEST);
        }
        RecommendationRequestVO previous = recommendationMapper.selectRecommendationRequest(
                recommendationRequestId, userId);
        if (previous == null) {
            throw new BusinessException(RecommendationErrorCode.INVALID_RECOMMENDATION_REQUEST);
        }
        if (previous.getRecommendationType() != RecommendationType.ACTIVITY) {
            throw new BusinessException(RecommendationErrorCode.INVALID_RECOMMENDATION_REQUEST);
        }
        ActivityConditionVO condition = getReadyActivityCondition(userId, previous.getWorkationId());
        RecommendationMerchantVO selected = activityRecommendationMapper.selectActivityReferenceMerchant(
                request.getReferenceMerchantId(),
                condition.getRegionId());
        if (selected == null) {
            throw new BusinessException(ActivityRecommendationErrorCode.ACTIVITY_REFERENCE_MERCHANT_NOT_FOUND);
        }
        return createActivityRecommendation(userId, condition, ReferenceType.USER_SELECTED,
                selected.getLatitude(), selected.getLongitude(), selected,
                parseSelectedActivityCodes(condition), DEFAULT_SIZE);
    }

    private ActivityRecommendationResponseDTO generateActivityRecommendation(Long userId,
                                                                           ActivityConditionVO condition,
                                                                           RecommendationMerchantVO reference,
                                                                           Set<String> selectedActivities,
                                                                           int size) {
        if (condition == null) {
            throw new BusinessException(RecommendationErrorCode.RECOMMENDATION_CONDITION_NOT_READY);
        }
        ReferenceType referenceType = reference == null ? ReferenceType.REGION_ONLY : ReferenceType.AUTO_MERCHANT;
        return createActivityRecommendation(
                userId,
                condition,
                referenceType,
                reference == null ? null : reference.getLatitude(),
                reference == null ? null : reference.getLongitude(),
                reference,
                selectedActivities,
                size
        );
    }

    private ActivityRecommendationResponseDTO createActivityRecommendation(Long userId,
                                                                          ActivityConditionVO condition,
                                                                          ReferenceType referenceType,
                                                                          BigDecimal referenceLatitude,
                                                                          BigDecimal referenceLongitude,
                                                                          RecommendationMerchantVO referenceMerchant,
                                                                          Set<String> selectedActivities,
                                                                          int size) {
        List<ActivityCandidateVO> candidates =
                activityRecommendationMapper.selectActivities(condition.getRegionId());
        if (candidates.isEmpty()) {
            throw new BusinessException(ActivityRecommendationErrorCode.ACTIVITY_CANDIDATE_NOT_FOUND);
        }
        boolean hasPreference = hasPreference(selectedActivities);
        String priority = condition.getPriorityOptionCode() + " " + condition.getPriorityOptionName();
        List<ActivityRecommendationResultVO> results = activityScoreCalculator.calculate(
                candidates.stream().map(candidate -> (RecommendationMerchantVO) candidate).collect(Collectors.toList()),
                condition.getLeisureBudget(),
                priority,
                referenceType,
                referenceLatitude,
                referenceLongitude,
                selectedActivities,
                hasPreference
        );

        RecommendationRequestVO request = new RecommendationRequestVO();
        request.setUserId(userId);
        request.setWorkationId(condition.getWorkationId());
        request.setRecommendationType(RecommendationType.ACTIVITY);
        request.setReferenceType(referenceType);
        request.setReferenceLatitude(referenceLatitude);
        request.setReferenceLongitude(referenceLongitude);
        if (referenceMerchant != null) {
            request.setReferenceMerchantId(referenceMerchant.getMerchantId());
        }
        recommendationMapper.insertRecommendationRequest(request);
        activityRecommendationMapper.insertRecommendationResults(request.getId(), results);
        RecommendationRequestVO saved = recommendationMapper.selectRecommendationRequest(request.getId(), userId);
        return findActivityPage(saved, null, size);
    }

    private ActivityRecommendationResponseDTO findActivityPage(RecommendationRequestVO request,
                                                              String cursor,
                                                              int size) {
        Cursor decoded = decodeActivityCursor(cursor);
        List<ActivityRecommendationResultVO> fetched = activityRecommendationMapper.selectRecommendationResults(request.getId(),
                decoded.ranking, decoded.id, size + 1);
        boolean hasNext = fetched.size() > size;
        List<ActivityRecommendationResultVO> content = hasNext
                ? new ArrayList<>(fetched.subList(0, size)) : fetched;
        String nextCursor = hasNext ? encodeActivityCursor(content.get(content.size() - 1)) : null;
        return ActivityRecommendationResponseDTO.of(request, content, size, hasNext, nextCursor);
    }

    private ActivityConditionVO getReadyActivityCondition(Long userId, Long workationId) {
        ActivityConditionVO condition = getActivityCondition(userId, workationId);
        long days = ChronoUnit.DAYS.between(condition.getStartDate(), condition.getEndDate());
        if (days <= 0 || condition.getLeisureBudget() == null
                || condition.getLeisureBudget().compareTo(BigDecimal.ZERO) <= 0
                || (condition.getPriorityOptionCode() == null && condition.getPriorityOptionName() == null)) {
            throw new BusinessException(RecommendationErrorCode.RECOMMENDATION_CONDITION_NOT_READY);
        }
        return condition;
    }

    private ActivityConditionVO getActivityCondition(Long userId, Long workationId) {
        ActivityConditionVO condition = activityRecommendationMapper.selectActivityCondition(userId, workationId);
        if (condition == null) {
            throw new BusinessException(RecommendationErrorCode.WORKATION_NOT_FOUND);
        }
        return condition;
    }

    private Set<String> parseSelectedActivityCodes(ActivityConditionVO condition) {
        if (condition == null || condition.getSelectedActivityCodes() == null) {
            return new HashSet<>();
        }
        return Arrays.stream(condition.getSelectedActivityCodes().split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toSet());
    }

    private boolean hasPreference(Set<String> selectedActivities) {
        if (selectedActivities == null || selectedActivities.isEmpty()) {
            return false;
        }
        for (String activity : selectedActivities) {
            String normalized = normalize(activity);
            if (normalized == null || normalized.trim().isEmpty()) {
                continue;
            }
            if (normalized.equals("특별히없음") || normalized.equals("NONE") || normalized.equals("NO_PREFERENCE")) {
                continue;
            }
            return true;
        }
        return false;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase().replaceAll("\\s+", "");
    }

    private int normalizeSize(int size) {
        return size < 1 || size > MAX_SIZE ? DEFAULT_SIZE : size;
    }

    private String encodeActivityCursor(ActivityRecommendationResultVO result) {
        try {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(
                    ACTIVITY_CURSOR_OBJECT_MAPPER.writeValueAsBytes(
                            new ActivityCursor(result.getRanking(), result.getRecommendationResultId())
                    )
            );
        } catch (JsonProcessingException exception) {
            throw new BusinessException(RecommendationErrorCode.INVALID_RECOMMENDATION_REQUEST);
        }
    }

    private Cursor decodeActivityCursor(String cursor) {
        if (cursor == null || cursor.trim().isEmpty()) {
            return new Cursor(null, null);
        }
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            try {
                java.util.Map<String, Object> payload = ACTIVITY_CURSOR_OBJECT_MAPPER.readValue(decoded,
                        new TypeReference<java.util.Map<String, Object>>() {
                        });
                Integer ranking = payload.get(ACTIVITY_CURSOR_RANKING_KEY) == null
                        ? null
                        : ((Number) payload.get(ACTIVITY_CURSOR_RANKING_KEY)).intValue();
                Long recommendationResultId = payload.get(ACTIVITY_CURSOR_RESULT_ID_KEY) == null
                        ? (payload.get(ACTIVITY_CURSOR_ID_KEY) == null
                        ? null
                        : ((Number) payload.get(ACTIVITY_CURSOR_ID_KEY)).longValue())
                        : ((Number) payload.get(ACTIVITY_CURSOR_RESULT_ID_KEY)).longValue();
                return new Cursor(ranking, recommendationResultId);
            } catch (JsonProcessingException ex) {
                String[] values = decoded.split(":", -1);
                if (values.length != 2) {
                    throw new IllegalArgumentException(ex);
                }
                return new Cursor(Integer.valueOf(values[0]), Long.valueOf(values[1]));
            }
        } catch (RuntimeException exception) {
            throw new BusinessException(RecommendationErrorCode.INVALID_RECOMMENDATION_REQUEST);
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

    private static class ActivityCursor {
        private final Integer ranking;
        private final Long recommendationResultId;

        private ActivityCursor(Integer ranking, Long recommendationResultId) {
            this.ranking = ranking;
            this.recommendationResultId = recommendationResultId;
        }

        public Integer getRanking() {
            return ranking;
        }

        public Long getRecommendationResultId() {
            return recommendationResultId;
        }
    }
}
