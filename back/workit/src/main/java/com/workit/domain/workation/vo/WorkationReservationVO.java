package com.workit.domain.workation.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;

// 워케이션에 묶인 예약. 기간 변경 영향을 판단하는 데 필요한 값만 담는다.
// 예약 도메인의 조회 DTO 를 그대로 쓰지 않는 이유는, 그쪽이 목록 화면용이라
// 취소 수수료·상품 상세 같은 필요 없는 정보까지 함께 조회하기 때문이다.
@Getter
@Setter
@ToString
public class WorkationReservationVO {

    private Long reservationId;
    private String reservationCode;
    private String merchantName;
    private String productName;

    // ROOM / OFFICE_SEAT / MEETING_ROOM
    private String productDetailType;

    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
}
