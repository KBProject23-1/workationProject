package com.workit.domain.workation.dto.response;

import com.workit.domain.workation.vo.WorkationStatus;
import com.workit.domain.workation.vo.WorkationVO;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// 1.6 워케이션 종료 응답
@Getter
@Builder
public class WorkationSettleResponseDTO {

    private Long id;
    private WorkationStatus status;
    private LocalDateTime settledAt;

    public static WorkationSettleResponseDTO from(WorkationVO vo) {
        return WorkationSettleResponseDTO.builder()
                .id(vo.getId())
                .status(vo.getStatus())
                .settledAt(vo.getSettledAt())
                .build();
    }
}