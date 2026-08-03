package com.workit.domain.merchant.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalTime;

@Getter
@Setter
public class AccommodationDetailVO {

    private Long merchantId;
    private Long regionId;
    private String name;
    private String address;
    private Double latitude;
    private Double longitude;
    private String phoneNumber;
    private AccommodationType accommodationType;
    private String description;
    private LocalTime checkInTime;
    private LocalTime checkOutTime;
    private BigDecimal rating;
    private String thumbnailUrl;
    private Long price;
}
