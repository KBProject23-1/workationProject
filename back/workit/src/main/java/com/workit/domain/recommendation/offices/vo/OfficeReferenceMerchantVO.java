package com.workit.domain.recommendation.offices.vo;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OfficeReferenceMerchantVO {

    private Long merchantId;
    private String merchantName;
    private BigDecimal latitude;
    private BigDecimal longitude;
}

