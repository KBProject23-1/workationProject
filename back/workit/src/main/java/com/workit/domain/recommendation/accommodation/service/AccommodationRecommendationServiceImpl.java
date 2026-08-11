package com.workit.domain.recommendation.accommodation.service;

import com.workit.domain.recommendation.accommodation.dto.request.RecommendationRecalculateRequestDTO;
import com.workit.domain.recommendation.common.dto.response.RecommendationListResponseDTO;
import com.workit.domain.recommendation.accommodation.dto.response.AccommodationRecommendationResponseDTO;
import com.workit.domain.recommendation.accommodation.dto.response.RecommendationCandidateListResponseDTO;
import com.workit.domain.recommendation.accommodation.dto.response.RecommendationCandidateResponseDTO;
import com.workit.domain.recommendation.accommodation.dto.response.RecommendationReferenceResponseDTO;
import com.workit.domain.recommendation.accommodation.exception.AccommodationRecommendationErrorCode;
import com.workit.domain.recommendation.accommodation.mapper.AccommodationRecommendationMapper;
import com.workit.domain.recommendation.accommodation.vo.AccommodationConditionVO;
import com.workit.domain.recommendation.accommodation.vo.RecommendationMerchantVO;
import com.workit.domain.recommendation.accommodation.vo.RecommendationRequestVO;
import com.workit.domain.recommendation.accommodation.vo.RecommendationResultVO;
import com.workit.domain.recommendation.accommodation.vo.ReferenceType;
import com.workit.domain.recommendation.accommodation.vo.RecommendationType;
import com.workit.domain.recommendation.common.mapper.ReservationProductAvailabilityMapper;
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
public class AccommodationRecommendationServiceImpl implements AccommodationRecommendationService {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    private final AccommodationRecommendationMapper recommendationMapper;
    private final ReservationProductAvailabilityMapper reservationProductAvailabilityMapper;
    private final AccommodationScoreCalculator scoreCalculator;

    @Override
    @Transactional(readOnly = true)
    public RecommendationReferenceResponseDTO findAccommodationReferencePlace(Long userId) {
        AccommodationConditionVO condition = getCondition(userId);
        RecommendationMerchantVO reference = recommendationMapper.selectConfirmedOffice(userId, condition.getWorkationId());
        return RecommendationReferenceResponseDTO.automatic(reference);
    }

    @Override
    @Transactional(readOnly = true)
    public RecommendationCandidateListResponseDTO findAccommodationReferencePlaceCandidates(Long userId) {
        AccommodationConditionVO condition = getCondition(userId);
        return new RecommendationCandidateListResponseDTO(
                recommendationMapper.selectReferenceCandidates(userId, condition.getWorkationId(), condition.getRegionId())
                        .stream().map(RecommendationCandidateResponseDTO::from)
                        .collect(Collectors.toList())
        );
    }

    @Override
    @Transactional
    public RecommendationListResponseDTO<AccommodationRecommendationResponseDTO.Item> addAccommodationRecommendation(Long userId) {
        AccommodationConditionVO condition = getReadyCondition(userId);
        RecommendationMerchantVO reference = recommendationMapper.selectConfirmedOffice(userId,
                condition.getWorkationId());
        ReferenceType referenceType = reference == null ? ReferenceType.REGION_ONLY : ReferenceType.AUTO_MERCHANT;
        return toCommon(createRecommendation(userId, condition, reference, referenceType));
    }

    @Override
    @Transactional(readOnly = true)
    public RecommendationListResponseDTO<AccommodationRecommendationResponseDTO.Item> findAccommodationRecommendation(Long userId,
                                                                                Long referenceMerchantId,
                                                                                String cursor,
                                                                                int size) {
        RecommendationRequestVO request = recommendationMapper.selectLatestAccommodationRequest(userId,
                referenceMerchantId);
        if (request == null) {
            throw new BusinessException(AccommodationRecommendationErrorCode.INVALID_RECOMMENDATION_REQUEST);
        }
        return toCommon(findPage(request, cursor, normalizeSize(size)));
    }

