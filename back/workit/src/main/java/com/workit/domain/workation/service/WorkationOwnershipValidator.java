package com.workit.domain.workation.service;

import com.workit.domain.workation.exception.WorkationErrorCode;
import com.workit.domain.workation.mapper.WorkationMapper;
import com.workit.domain.workation.vo.WorkationStatus;
import com.workit.domain.workation.vo.WorkationVO;
import com.workit.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// 워케이션 존재 여부와 소유자를 검증한다.
// 예산·지출·정산은 모두 특정 워케이션에 딸린 데이터라 같은 검증이 반복되므로 한곳에 모았다.
// 검증 규칙이 바뀔 때 여러 Service 를 동시에 고치다 한 곳을 빠뜨리는 상황을 막는 목적이다.
@Component
@RequiredArgsConstructor
public class WorkationOwnershipValidator {

    private final WorkationMapper workationMapper;

    // 존재 여부 + 소유자 검증 후 워케이션을 반환한다
    // 없으면 404, 남의 것이면 403
    public WorkationVO getOwned(Long userId, Long workationId) {

        WorkationVO vo = workationMapper.selectWorkationById(workationId);

        if (vo == null) {
            throw new BusinessException(WorkationErrorCode.WORKATION_NOT_FOUND);
        }
        // Long 은 객체이므로 == 이 아닌 equals 로 비교해야 한다
        if (!vo.getUserId().equals(userId)) {
            throw new BusinessException(WorkationErrorCode.ACCESS_DENIED);
        }
        return vo;
    }

    // 소유자 검증에 더해 정산 완료 여부까지 확인한다
    // 정산이 끝난 워케이션은 정산 근거 자료이므로 하위 데이터를 변경할 수 없다
    // action 에는 "수정", "삭제" 처럼 메시지에 들어갈 동작 이름을 넘긴다
    public WorkationVO getOwnedActive(Long userId, Long workationId, String action) {

        WorkationVO vo = getOwned(userId, workationId);

        if (vo.getStatus() == WorkationStatus.SETTLED) {
            throw new BusinessException(WorkationErrorCode.ALREADY_SETTLED,
                    "정산 완료된 워케이션은 " + action + "할 수 없습니다.");
        }
        return vo;
    }
}
