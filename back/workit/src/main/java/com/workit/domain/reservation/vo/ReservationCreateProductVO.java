package com.workit.domain.reservation.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

// 예약 생성 시 가격과 가맹점 정보를 검증하기 위한 상품 정보
@Getter
@Setter
public class ReservationCreateProductVO {

    private Long id;
    private String productName;
    private ReservationProductDetailType productDetailType;
    private Integer maxHeadcount;
    private BigDecimal pricePerUnit;
    private String priceUnit;
    private Long merchantId;
    private String merchantName;
    private ReservationCategory merchantCategory;
    private Long merchantRegionId;
}
