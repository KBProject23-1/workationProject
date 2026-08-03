package com.workit.domain.merchant.dto.request;

import com.workit.domain.merchant.vo.AccommodationSort;
import com.workit.domain.merchant.vo.AccommodationType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class AccommodationListRequestDTO {

    private Long regionId;
    private AccommodationType accommodationType;
    private Long minPrice;
    private Long maxPrice;
    private BigDecimal minRating;
    private AccommodationSort sort = AccommodationSort.RATING_DESC;
    private int page = 0;
    private int size = 10;
}
