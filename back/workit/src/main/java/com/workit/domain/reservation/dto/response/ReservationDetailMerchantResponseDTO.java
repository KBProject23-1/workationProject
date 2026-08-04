package com.workit.domain.reservation.dto.response;

import com.workit.domain.reservation.vo.ReservationDetailVO;
import lombok.Builder;
import lombok.Getter;

// 예약 상세에 포함되는 가맹점 정보를 표현하는 응답 DTO
@Getter
@Builder
public class ReservationDetailMerchantResponseDTO {

    private Long merchantId;
    private String name;
    private String category;
    private String address;
    private String phoneNumber;
    private String thumbnailUrl;


//    예약 상세 조회 결과에서 가맹점 정보만 변환
    public static ReservationDetailMerchantResponseDTO from(ReservationDetailVO vo) {
        return ReservationDetailMerchantResponseDTO.builder()
                .merchantId(vo.getMerchantId())
                .name(vo.getMerchantName())
                .category(vo.getMerchantCategory())
                .address(vo.getMerchantAddress())
                .phoneNumber(vo.getMerchantPhoneNumber())
                .thumbnailUrl(vo.getMerchantThumbnailUrl())
                .build();
    }
}
