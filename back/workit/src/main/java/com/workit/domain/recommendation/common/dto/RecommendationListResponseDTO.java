package com.workit.domain.recommendation.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecommendationListResponseDTO<T> {

    private final Long recommendationRequestId;
    private final String recommendationType;
    private final String mealType;
    private final RecommendationReference reference;
    private final List<T> content;
    private final RecommendationPageInfo pageInfo;

    public static <T> RecommendationListResponseDTO<T> of(Long recommendationRequestId,
                                                           String recommendationType,
                                                           String mealType,
                                                           RecommendationReference reference,
                                                           List<T> content,
                                                           int size,
                                                           boolean hasNext,
                                                           String nextCursor) {
        return of(recommendationRequestId, recommendationType, mealType, reference, content,
                new RecommendationPageInfo(size, content == null ? 0 : content.size(), hasNext, nextCursor));
    }

    public static <T> RecommendationListResponseDTO<T> of(Long recommendationRequestId,
                                                           String recommendationType,
                                                           String mealType,
                                                           RecommendationReference reference,
                                                           List<T> content,
                                                           RecommendationPageInfo pageInfo) {
        List<T> safeContent = content == null ? new ArrayList<>() : content;
        return RecommendationListResponseDTO.<T>builder()
                .recommendationRequestId(recommendationRequestId)
                .recommendationType(recommendationType)
                .mealType(mealType)
                .reference(reference)
                .content(safeContent)
                .pageInfo(pageInfo == null
                        ? new RecommendationPageInfo(content == null ? 0 : content.size(), content == null ? 0 : content.size(), false, null)
                        : pageInfo)
                .build();
    }

    @Getter
    @AllArgsConstructor
    public static class RecommendationReference {
        private final String referenceType;
        private final Long primaryMerchantId;
        private final String primaryMerchantName;
        private final Long secondaryMerchantId;
        private final String secondaryMerchantName;
        private final String latitude;
        private final String longitude;
        private final String description;
    }

    @Getter
    @AllArgsConstructor
    public static class RecommendationPageInfo {
        private final int size;
        private final int numberOfElements;
        private final boolean hasNext;
        private final String nextCursor;
    }
}

