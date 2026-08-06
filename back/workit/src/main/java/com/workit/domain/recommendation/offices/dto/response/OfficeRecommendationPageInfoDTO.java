package com.workit.domain.recommendation.offices.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OfficeRecommendationPageInfoDTO {

    private final Integer size;
    private final Integer numberOfElements;
    private final Boolean hasNext;
    private final String nextCursor;
}

