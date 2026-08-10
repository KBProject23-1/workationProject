package com.workit.domain.merchant.vo;

import com.workit.domain.reservation.vo.ReservationProductDetailType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class MerchantProductVO {

    private Long productId;
    private String productName;
    private String description;
    private ReservationProductDetailType productDetailType;
    private Integer maxHeadcount;
    private BigDecimal unitPrice;
    private String thumbnailUrl;
}
