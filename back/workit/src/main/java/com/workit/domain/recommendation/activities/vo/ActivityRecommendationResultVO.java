package com.workit.domain.recommendation.activities.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ActivityRecommendationResultVO extends RecommendationResultVO {

    private String activityType;
    private String difficulty;
    private Long reviewCount;
    private String activityTags;
    private LocalDateTime calculatedAt;
}