    @Override
    @Transactional
    public RecommendationListResponseDTO<AccommodationRecommendationResponseDTO.Item> recalculateAccommodation(Long userId,
                                                                           Long recommendationRequestId,
                                                                           RecommendationRecalculateRequestDTO request) {
        if (request == null || request.getReferenceMerchantId() == null) {
            throw new BusinessException(AccommodationRecommendationErrorCode.INVALID_RECOMMENDATION_REQUEST);
        }
        RecommendationRequestVO previous = recommendationMapper.selectRecommendationRequest(
                recommendationRequestId, userId);
        if (previous == null || previous.getRecommendationType() != RecommendationType.ACCOMMODATION) {
            throw new BusinessException(AccommodationRecommendationErrorCode.INVALID_RECOMMENDATION_REQUEST);
        }
        AccommodationConditionVO condition = getReadyCondition(userId);
        if (!condition.getWorkationId().equals(previous.getWorkationId())) {
            throw new BusinessException(AccommodationRecommendationErrorCode.INVALID_RECOMMENDATION_REQUEST);
        }
        RecommendationMerchantVO reference = recommendationMapper.selectOfficeInRegion(
                request.getReferenceMerchantId(), condition.getRegionId());
        if (reference == null) {
            throw new BusinessException(AccommodationRecommendationErrorCode.REFERENCE_MERCHANT_NOT_FOUND);
        }
        return toCommon(createRecommendation(userId, condition, reference, ReferenceType.USER_SELECTED));
    }

    private RecommendationListResponseDTO<AccommodationRecommendationResponseDTO.Item> toCommon(
            AccommodationRecommendationResponseDTO response) {
        if (response == null) {
            return RecommendationListResponseDTO.<AccommodationRecommendationResponseDTO.Item>of(
                    null, null, null, null, null, 0, false, null);
        }
        RecommendationListResponseDTO.RecommendationReference reference =
                new RecommendationListResponseDTO.RecommendationReference(
                        response.getReference().getReferenceType() == null ? null : response.getReference().getReferenceType().name(),
                        response.getReference().getMerchantId(),
                        response.getReference().getMerchantName(),
                        null,
                        null,
                        response.getReference().getLatitude() == null ? null : response.getReference().getLatitude().toPlainString(),
                        response.getReference().getLongitude() == null ? null : response.getReference().getLongitude().toPlainString(),
                        null
                );

        return RecommendationListResponseDTO.of(
                response.getRecommendationRequestId(),
                response.getRecommendationType() == null ? null : response.getRecommendationType().name(),
                null,
                reference,
                response.getContent(),
                response.getPageInfo().getSize(),
                response.getPageInfo().isHasNext(),
                response.getPageInfo().getNextCursor()
        );
    }

    private AccommodationRecommendationResponseDTO createRecommendation(Long userId,
                                                                     AccommodationConditionVO condition,
                                                                     RecommendationMerchantVO reference,
                                                                     ReferenceType referenceType) {
        List<Long> availableMerchantIds = reservationProductAvailabilityMapper.selectAvailableMerchantIdsByPeriod(
                condition.getRegionId(),
                "ACCOMMODATION",
                "ROOM",
                condition.getStartDate(),
                condition.getEndDate(),
                false,
                (int) ChronoUnit.DAYS.between(condition.getStartDate(), condition.getEndDate())
        );

        if (availableMerchantIds == null || availableMerchantIds.isEmpty()) {
            throw new BusinessException(AccommodationRecommendationErrorCode.ACCOMMODATION_CANDIDATE_NOT_FOUND);
        }

        List<RecommendationMerchantVO> candidates = recommendationMapper.selectAvailableAccommodationsByMerchantIds(
                availableMerchantIds
        );
        if (candidates.isEmpty()) {
            throw new BusinessException(AccommodationRecommendationErrorCode.ACCOMMODATION_CANDIDATE_NOT_FOUND);
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
            throw new BusinessException(AccommodationRecommendationErrorCode.RECOMMENDATION_CONDITION_NOT_READY);
        }
        return condition;
    }

    private AccommodationConditionVO getCondition(Long userId) {
        AccommodationConditionVO condition = recommendationMapper.selectAccommodationCondition(userId);
        if (condition == null) {
            throw new BusinessException(AccommodationRecommendationErrorCode.WORKATION_NOT_FOUND);
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
            throw new BusinessException(AccommodationRecommendationErrorCode.INVALID_RECOMMENDATION_REQUEST);
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
