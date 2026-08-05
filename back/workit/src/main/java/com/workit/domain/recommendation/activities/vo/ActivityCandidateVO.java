package com.workit.domain.recommendation.activities.vo;

import com.workit.domain.recommendation.vo.RecommendationMerchantVO;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ActivityCandidateVO extends RecommendationMerchantVO {
    private String activityType;
    private Long reviewCount;
    private String difficulty;
}
