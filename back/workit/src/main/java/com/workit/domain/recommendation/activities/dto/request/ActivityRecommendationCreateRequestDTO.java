package com.workit.domain.recommendation.activities.dto.request;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class ActivityRecommendationCreateRequestDTO {
    private Long workationId;
    private Integer size;
    private String cursor;
}
