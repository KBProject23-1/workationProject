package com.workit.domain.workation.service;

import com.workit.domain.workation.domain.BudgetSpentVO;
import com.workit.domain.workation.domain.WorkationVO;
import com.workit.domain.workation.dto.WorkationCreateRequestDTO;
import com.workit.domain.workation.dto.WorkationCurrentResponseDTO;
import com.workit.domain.workation.dto.WorkationResponseDTO;
import com.workit.domain.workation.exception.WorkationErrorCode;
import com.workit.domain.workation.mapper.WorkationMapper;
import com.workit.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
public class WorkationServiceImpl implements WorkationService {

    private final WorkationMapper workationMapper;

    @Override
    @Transactional
    public WorkationResponseDTO addWorkation(Long userId, WorkationCreateRequestDTO dto) {

        validateRequest(dto);

        // 진행 중인 워케이션 중복 검증
        if (workationMapper.countActiveWorkation(userId) > 0) {
            throw new BusinessException(WorkationErrorCode.ALREADY_ACTIVE);
        }

        WorkationVO vo = dto.toVO(userId);
        workationMapper.insertWorkation(vo);
        log.info("워케이션 등록 완료 - id: {}, userId: {}", vo.getId(), userId);

        return WorkationResponseDTO.from(workationMapper.selectWorkationById(vo.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public WorkationCurrentResponseDTO getCurrentWorkation(Long userId) {

        WorkationVO vo = workationMapper.selectActiveWorkation(userId);

        // 진행 중인 워케이션이 없는 것은 정상 상태이므로 예외가 아닌 빈 응답으로 처리
        if (vo == null) {
            log.info("진행 중 워케이션 없음 - userId: {}", userId);
            return WorkationCurrentResponseDTO.empty();
        }

        List<BudgetSpentVO> spentList = workationMapper.selectBudgetSpentList(vo.getId());
        int uncheckedCount = workationMapper.countUncheckedExpenses(vo.getId());

        return WorkationCurrentResponseDTO.of(vo, spentList, uncheckedCount);
    }

    private void validateRequest(WorkationCreateRequestDTO dto) {

        if (dto.getTitle() == null || dto.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("워케이션 제목을 입력해 주세요.");
        }
        if (dto.getTitle().length() > 100) {
            throw new IllegalArgumentException("워케이션 제목은 100자를 넘을 수 없습니다.");
        }
        if (dto.getStartDate() == null || dto.getEndDate() == null) {
            throw new IllegalArgumentException("워케이션 기간을 입력해 주세요.");
        }
        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new IllegalArgumentException("종료일은 시작일 이후여야 합니다.");
        }
        if (dto.getRegionId() == null || workationMapper.countRegion(dto.getRegionId()) == 0) {
            throw new BusinessException(WorkationErrorCode.REGION_NOT_FOUND);
        }
        validateBudget(dto.getBusinessBudgetTotal(), "법인 예산");
        validateBudget(dto.getPersonalBudgetTotal(), "개인 예산");
    }

    private void validateBudget(BigDecimal amount, String label) {
        if (amount == null) {
            throw new IllegalArgumentException(label + "을 입력해 주세요.");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(label + "은 0원 이상이어야 합니다.");
        }
    }
}
