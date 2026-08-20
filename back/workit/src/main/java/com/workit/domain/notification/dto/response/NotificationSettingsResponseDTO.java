package com.workit.domain.notification.dto.response;

import com.workit.domain.notification.vo.NotificationSettingsVO;
import lombok.Builder;
import lombok.Getter;

// 알림 수신 설정 조회 응답 DTO
@Getter
@Builder
public class NotificationSettingsResponseDTO {

    /** 예산 알림 수신 여부 */
    private Boolean budgetNotify;

    /** 입출금 알림 수신 여부 */
    private Boolean transferNotify;

    /** 결제 알림 수신 여부 */
    private Boolean paymentNotify;

    /** 워케이션 알림 수신 여부 */
    private Boolean workationNotify;

    /** 정산 알림 수신 여부 */
    private Boolean settlementNotify;

    /** 일정 알림 수신 여부 */
    private Boolean scheduleNotify;

    /** VO → DTO 변환 */
    public static NotificationSettingsResponseDTO fromVO(NotificationSettingsVO vo) {
        return NotificationSettingsResponseDTO.builder()
                .budgetNotify(vo.getBudgetNotify())
                .transferNotify(vo.getTransferNotify())
                .paymentNotify(vo.getPaymentNotify())
                .workationNotify(vo.getWorkationNotify())
                .settlementNotify(vo.getSettlementNotify())
                .scheduleNotify(vo.getScheduleNotify())
                .build();
    }
}
