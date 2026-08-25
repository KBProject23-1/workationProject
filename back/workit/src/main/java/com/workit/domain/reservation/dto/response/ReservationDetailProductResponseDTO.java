package com.workit.domain.reservation.dto.response;

import com.workit.domain.reservation.vo.ReservationDetailVO;
import com.workit.domain.reservation.vo.ReservationProductDetailType;
import lombok.Builder;
import lombok.Getter;


// 예약 상세에 포함되는 객실·좌석·회의실 상품 정보를 표현하는 응답 DTO
@Getter
@Builder
public class ReservationDetailProductResponseDTO {

    private Long productId;
    private String productName;
    private ReservationProductDetailType productDetailType;
    private String thumbnailUrl;

//    예약 상세 조회 결과에서 예약 상품 정보만 변환
    public static ReservationDetailProductResponseDTO from(ReservationDetailVO vo) {
        return ReservationDetailProductResponseDTO.builder()
                .productId(vo.getProductId())
                .productName(vo.getProductName())
                .productDetailType(vo.getProductDetailType())
                .thumbnailUrl(vo.getProductThumbnailUrl())
                .build();
    }
}
