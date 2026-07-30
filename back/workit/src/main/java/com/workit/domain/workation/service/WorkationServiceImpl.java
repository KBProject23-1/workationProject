package com.workit.domain.workation.service;

import com.workit.domain.workation.vo.BudgetSpentVO;
import com.workit.domain.workation.vo.WorkationVO;
import com.workit.domain.workation.dto.request.WorkationCreateRequestDTO;
import com.workit.domain.workation.dto.request.WorkationUpdateRequestDTO;
import com.workit.domain.workation.dto.response.*;
import com.workit.domain.workation.exception.WorkationErrorCode;
import com.workit.domain.workation.mapper.WorkationMapper;
import com.workit.exception.BusinessException;
import com.workit.global.dto.PageResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkationServiceImpl implements WorkationService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;

    private final WorkationMapper workationMapper;
    private final WorkationOwnershipValidator ownershipValidator;

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

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<WorkationHistoryResponseDTO> getWorkationHistory(Long userId, int page, int size) {

        // 잘못된 페이징 값이 들어와도 목록이 깨지지 않도록 보정
        int safePage = Math.max(page, 0);
        int safeSize = (size < 1 || size > MAX_PAGE_SIZE) ? DEFAULT_PAGE_SIZE : size;
        int offset = safePage * safeSize;

        long totalElements = workationMapper.countSettledWorkation(userId);

        // 조회 결과가 없으면 집계가 포함된 무거운 목록 쿼리를 실행하지 않는다
        if (totalElements == 0) {
            return PageResponseDTO.of(Collections.emptyList(), safePage, safeSize, 0);
        }

        // DB 조회 결과(VO)를 응답 형식(DTO)으로 변환
        List<WorkationHistoryResponseDTO> content = workationMapper
                .selectSettledWorkationList(userId, offset, safeSize)
                .stream()
                .map(WorkationHistoryResponseDTO::from)
                .collect(Collectors.toList());

        return PageResponseDTO.of(content, safePage, safeSize, totalElements);
    }

    @Override
    @Transactional
    public WorkationResponseDTO modifyWorkation(Long userId, Long workationId, WorkationUpdateRequestDTO dto) {

        // 1) 존재 여부 + 소유자 + 정산 완료 여부 검증
        ownershipValidator.getOwnedActive(userId, workationId, "수정");

        // 2) 입력값 검증 (등록과 동일한 규칙)
        validateWorkationInput(dto.getTitle(), dto.getStartDate(), dto.getEndDate(),
                dto.getRegionId(), dto.getBusinessBudgetTotal(), dto.getPersonalBudgetTotal());

        // 3) 기간을 줄였을 때 기존 지출이 기간 밖으로 벗어나는지 확인
        int outOfPeriod = workationMapper.countExpensesOutOfPeriod(
                workationId, dto.getStartDate(), dto.getEndDate());

        if (outOfPeriod > 0) {
            throw new BusinessException(WorkationErrorCode.EXPENSE_OUT_OF_PERIOD,
                    "이미 등록된 지출 " + outOfPeriod + "건이 변경한 기간을 벗어납니다.");
        }

        workationMapper.updateWorkation(dto.toVO(workationId));
        log.info("워케이션 수정 완료 - id: {}, userId: {}", workationId, userId);

        return WorkationResponseDTO.from(workationMapper.selectWorkationById(workationId));
    }

    @Override
    @Transactional
    public void removeWorkation(Long userId, Long workationId) {

        // 1) 존재 여부 + 소유자 + 정산 완료 여부 검증
        //    정산 완료된 워케이션은 정산 근거 자료이므로 삭제 불가
        ownershipValidator.getOwnedActive(userId, workationId, "삭제");

        // 2) 결제 원본은 보존하고 워케이션 연결만 해제
        workationMapper.unlinkTransactions(workationId);

        // 3) FK 제약 때문에 하위 데이터부터 삭제
        workationMapper.deleteExpensesByWorkationId(workationId);
        workationMapper.deleteBudgetsByWorkationId(workationId);
        workationMapper.deleteSurveysByWorkationId(workationId);

        workationMapper.deleteWorkation(workationId);
        log.info("워케이션 삭제 완료 - id: {}, userId: {}", workationId, userId);
    }

    @Override
    @Transactional
    public WorkationSettleResponseDTO settleWorkation(Long userId, Long workationId) {

        // 존재 여부 + 소유자 + 정산 완료 여부 검증
        // 이미 종료된 워케이션은 다시 종료할 수 없다
        ownershipValidator.getOwnedActive(userId, workationId, "종료");

        workationMapper.settleWorkation(workationId);
        log.info("워케이션 종료 완료 - id: {}, userId: {}", workationId, userId);

        // settled_at 은 DB 에서 채워지므로 재조회해서 응답한다
        return WorkationSettleResponseDTO.from(workationMapper.selectWorkationById(workationId));
    }

    // 등록 요청 검증
    private void validateRequest(WorkationCreateRequestDTO dto) {
        validateWorkationInput(dto.getTitle(), dto.getStartDate(), dto.getEndDate(),
                dto.getRegionId(), dto.getBusinessBudgetTotal(), dto.getPersonalBudgetTotal());
    }

    // 등록·수정 공통 입력값 검증
    private void validateWorkationInput(String title, LocalDate startDate, LocalDate endDate,
                                        Long regionId, BigDecimal businessBudget, BigDecimal personalBudget) {

        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("워케이션 제목을 입력해 주세요.");
        }
        if (title.length() > 100) {
            throw new IllegalArgumentException("워케이션 제목은 100자를 넘을 수 없습니다.");
        }
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("워케이션 기간을 입력해 주세요.");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("종료일은 시작일 이후여야 합니다.");
        }
        if (regionId == null || workationMapper.countRegion(regionId) == 0) {
            throw new BusinessException(WorkationErrorCode.REGION_NOT_FOUND);
        }
        validateBudget(businessBudget, "법인 예산");
        validateBudget(personalBudget, "개인 예산");
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
