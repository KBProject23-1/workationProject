package com.workit.domain.reservation.dto.response;

import com.workit.domain.reservation.vo.ReservationCancellationDetailVO;
import com.workit.domain.reservation.vo.ReservationProductDetailType;
import lombok.Builder;
import lombok.Getter;

/**
 * 예약 취소 상세에 포함되는 예약 상품 정보 응답 DTO
 */
@Getter
@Builder
public class ReservationCancellationDetailProductResponseDTO {

    private String productName;
    private ReservationProductDetailType productDetailType;
    private String thumbnailUrl;

    // 취소 상세 조회 결과에서 예약 상품 정보만 변환
    public static ReservationCancellationDetailProductResponseDTO from(
            ReservationCancellationDetailVO vo) {

        return ReservationCancellationDetailProductResponseDTO.builder()
                .productName(vo.getProductName())
                .productDetailType(vo.getProductDetailType())
                .thumbnailUrl(vo.getThumbnailUrl())
                .build();
    }
}
