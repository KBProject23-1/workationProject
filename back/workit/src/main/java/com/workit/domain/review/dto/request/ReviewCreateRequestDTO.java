package com.workit.domain.review.dto.request;

import com.workit.domain.review.vo.ReviewAtmosphere;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewCreateRequestDTO {

    private Integer rating;
    private String content;
    private ReviewAtmosphere atmosphere;
    private String imageUrl;
}
