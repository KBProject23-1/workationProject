package com.workit.domain.recommendation.offices.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OfficeRecommendationCreateRequestDTO {

    private Long workationId;
    private Integer size;
}

