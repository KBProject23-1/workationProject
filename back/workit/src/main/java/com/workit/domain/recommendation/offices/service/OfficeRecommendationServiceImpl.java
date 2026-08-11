package com.workit.domain.recommendation.offices.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.workit.domain.budget.mapper.BudgetMapper;
import com.workit.domain.recommendation.offices.dto.request.OfficeRecommendationCreateRequestDTO;
import com.workit.domain.recommendation.offices.dto.request.OfficeRecommendationRecalculateRequestDTO;
import com.workit.domain.recommendation.offices.dto.response.OfficeRecommendationPageInfoDTO;
import com.workit.domain.recommendation.offices.dto.response.OfficeRecommendationReferenceResponseDTO;
import com.workit.domain.recommendation.offices.dto.response.OfficeRecommendationResponseDTO;
import com.workit.domain.recommendation.offices.dto.response.OfficeRecommendationCandidateListResponseDTO;
import com.workit.domain.recommendation.offices.dto.response.OfficeRecommendationCandidateResponseDTO;
import com.workit.domain.recommendation.offices.dto.response.OfficeRecommendationResultItemResponseDTO;
import com.workit.domain.recommendation.offices.dto.response.OfficeRecommendationScoreResponseDTO;
import com.workit.domain.recommendation.offices.exception.OfficeRecommendationErrorCode;
import com.workit.domain.recommendation.offices.vo.OfficeAtmosphereType;
import com.workit.domain.recommendation.offices.vo.OfficePriorityType;
import com.workit.domain.recommendation.offices.vo.OfficeReferenceType;
import com.workit.domain.recommendation.offices.vo.RecommendationType;
import com.workit.domain.recommendation.offices.mapper.OfficeRecommendationMapper;
import com.workit.domain.recommendation.offices.vo.OfficeCandidateVO;
import com.workit.domain.recommendation.offices.vo.OfficeRecommendationRequestVO;
import com.workit.domain.recommendation.offices.vo.OfficeReferenceMerchantVO;
import com.workit.domain.recommendation.offices.vo.OfficeRecommendationResultVO;
import com.workit.domain.recommendation.offices.vo.OfficeSurveyAnswerVO;
import com.workit.domain.recommendation.common.mapper.ReservationProductAvailabilityMapper;
import com.workit.domain.recommendation.common.dto.response.RecommendationListResponseDTO;
import com.workit.domain.workation.mapper.WorkationMapper;
import com.workit.domain.workation.service.WorkationOwnershipValidator;
import com.workit.domain.workation.vo.BudgetType;
import com.workit.domain.workation.vo.WorkationVO;
import com.workit.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OfficeRecommendationServiceImpl implements OfficeRecommendationService {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal THIRTY = BigDecimal.valueOf(30);
    private static final BigDecimal FIFTY = BigDecimal.valueOf(50);
    private static final BigDecimal SEVENTY = BigDecimal.valueOf(70);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal SCORE_DENOMINATOR = BigDecimal.valueOf(100);
    private static final BigDecimal EARTH_RADIUS_METER = BigDecimal.valueOf(6371000);
    private static final String RECOMMENDATION_MEAL_TYPE = "LUNCH";
    private static final String CURSOR_RANKING_KEY = "ranking";
    private static final String CURSOR_RESULT_ID_KEY = "recommendationResultId";

    private static final ObjectMapper OFFICE_RECOMMENDATION_CURSOR_OBJECT_MAPPER = new ObjectMapper();

    private final OfficeRecommendationMapper recommendationMapper;
    private final BudgetMapper budgetMapper;
    private final WorkationOwnershipValidator ownershipValidator;
    private final WorkationMapper workationMapper;
    private final ReservationProductAvailabilityMapper reservationProductAvailabilityMapper;

    @Override
    @Transactional
    public RecommendationListResponseDTO<OfficeRecommendationResultItemResponseDTO> createOfficeRecommendation(
            Long userId,
            OfficeRecommendationCreateRequestDTO request) {
        validateCreateRequest(request);
        WorkationVO workation = ownershipValidator.getOwned(userId, request.getWorkationId());

        OfficeReferenceMerchantVO reference = recommendationMapper.selectAccommodationReference(userId, workation.getId());
        OfficeReferenceType referenceType = reference == null ? OfficeReferenceType.REGION_ONLY : OfficeReferenceType.AUTO_MERCHANT;
        Long referenceMerchantId = reference == null ? null : reference.getMerchantId();

        LocalDateTime[] todayRange = getTodayRangeForKorea();
        OfficeRecommendationRequestVO todayRequest = recommendationMapper.selectTodayLatestRecommendationRequestByUser(
                userId,
                workation.getId(),
                referenceMerchantId,
                todayRange[0],
                todayRange[1]
        );
        if (todayRequest != null && hasSavedRecommendationResult(todayRequest.getId())) {
            return toCommon(buildOfficeRecommendationResponse(userId, todayRequest, null, request.getSize()));
        }

        return createOfficeRecommendationByReference(userId, workation, reference, referenceType, request.getSize());
    }

    private RecommendationListResponseDTO<OfficeRecommendationResultItemResponseDTO> toCommon(
            OfficeRecommendationResponseDTO response) {
        if (response == null) {
            return RecommendationListResponseDTO.<OfficeRecommendationResultItemResponseDTO>of(
                    null, null, null, null, null, 0, false, null);
        }

        RecommendationListResponseDTO.RecommendationReference reference =
                new RecommendationListResponseDTO.RecommendationReference(
                        response.getRecommendationType(),
                        response.getReference() == null ? null : response.getReference().getPrimaryMerchantId(),
                        response.getReference() == null ? null : response.getReference().getPrimaryMerchantName(),
                        response.getReference() == null ? null : response.getReference().getSecondaryMerchantId(),
                        response.getReference() == null ? null : response.getReference().getSecondaryMerchantName(),
                        response.getReference() == null || response.getReference().getLatitude() == null
                                ? null
                                : response.getReference().getLatitude().toPlainString(),
                        response.getReference() == null || response.getReference().getLongitude() == null
                                ? null
                                : response.getReference().getLongitude().toPlainString(),
                        null
                );

        return RecommendationListResponseDTO.of(
                response.getRecommendationRequestId(),
                response.getRecommendationType(),
                null,
                reference,
                response.getContent(),
                response.getPageInfo() == null ? 0 : response.getPageInfo().getSize(),
                response.getPageInfo() != null && response.getPageInfo().getHasNext() != null
                        && response.getPageInfo().getHasNext(),
                response.getPageInfo() == null ? null : response.getPageInfo().getNextCursor()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public OfficeRecommendationReferenceResponseDTO findOfficeReferencePlace(Long userId) {
        WorkationVO workation = workationMapper.selectActiveWorkation(userId);
        if (workation == null) {
            throw new BusinessException(OfficeRecommendationErrorCode.WORKATION_NOT_FOUND);
        }

        OfficeRecommendationRequestVO latest = recommendationMapper.selectLatestRecommendationRequestByUser(
                userId,
                workation.getId(),
                null
        );
        if (latest != null) {
            OfficeReferenceType latestReferenceType = toReferenceType(latest.getReferenceType());
            return buildOfficeReferenceResponse(latest, latestReferenceType);
        }

        OfficeReferenceMerchantVO reference = recommendationMapper.selectAccommodationReference(userId, workation.getId());
        OfficeReferenceType referenceType = reference == null ? OfficeReferenceType.REGION_ONLY : OfficeReferenceType.AUTO_MERCHANT;
        return buildOfficeReferenceResponse(reference, referenceType);
    }

    @Override
    @Transactional(readOnly = true)
    public OfficeRecommendationCandidateListResponseDTO findOfficeReferencePlaceCandidates(Long userId) {
        WorkationVO workation = workationMapper.selectActiveWorkation(userId);
        if (workation == null) {
            throw new BusinessException(OfficeRecommendationErrorCode.WORKATION_NOT_FOUND);
        }

        List<OfficeCandidateVO> candidates = recommendationMapper.selectOfficeCandidates(
                userId,
                workation.getRegionId(),
                workation.getStartDate(),
                workation.getEndDate()
        );
        if (candidates == null) {
            candidates = new ArrayList<>();
        }

        return new OfficeRecommendationCandidateListResponseDTO(
                candidates.stream().map(OfficeRecommendationCandidateResponseDTO::from).collect(Collectors.toList())
        );
    }

    @Override
    @Transactional
    public RecommendationListResponseDTO<OfficeRecommendationResultItemResponseDTO> getOfficeRecommendation(Long userId, Long referenceMerchantId,
                                                                  String cursor, Integer size) {

        WorkationVO workation = workationMapper.selectActiveWorkation(userId);
        if (workation == null) {
            throw new BusinessException(OfficeRecommendationErrorCode.WORKATION_NOT_FOUND);
        }

        OfficeRecommendationRequestVO requestVO = recommendationMapper.selectLatestRecommendationRequestByUser(
                userId,
                workation.getId(),
                referenceMerchantId
        );

        if (requestVO == null
                || !RecommendationType.OFFICE.name().equals(requestVO.getRecommendationType())
                || !hasSavedRecommendationResult(requestVO.getId())) {
            OfficeRecommendationCreateRequestDTO createRequest = new OfficeRecommendationCreateRequestDTO();
            createRequest.setWorkationId(workation.getId());
            createRequest.setSize(size);
            RecommendationListResponseDTO<OfficeRecommendationResultItemResponseDTO> createdResponse =
                    createOfficeRecommendation(userId, createRequest);
            if (createdResponse != null) {
                return createdResponse;
            }

            requestVO = recommendationMapper.selectLatestRecommendationRequestByUser(
                    userId,
                    workation.getId(),
                    referenceMerchantId
            );
        }

        if (requestVO == null || !RecommendationType.OFFICE.name().equals(requestVO.getRecommendationType())
                || !hasSavedRecommendationResult(requestVO.getId())) {
            throw new BusinessException(OfficeRecommendationErrorCode.INVALID_RECOMMENDATION_REQUEST);
        }

        return toCommon(buildOfficeRecommendationResponse(userId, requestVO, cursor, size));
    }

    @Override
    @Transactional
    public RecommendationListResponseDTO<OfficeRecommendationResultItemResponseDTO> getOfficeRecommendationByRequestId(Long userId,
                                                                              Long recommendationRequestId,
                                                                              String cursor,
                                                                              Integer size) {
        OfficeRecommendationRequestVO requestVO = recommendationMapper.selectRecommendationRequestById(
                userId,
                recommendationRequestId
        );
        if (requestVO == null || !RecommendationType.OFFICE.name().equals(requestVO.getRecommendationType())
                || !hasSavedRecommendationResult(requestVO.getId())) {
            throw new BusinessException(OfficeRecommendationErrorCode.INVALID_RECOMMENDATION_REQUEST);
        }
        return toCommon(buildOfficeRecommendationResponse(userId, requestVO, cursor, size));
    }

    @Override
    @Transactional
    public RecommendationListResponseDTO<OfficeRecommendationResultItemResponseDTO> recalculateOfficeRecommendation(
            Long userId, Long recommendationRequestId, OfficeRecommendationRecalculateRequestDTO request) {
        if (recommendationRequestId == null || request == null || request.getReferenceMerchantId() == null) {
            throw new BusinessException(OfficeRecommendationErrorCode.INVALID_RECOMMENDATION_REQUEST);
        }

        OfficeRecommendationRequestVO previous = recommendationMapper.selectRecommendationRequestById(userId, recommendationRequestId);
        if (previous == null || !RecommendationType.OFFICE.name().equals(previous.getRecommendationType())) {
            throw new BusinessException(OfficeRecommendationErrorCode.INVALID_RECOMMENDATION_REQUEST);
        }

        WorkationVO workation = ownershipValidator.getOwned(userId, previous.getWorkationId());
        if (workation == null) {
            throw new BusinessException(OfficeRecommendationErrorCode.WORKATION_NOT_FOUND);
        }

        List<OfficeCandidateVO> availableCandidates = recommendationMapper.selectOfficeCandidates(
                userId,
                workation.getRegionId(),
                workation.getStartDate(),
                workation.getEndDate()
        );

        OfficeCandidateVO selectedCandidate = availableCandidates == null
                ? null
                : availableCandidates.stream()
                .filter(candidate -> request.getReferenceMerchantId().equals(candidate.getMerchantId()))
                .findFirst()
                .orElse(null);

        if (selectedCandidate == null) {
            throw new BusinessException(OfficeRecommendationErrorCode.INVALID_RECOMMENDATION_REQUEST);
        }

        OfficeReferenceMerchantVO reference = new OfficeReferenceMerchantVO();
        reference.setMerchantId(selectedCandidate.getMerchantId());
        reference.setMerchantName(selectedCandidate.getName());
        reference.setLatitude(selectedCandidate.getLatitude());
        reference.setLongitude(selectedCandidate.getLongitude());

        return createOfficeRecommendationByReference(
                userId,
                workation,
                reference,
                OfficeReferenceType.USER_SELECTED,
                DEFAULT_SIZE
        );
    }

    private RecommendationListResponseDTO<OfficeRecommendationResultItemResponseDTO> createOfficeRecommendationByReference(
            Long userId,
            WorkationVO workation,
            OfficeReferenceMerchantVO reference,
            OfficeReferenceType referenceType,
            Integer size) {
        List<OfficeSurveyAnswerVO> surveyAnswers = loadSurveyAnswers(userId, workation.getId());
        String q1Code = extractQ1Code(surveyAnswers);
        Set<String> q2Codes = extractQ2Codes(surveyAnswers);

        OfficePriorityType priority = resolvePriority(q1Code);
        Set<OfficeAtmosphereType> atmospheres = resolveAtmospheres(q2Codes);

        BigDecimal officeBudget = loadWorkBudget(workation.getId());
        BigDecimal budgetPerDay = calculateDailyBudget(officeBudget, workation.getStartDate(), workation.getEndDate());

        OfficeRecommendationRequestVO requestVO = new OfficeRecommendationRequestVO();
        requestVO.setUserId(userId);
        requestVO.setWorkationId(workation.getId());
        requestVO.setRecommendationType(RecommendationType.OFFICE.name());
        requestVO.setReferenceType(referenceType.name());
        requestVO.setMealType(RECOMMENDATION_MEAL_TYPE);
        requestVO.setReferenceMerchantId(reference == null ? null : reference.getMerchantId());
        requestVO.setReferenceLatitude(reference == null ? null : reference.getLatitude());
        requestVO.setReferenceLongitude(reference == null ? null : reference.getLongitude());
        requestVO.setSecondaryReferenceMerchantId(null);

        recommendationMapper.insertRecommendationRequest(requestVO);
        Long recommendationRequestId = requestVO.getId();

        List<Long> availableMerchantIds = reservationProductAvailabilityMapper.selectAvailableMerchantIdsByPeriod(
                workation.getRegionId(),
                "OFFICE",
                null,
                workation.getStartDate(),
                workation.getEndDate(),
                true,
                null
        );

        List<OfficeCandidateVO> candidates = availableMerchantIds == null || availableMerchantIds.isEmpty()
                ? new ArrayList<>()
                : recommendationMapper.selectOfficeCandidatesByMerchantIds(userId, availableMerchantIds);

        log.info(
                "공유오피스 추천 후보 조회: userId={}, workationId={}, regionId={}, startDate={}, endDate={}, candidateCount={}",
                userId,
                workation.getId(),
                workation.getRegionId(),
                workation.getStartDate(),
                workation.getEndDate(),
                candidates.size()
        );

        List<OfficeRecommendationResultVO> recommendationResultList = calculateRecommendationResults(
                candidates,
                budgetPerDay,
                priority,
                atmospheres,
                referenceType,
                reference
        );

        sortByTotalScore(recommendationResultList);

        if (!recommendationResultList.isEmpty()) {
            for (int i = 0; i < recommendationResultList.size(); i++) {
                recommendationResultList.get(i).setRanking(i + 1);
            }
            recommendationMapper.insertRecommendationResults(recommendationRequestId, recommendationResultList);
        }

        return toCommon(buildOfficeRecommendationResponse(userId, requestVO, null, size));
    }

    private OfficeRecommendationReferenceResponseDTO buildOfficeReferenceResponse(OfficeRecommendationRequestVO requestVO,
                                                                                OfficeReferenceType referenceType) {
        String description = resolveOfficeReferenceDescription(referenceType);
        return OfficeRecommendationReferenceResponseDTO.builder()
                .referenceType(referenceType.name())
                .primaryMerchantId(requestVO == null ? null : requestVO.getReferenceMerchantId())
                .primaryMerchantName(requestVO == null ? null : requestVO.getReferenceMerchantName())
                .secondaryMerchantId(requestVO == null ? null : requestVO.getSecondaryReferenceMerchantId())
                .secondaryMerchantName(requestVO == null ? null : requestVO.getSecondaryReferenceMerchantName())
                .latitude(requestVO == null ? null : requestVO.getReferenceLatitude())
                .longitude(requestVO == null ? null : requestVO.getReferenceLongitude())
                .description(description)
                .build();
    }

    private OfficeRecommendationReferenceResponseDTO buildOfficeReferenceResponse(OfficeReferenceMerchantVO reference,
                                                                                OfficeReferenceType referenceType) {
        String description = resolveOfficeReferenceDescription(referenceType);
        return OfficeRecommendationReferenceResponseDTO.builder()
                .referenceType(referenceType.name())
                .primaryMerchantId(reference == null ? null : reference.getMerchantId())
                .primaryMerchantName(reference == null ? null : reference.getMerchantName())
                .secondaryMerchantId(null)
                .secondaryMerchantName(null)
                .latitude(reference == null ? null : reference.getLatitude())
                .longitude(reference == null ? null : reference.getLongitude())
                .description(description)
                .build();
    }

    private String resolveOfficeReferenceDescription(OfficeReferenceType referenceType) {
        if (OfficeReferenceType.AUTO_MERCHANT == referenceType) {
            return "확정 예약된 숙소를 기준으로 추천했어요.";
        }
        if (OfficeReferenceType.USER_SELECTED == referenceType) {
            return "선택한 숙소를 기준으로 추천했어요.";
        }
        return "확정된 숙소가 없어 워케이션 지역을 기준으로 추천했어요.";
    }

    private OfficeRecommendationResponseDTO buildOfficeRecommendationResponse(Long userId,
                                                                            OfficeRecommendationRequestVO requestVO,
                                                                            String cursor,
                                                                            Integer size) {
        if (requestVO == null || !RecommendationType.OFFICE.name().equals(requestVO.getRecommendationType())) {
            throw new BusinessException(OfficeRecommendationErrorCode.INVALID_RECOMMENDATION_REQUEST);
        }

        Long recommendationRequestId = requestVO.getId();

        int safeSize = normalizeSize(size);
        int querySize = safeSize + 1;

        RecommendationCursor decodedCursor = decodeCursor(cursor);

        List<OfficeRecommendationResultVO> rawResults = recommendationMapper.selectRecommendationResultsByCursor(
                recommendationRequestId,
                decodedCursor.getRanking(),
                decodedCursor.getRecommendationResultId(),
                userId,
                querySize
        );

        if (rawResults == null) {
            rawResults = new ArrayList<>();
        }

        boolean hasNext = rawResults.size() > safeSize;
        List<OfficeRecommendationResultVO> contentSource = rawResults.stream()
                .limit(safeSize)
                .collect(Collectors.toList());

        OfficeReferenceType referenceType = toReferenceType(requestVO.getReferenceType());

        for (OfficeRecommendationResultVO resultVO : contentSource) {
            resultVO.setRecommendationRequestId(recommendationRequestId);
            resultVO.setDistanceMeters(calculateDistanceMeters(requestVO, resultVO, referenceType));
        }

        String nextCursor = hasNext && !contentSource.isEmpty()
                ? encodeCursor(contentSource.get(contentSource.size() - 1))
                : null;

        OfficeRecommendationPageInfoDTO pageInfo = OfficeRecommendationPageInfoDTO.builder()
                .size(safeSize)
                .numberOfElements(contentSource.size())
                .hasNext(hasNext)
                .nextCursor(nextCursor)
                .build();

        return OfficeRecommendationResponseDTO.of(
                requestVO,
                contentSource.stream()
                        .map(result -> toItemResponse(result, referenceType))
                        .collect(Collectors.toList()),
                pageInfo,
                RecommendationType.OFFICE.name(),
                buildReference(requestVO, referenceType)
        );
    }

    private LocalDateTime[] getTodayRangeForKorea() {
        ZoneId zone = ZoneId.of("Asia/Seoul");
        LocalDate today = LocalDate.now(zone);
        LocalDateTime startAt = today.atStartOfDay(zone).toLocalDateTime();
        LocalDateTime endAt = today.plusDays(1).atStartOfDay(zone).toLocalDateTime();
        return new LocalDateTime[]{startAt, endAt};
    }

    private boolean hasSavedRecommendationResult(Long requestId) {
        if (requestId == null) {
            return false;
        }
        return recommendationMapper.selectRecommendationResultCountByRequestId(requestId) > 0;
    }

    private List<OfficeRecommendationResultVO> calculateRecommendationResults(List<OfficeCandidateVO> candidates,
                                                                             BigDecimal budgetPerDay,
                                                                             OfficePriorityType priority,
                                                                             Set<OfficeAtmosphereType> atmospheres,
                                                                             OfficeReferenceType referenceType,
                                                                             OfficeReferenceMerchantVO reference) {
        if (candidates == null || candidates.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, BigDecimal> weightMap = calculateWeights(priority, referenceType);

        List<OfficeRecommendationResultVO> results = new ArrayList<>();

        for (OfficeCandidateVO candidate : candidates) {
            BigDecimal priceScore = calculatePriceScore(candidate, budgetPerDay);
            BigDecimal preferenceScore = calculatePreferenceScore(candidate, atmospheres);
            BigDecimal accessibilityScore = calculateAccessibilityScore(referenceType, reference, candidate);
            BigDecimal ratingScore = calculateRatingScore(candidate);
            BigDecimal totalScore = calculateTotalScore(
                    priceScore,
                    preferenceScore,
                    accessibilityScore,
                    ratingScore,
                    weightMap
            );

            OfficeRecommendationResultVO vo = new OfficeRecommendationResultVO();
            vo.setRecommendationRequestId(null);
            vo.setMerchantId(candidate.getMerchantId());
            vo.setName(candidate.getName());
            vo.setAddress(candidate.getAddress());
            vo.setThumbnailUrl(candidate.getThumbnailUrl());
            vo.setPrice(candidate.getPrice());
            vo.setRating(candidate.getRating());
            vo.setNoiseLevel(candidate.getNoiseLevel());
            vo.setReviewCount(candidate.getReviewCount());
            vo.setQuietReviewCount(candidate.getQuietReviewCount());
            vo.setOpenReviewCount(candidate.getOpenReviewCount());
            vo.setCollabReviewCount(candidate.getCollabReviewCount());
            vo.setBookmarked(candidate.getBookmarked());
            vo.setPriceScore(priceScore);
            vo.setPreferenceScore(preferenceScore);
            vo.setAccessibilityScore(accessibilityScore);
            vo.setRatingScore(ratingScore);
            vo.setTotalScore(totalScore);
            results.add(vo);
        }
        return results;
    }

    private void sortByTotalScore(List<OfficeRecommendationResultVO> results) {
        results.sort(Comparator
                .comparing(OfficeRecommendationResultVO::getTotalScore, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(OfficeRecommendationResultVO::getMerchantId, Comparator.nullsLast(Comparator.naturalOrder())));
    }

    private BigDecimal calculateTotalScore(BigDecimal priceScore,
                                          BigDecimal preferenceScore,
                                          BigDecimal accessibilityScore,
                                          BigDecimal ratingScore,
                                          Map<String, BigDecimal> weightMap) {
        BigDecimal score = ZERO;

        score = score.add(priceScore.multiply(weightMap.get("PRICE")));
        score = score.add(preferenceScore.multiply(weightMap.get("PREFERENCE")));

        if (accessibilityScore != null) {
            score = score.add(accessibilityScore.multiply(weightMap.get("ACCESSIBILITY")));
        }

        score = score.add(ratingScore.multiply(weightMap.get("RATING")));
        return score.setScale(2, RoundingMode.HALF_UP);
    }

    private Map<String, BigDecimal> calculateWeights(OfficePriorityType priority, OfficeReferenceType referenceType) {
        BigDecimal price;
        BigDecimal preference;
        BigDecimal accessibility;
        BigDecimal rating;

        if (OfficeReferenceType.REGION_ONLY == referenceType) {
            switch (priority) {
                case PRICE:
                    price = BigDecimal.valueOf(56);
                    preference = BigDecimal.valueOf(31);
                    rating = BigDecimal.valueOf(13);
                    accessibility = ZERO;
                    break;
                case ACCESSIBILITY:
                    price = BigDecimal.valueOf(42);
                    preference = BigDecimal.valueOf(42);
                    rating = BigDecimal.valueOf(16);
                    accessibility = ZERO;
                    break;
                case RATING:
                    price = BigDecimal.valueOf(29);
                    preference = BigDecimal.valueOf(29);
                    rating = BigDecimal.valueOf(42);
                    accessibility = ZERO;
                    break;
                default:
                    price = BigDecimal.valueOf(37);
                    preference = BigDecimal.valueOf(44);
                    rating = BigDecimal.valueOf(19);
                    accessibility = ZERO;
                    break;
            }
        } else {
            switch (priority) {
                case PRICE:
                    price = BigDecimal.valueOf(45);
                    preference = BigDecimal.valueOf(25);
                    accessibility = BigDecimal.valueOf(20);
                    rating = BigDecimal.valueOf(10);
                    break;
                case ACCESSIBILITY:
                    price = BigDecimal.valueOf(25);
                    preference = BigDecimal.valueOf(25);
                    accessibility = BigDecimal.valueOf(40);
                    rating = BigDecimal.valueOf(10);
                    break;
                case RATING:
                    price = BigDecimal.valueOf(25);
                    preference = BigDecimal.valueOf(25);
                    accessibility = BigDecimal.valueOf(15);
                    rating = BigDecimal.valueOf(35);
                    break;
                default:
                    price = BigDecimal.valueOf(30);
                    preference = BigDecimal.valueOf(35);
                    accessibility = BigDecimal.valueOf(20);
                    rating = BigDecimal.valueOf(15);
                    break;
            }
        }

        BigDecimal total = price.add(preference).add(rating);
        if (accessibility != null && accessibility.compareTo(ZERO) > 0) {
            total = total.add(accessibility);
        }

        if (total.compareTo(ZERO) == 0) {
            Map<String, BigDecimal> weights = new HashMap<>();
            weights.put("PRICE", ZERO);
            weights.put("PREFERENCE", ZERO);
            weights.put("ACCESSIBILITY", ZERO);
            weights.put("RATING", ZERO);
            return weights;
        }

        Map<String, BigDecimal> weights = new HashMap<>();
        weights.put("PRICE", price.divide(total, 4, RoundingMode.HALF_UP));
        weights.put("PREFERENCE", preference.divide(total, 4, RoundingMode.HALF_UP));
        weights.put("ACCESSIBILITY", accessibility.divide(total, 4, RoundingMode.HALF_UP));
        weights.put("RATING", rating.divide(total, 4, RoundingMode.HALF_UP));
        return weights;
    }

    private BigDecimal calculatePriceScore(OfficeCandidateVO candidate, BigDecimal budgetPerDay) {
        if (candidate == null || budgetPerDay == null || budgetPerDay.compareTo(ZERO) <= 0) {
            return ZERO;
        }

        Long priceValue = candidate.getPrice();
        if (priceValue == null) {
            return ZERO;
        }

        BigDecimal officePrice = BigDecimal.valueOf(priceValue);
        if (officePrice.compareTo(budgetPerDay) <= 0) {
            return HUNDRED;
        }

        BigDecimal over = officePrice.subtract(budgetPerDay);
        BigDecimal discountRate = over.divide(budgetPerDay, 10, RoundingMode.HALF_UP).multiply(HUNDRED);
        BigDecimal score = HUNDRED.subtract(discountRate);

        if (score.compareTo(ZERO) < 0) {
            return ZERO;
        }
        return score.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculatePreferenceScore(OfficeCandidateVO candidate, Set<OfficeAtmosphereType> preferences) {
        if (preferences == null || preferences.isEmpty()) {
            return ZERO;
        }

        long quietCount = candidate.getQuietReviewCount() == null ? 0L : candidate.getQuietReviewCount();
        long openCount = candidate.getOpenReviewCount() == null ? 0L : candidate.getOpenReviewCount();
        long collabCount = candidate.getCollabReviewCount() == null ? 0L : candidate.getCollabReviewCount();
        long totalCount = quietCount + openCount + collabCount;

        if (totalCount == 0) {
            return FIFTY;
        }

        BigDecimal sum = ZERO;
        for (OfficeAtmosphereType preference : preferences) {
            sum = sum.add(calculatePreferenceScore(preference, quietCount, openCount, collabCount, totalCount));
        }

        return sum.divide(BigDecimal.valueOf(preferences.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculatePreferenceScore(OfficeAtmosphereType preference,
                                              long quietCount,
                                              long openCount,
                                              long collabCount,
                                              long totalCount) {
        long targetCount = 0;
        long similarCount = 0;

        if (OfficeAtmosphereType.OPEN == preference) {
            targetCount = openCount;
            similarCount = collabCount;
        }

        if (OfficeAtmosphereType.COLLAB == preference) {
            targetCount = collabCount;
            similarCount = openCount;
        }

        if (OfficeAtmosphereType.QUIET == preference) {
            targetCount = quietCount;
            similarCount = 0;
        }

        if (targetCount == 0) {
            if (similarCount == 0) {
                return THIRTY;
            }
            return SEVENTY;
        }

        if (targetCount == totalCount) {
            return HUNDRED;
        }

        return BigDecimal.valueOf(targetCount)
                .multiply(HUNDRED)
                .divide(BigDecimal.valueOf(totalCount), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateAccessibilityScore(OfficeReferenceType referenceType, OfficeReferenceMerchantVO reference,
                                                  OfficeCandidateVO candidate) {
        if (referenceType == OfficeReferenceType.REGION_ONLY || reference == null) {
            return null;
        }

        BigDecimal distance = calculateDistanceMeters(reference, candidate);
        return distance == null ? null : mapDistanceToScore(distance);
    }

    private BigDecimal calculateDistanceMeters(OfficeReferenceMerchantVO reference, OfficeCandidateVO candidate) {
        if (reference == null
                || reference.getLatitude() == null || reference.getLongitude() == null
                || candidate.getLatitude() == null || candidate.getLongitude() == null) {
            return null;
        }

        double lat1 = Math.toRadians(reference.getLatitude().doubleValue());
        double lon1 = Math.toRadians(reference.getLongitude().doubleValue());
        double lat2 = Math.toRadians(candidate.getLatitude().doubleValue());
        double lon2 = Math.toRadians(candidate.getLongitude().doubleValue());

        double deltaLat = lat2 - lat1;
        double deltaLon = lon2 - lon1;

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        BigDecimal earthRadius = EARTH_RADIUS_METER;
        return earthRadius.multiply(BigDecimal.valueOf(c));
    }

    private BigDecimal mapDistanceToScore(BigDecimal distance) {
        if (distance == null) {
            return null;
        }

        int meter = distance.setScale(0, RoundingMode.HALF_UP).intValue();
        if (meter <= 400) {
            return HUNDRED;
        }
        if (meter <= 800) {
            return BigDecimal.valueOf(90);
        }
        if (meter <= 1200) {
            return BigDecimal.valueOf(75);
        }
        if (meter <= 2000) {
            return BigDecimal.valueOf(55);
        }
        if (meter <= 5000) {
            return BigDecimal.valueOf(35);
        }
        return BigDecimal.valueOf(15);
    }

    private BigDecimal calculateRatingScore(OfficeCandidateVO candidate) {
        if (candidate.getRating() == null) {
            return ZERO;
        }

        BigDecimal normalized = candidate.getRating()
                .divide(BigDecimal.valueOf(5), 10, RoundingMode.HALF_UP)
                .multiply(HUNDRED);
        return normalized.setScale(2, RoundingMode.HALF_UP);
    }

    private Integer calculateDistanceMeters(OfficeRecommendationRequestVO requestVO, OfficeRecommendationResultVO resultVO,
                                           OfficeReferenceType referenceType) {
        if (referenceType == OfficeReferenceType.REGION_ONLY) {
            return null;
        }

        if (requestVO.getReferenceLatitude() == null
                || requestVO.getReferenceLongitude() == null
                || resultVO.getLatitude() == null
                || resultVO.getLongitude() == null) {
            return null;
        }

        double lat1 = Math.toRadians(requestVO.getReferenceLatitude().doubleValue());
        double lon1 = Math.toRadians(requestVO.getReferenceLongitude().doubleValue());
        double lat2 = Math.toRadians(resultVO.getLatitude());
        double lon2 = Math.toRadians(resultVO.getLongitude());

        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        double meter = 6371000 * c;
        return (int) Math.round(meter);
    }

    private BigDecimal calculateDailyBudget(BigDecimal workBudget, LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new BusinessException(OfficeRecommendationErrorCode.INVALID_RECOMMENDATION_REQUEST);
        }

        if (workBudget == null || workBudget.compareTo(ZERO) <= 0) {
            throw new BusinessException(OfficeRecommendationErrorCode.RECOMMENDATION_CONDITION_NOT_READY);
        }

        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (days <= 0) {
            throw new BusinessException(OfficeRecommendationErrorCode.INVALID_RECOMMENDATION_REQUEST);
        }
        return workBudget.divide(BigDecimal.valueOf(days), 10, RoundingMode.HALF_UP);
    }

    private BigDecimal loadWorkBudget(Long workationId) {
        BigDecimal budget = budgetMapper.selectWorkBudgetByCategoryCode(workationId, BudgetType.WORK, "RENT");
        if (budget == null || budget.compareTo(ZERO) <= 0) {
            throw new BusinessException(OfficeRecommendationErrorCode.RECOMMENDATION_CONDITION_NOT_READY);
        }
        return budget;
    }

    private List<OfficeSurveyAnswerVO> loadSurveyAnswers(Long userId, Long workationId) {
        Long surveyId = recommendationMapper.selectLatestSurveyIdByWorkation(userId, workationId);
        if (surveyId == null) {
            throw new BusinessException(OfficeRecommendationErrorCode.RECOMMENDATION_CONDITION_NOT_READY);
        }

        List<OfficeSurveyAnswerVO> answers = recommendationMapper.selectSurveyAnswersBySurveyId(surveyId);
        if (answers == null || answers.isEmpty()) {
            throw new BusinessException(OfficeRecommendationErrorCode.RECOMMENDATION_CONDITION_NOT_READY);
        }
        return answers;
    }

    private String extractQ1Code(List<OfficeSurveyAnswerVO> answers) {
        for (OfficeSurveyAnswerVO answer : answers) {
            if ("Q1".equals(answer.getQuestionCode())
                    && StringUtils.hasText(answer.getOptionCode())) {
                return answer.getOptionCode().trim();
            }
        }
        throw new BusinessException(OfficeRecommendationErrorCode.RECOMMENDATION_CONDITION_NOT_READY);
    }

    private Set<String> extractQ2Codes(List<OfficeSurveyAnswerVO> answers) {
        Set<String> codes = answers.stream()
                .filter(a -> "Q2".equals(a.getQuestionCode()))
                .map(OfficeSurveyAnswerVO::getOptionCode)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toSet());

        if (codes.isEmpty()) {
            throw new BusinessException(OfficeRecommendationErrorCode.RECOMMENDATION_CONDITION_NOT_READY);
        }
        return codes;
    }

    private OfficePriorityType resolvePriority(String priorityCode) {
        OfficePriorityType priority = OfficePriorityType.from(priorityCode);
        if (priority == null) {
            throw new BusinessException(OfficeRecommendationErrorCode.RECOMMENDATION_CONDITION_NOT_READY);
        }
        return priority;
    }

    private Set<OfficeAtmosphereType> resolveAtmospheres(Set<String> codes) {
        Set<OfficeAtmosphereType> atmospheres = codes.stream()
                .map(OfficeAtmosphereType::from)
                .filter(type -> type != null)
                .collect(Collectors.toSet());

        if (atmospheres.isEmpty()) {
            throw new BusinessException(OfficeRecommendationErrorCode.RECOMMENDATION_CONDITION_NOT_READY);
        }
        return atmospheres;
    }

    private OfficeRecommendationReferenceResponseDTO buildReference(OfficeRecommendationRequestVO requestVO,
                                                                   OfficeReferenceType referenceType) {
        String description;
        if (OfficeReferenceType.AUTO_MERCHANT == referenceType) {
            description = "확정 예약된 숙소를 기준으로 추천했어요.";
        } else if (OfficeReferenceType.USER_SELECTED == referenceType) {
            description = "선택한 숙소를 기준으로 추천했어요.";
        } else {
            description = "확정된 숙소가 없어 워케이션 지역을 기준으로 추천했어요.";
        }
        return OfficeRecommendationReferenceResponseDTO.from(requestVO, description);
    }

    private OfficeRecommendationResultItemResponseDTO toItemResponse(OfficeRecommendationResultVO resultVO,
                                                                     OfficeReferenceType referenceType) {
        OfficeRecommendationScoreResponseDTO score = OfficeRecommendationScoreResponseDTO.builder()
                .priceScore(resultVO.getPriceScore())
                .preferenceScore(resultVO.getPreferenceScore())
                .accessibilityScore(resultVO.getAccessibilityScore())
                .ratingScore(resultVO.getRatingScore())
                .totalScore(resultVO.getTotalScore())
                .build();

        List<String> atmosphereTags = new ArrayList<>();
        if (resultVO.getQuietReviewCount() != null && resultVO.getQuietReviewCount() > 0) {
            atmosphereTags.add("QUIET");
        }
        if (resultVO.getOpenReviewCount() != null && resultVO.getOpenReviewCount() > 0) {
            atmosphereTags.add("OPEN");
        }
        if (resultVO.getCollabReviewCount() != null && resultVO.getCollabReviewCount() > 0) {
            atmosphereTags.add(OfficeAtmosphereType.COLLAB.toResponseValue());
        }

        return OfficeRecommendationResultItemResponseDTO.builder()
                .recommendationResultId(resultVO.getRecommendationResultId())
                .ranking(resultVO.getRanking())
                .merchantId(resultVO.getMerchantId())
                .merchantName(resultVO.getName())
                .thumbnailUrl(resultVO.getThumbnailUrl())
                .address(resultVO.getAddress())
                .price(resultVO.getPrice())
                .rating(resultVO.getRating())
                .reviewCount(resultVO.getReviewCount())
                .noiseLevel(resultVO.getNoiseLevel())
                .distanceMeters(resultVO.getDistanceMeters())
                .atmosphereTags(atmosphereTags)
                .score(score)
                .recommendationReason(buildRecommendationReason(resultVO, referenceType))
                .bookmarked(resultVO.getBookmarked())
                .calculatedAt(resultVO.getCalculatedAt())
                .build();
    }

    private String buildRecommendationReason(OfficeRecommendationResultVO resultVO, OfficeReferenceType referenceType) {
        if (referenceType == OfficeReferenceType.REGION_ONLY) {
            return String.format(
                    "가격 %.1f점, 선호도 %.1f점, 평점 %.1f점을 반영해 계산했어요.",
                    toScoreDouble(resultVO.getPriceScore()),
                    toScoreDouble(resultVO.getPreferenceScore()),
                    toScoreDouble(resultVO.getRatingScore())
            );
        }
        BigDecimal accessibilityScore = resultVO.getAccessibilityScore();
        String accessibilityValue = accessibilityScore == null ? "미계산" : String.format("%.1f", accessibilityScore);
        return String.format(
                "가격 %.1f점, 선호도 %.1f점, 접근성 %s점, 평점 %.1f점을 반영해 계산했어요.",
                toScoreDouble(resultVO.getPriceScore()),
                toScoreDouble(resultVO.getPreferenceScore()),
                accessibilityValue,
                toScoreDouble(resultVO.getRatingScore())
        );
    }

    private double toScoreDouble(BigDecimal score) {
        return score == null ? 0.0 : score.doubleValue();
    }

    private RecommendationCursor decodeCursor(String cursor) {
        if (!StringUtils.hasText(cursor)) {
            return RecommendationCursor.empty();
        }

        try {
            String normalizedCursor = URLDecoder.decode(cursor, StandardCharsets.UTF_8.toString());
            byte[] decodedBytes;
            try {
                decodedBytes = Base64.getDecoder().decode(normalizedCursor);
            } catch (IllegalArgumentException e) {
                decodedBytes = Base64.getUrlDecoder().decode(normalizedCursor);
            }

            String payload = new String(decodedBytes, StandardCharsets.UTF_8);
            Map<String, Object> map = OFFICE_RECOMMENDATION_CURSOR_OBJECT_MAPPER.readValue(
                    payload,
                    new TypeReference<Map<String, Object>>() {
                    }
            );

            Object rankingRaw = map.get(CURSOR_RANKING_KEY);
            Object resultIdRaw = map.get(CURSOR_RESULT_ID_KEY);
            if (rankingRaw == null || resultIdRaw == null) {
                throw new IllegalArgumentException();
            }

            int ranking = Integer.parseInt(rankingRaw.toString());
            long recommendationResultId = Long.parseLong(resultIdRaw.toString());

            if (ranking < 1 || recommendationResultId < 1) {
                throw new IllegalArgumentException();
            }
            return new RecommendationCursor(ranking, recommendationResultId);
        } catch (Exception e) {
            log.warn("오피스 추천 커서 디코딩 실패 - cursor={}", cursor);
            throw new BusinessException(OfficeRecommendationErrorCode.INVALID_RECOMMENDATION_CURSOR);
        }
    }

    private String encodeCursor(OfficeRecommendationResultVO office) {
        if (office == null || office.getRanking() == null || office.getRecommendationResultId() == null) {
            return null;
        }

        Map<String, Object> cursorMap = new LinkedHashMap<>();
        cursorMap.put(CURSOR_RANKING_KEY, office.getRanking());
        cursorMap.put(CURSOR_RESULT_ID_KEY, office.getRecommendationResultId());

        try {
            String cursorPayload = OFFICE_RECOMMENDATION_CURSOR_OBJECT_MAPPER.writeValueAsString(cursorMap);
            return Base64.getUrlEncoder().encodeToString(cursorPayload.getBytes(StandardCharsets.UTF_8));
        } catch (JsonProcessingException e) {
            log.warn("오피스 추천 커서 인코딩 실패 - ranking={}, resultId={}",
                    office.getRanking(), office.getRecommendationResultId());
            return null;
        }
    }

    private OfficeReferenceType toReferenceType(String referenceTypeRaw) {
        if (!StringUtils.hasText(referenceTypeRaw)) {
            return OfficeReferenceType.REGION_ONLY;
        }
        try {
            return OfficeReferenceType.valueOf(referenceTypeRaw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("알 수 없는 추천 기준 타입입니다. referenceTypeRaw={}", referenceTypeRaw);
            return OfficeReferenceType.REGION_ONLY;
        }
    }

    private int normalizeSize(Integer size) {
        if (size == null) {
            return DEFAULT_SIZE;
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new BusinessException(OfficeRecommendationErrorCode.INVALID_RECOMMENDATION_REQUEST);
        }
        return size;
    }

    private void validateCreateRequest(OfficeRecommendationCreateRequestDTO request) {
        if (request == null || request.getWorkationId() == null || request.getWorkationId() < 1) {
            throw new BusinessException(OfficeRecommendationErrorCode.INVALID_RECOMMENDATION_REQUEST);
        }
    }

    private static class RecommendationCursor {
        private final Integer ranking;
        private final Long recommendationResultId;

        private RecommendationCursor(Integer ranking, Long recommendationResultId) {
            this.ranking = ranking;
            this.recommendationResultId = recommendationResultId;
        }

        private static RecommendationCursor empty() {
            return new RecommendationCursor(null, null);
        }

        private Integer getRanking() {
            return ranking;
        }

        private Long getRecommendationResultId() {
            return recommendationResultId;
        }
    }
}
