package com.workit.domain.recommendation.offices.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class OfficeCandidateVO {

    private Long merchantId;
    private String name;
    private String address;
    private String thumbnailUrl;
    private Long price;
    private BigDecimal rating;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String noiseLevel;
    private Long reviewCount;
    private Long quietReviewCount;
    private Long openReviewCount;
    private Long collabReviewCount;
    private Boolean bookmarked;
}

