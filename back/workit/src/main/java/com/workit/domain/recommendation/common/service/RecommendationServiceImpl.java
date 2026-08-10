package com.workit.domain.recommendation.service;

import com.workit.domain.recommendation.dto.response.RecommendationCandidateListResponseDTO;
import com.workit.domain.recommendation.dto.response.RecommendationCandidateResponseDTO;
import com.workit.domain.recommendation.dto.response.RecommendationReferenceResponseDTO;
import com.workit.domain.recommendation.accommodation.vo.AccommodationConditionVO;
import com.workit.domain.recommendation.enums.RecommendationType;
import com.workit.domain.recommendation.exception.RecommendationErrorCode;
import com.workit.domain.recommendation.mapper.RecommendationMapper;
import com.workit.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private final RecommendationMapper recommendationMapper;

    @Override
    @Transactional(readOnly = true)
    public RecommendationReferenceResponseDTO findReferencePlace(Long userId,
                                                               RecommendationType recommendationType) {
        if (recommendationType != RecommendationType.ACCOMMODATION) {
            throw new BusinessException(RecommendationErrorCode.INVALID_RECOMMENDATION_REQUEST);
        }
        AccommodationConditionVO condition = getCondition(userId);
        return RecommendationReferenceResponseDTO.automatic(
                recommendationMapper.selectConfirmedOffice(userId, condition.getWorkationId()));
    }

    @Override
    @Transactional(readOnly = true)
    public RecommendationCandidateListResponseDTO findReferencePlaceCandidates(Long userId) {
        AccommodationConditionVO condition = getCondition(userId);
        return new RecommendationCandidateListResponseDTO(recommendationMapper
                .selectReferenceCandidates(userId, condition.getWorkationId(), condition.getRegionId())
                .stream().map(RecommendationCandidateResponseDTO::from).collect(Collectors.toList()));
    }

    private AccommodationConditionVO getCondition(Long userId) {
        AccommodationConditionVO condition = recommendationMapper.selectAccommodationCondition(userId);
        if (condition == null) {
            throw new BusinessException(RecommendationErrorCode.WORKATION_NOT_FOUND);
        }
        return condition;
    }
}
