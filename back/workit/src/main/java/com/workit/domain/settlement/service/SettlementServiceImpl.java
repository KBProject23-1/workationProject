package com.workit.domain.settlement.service;

import com.workit.domain.expense.vo.WorkationExpenseVO;
import com.workit.domain.settlement.dto.response.SettlementResponseDTO;
import com.workit.domain.settlement.dto.response.SettlementSummaryDTO;
import com.workit.domain.settlement.exception.SettlementErrorCode;
import com.workit.domain.settlement.mapper.SettlementMapper;
import com.workit.domain.settlement.vo.SettlementCategoryVO;
import com.workit.domain.settlement.vo.SettlementDocumentVO;
import com.workit.domain.settlement.vo.SettlementValidationVO;
import com.workit.domain.workation.service.WorkationOwnershipValidator;
import com.workit.domain.workation.vo.BudgetType;
import com.workit.domain.workation.vo.Region;
import com.workit.domain.workation.vo.WorkationVO;
import com.workit.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettlementServiceImpl implements SettlementService {

    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final SettlementMapper settlementMapper;
    private final WorkationOwnershipValidator ownershipValidator;
    private final SettlementExcelWriter excelWriter;
    private final SettlementPdfWriter pdfWriter;

    // =====================================================================================
    // 6.1 정산 내역 조회
    // =====================================================================================

    @Override
    @Transactional(readOnly = true)
    public SettlementResponseDTO getSettlement(Long userId, Long workationId, BudgetType budgetType) {

        // 정산 완료된 워케이션도 조회는 가능해야 하므로 getOwned 를 쓴다
        WorkationVO workation = ownershipValidator.getOwned(userId, workationId);

        List<SettlementSummaryDTO> settlements =
                buildSummaries(workationId, userId, budgetType);

        SettlementValidationVO validation =
                settlementMapper.selectValidation(workationId, budgetType);

        return SettlementResponseDTO.builder()
                .workation(toWorkationInfo(workation))
                .settlements(settlements)
                .validation(toValidationInfo(validation))
                .build();
    }

    // =====================================================================================
    // 6.2 Excel 다운로드
    // =====================================================================================

    @Override
    @Transactional(readOnly = true)
    public byte[] exportExcel(Long userId, Long workationId, BudgetType budgetType) {

        SettlementDocumentVO doc = buildDocument(userId, workationId,
                SettlementErrorCode.NO_EXPENSE_TO_SETTLE);

        log.info("정산 Excel 생성 - workationId: {}, 지출 {}건",
                workationId, doc.getExpenses().size());

        return excelWriter.write(doc);
    }

    // =====================================================================================
    // 6.3 PDF 다운로드
    // =====================================================================================

    @Override
    @Transactional(readOnly = true)
    public byte[] exportPdf(Long userId, Long workationId, BudgetType budgetType) {

        SettlementDocumentVO doc = buildDocument(userId, workationId,
                SettlementErrorCode.NO_EXPENSE_TO_EXPORT);

        log.info("정산 PDF 생성 - workationId: {}, 지출 {}건",
                workationId, doc.getExpenses().size());

        return pdfWriter.write(doc);
    }

    // 문서용 데이터를 한 번에 모은다
    // Excel·PDF 모두 회사 제출용이므로 법인 경비(WORK)만 담는다
    private SettlementDocumentVO buildDocument(Long userId, Long workationId,
                                               SettlementErrorCode emptyError) {

        WorkationVO workation = ownershipValidator.getOwned(userId, workationId);

        if (settlementMapper.countExpenses(workationId, BudgetType.WORK) == 0) {
            throw new BusinessException(emptyError);
        }

        List<SettlementSummaryDTO> summaries =
                buildSummaries(workationId, userId, BudgetType.WORK);

        return SettlementDocumentVO.builder()
                .workation(workation)
                .userName(settlementMapper.selectUserName(userId))
                .cardLabels(settlementMapper.selectUsedCardLabels(workationId, BudgetType.WORK))
                .summary(summaries.isEmpty()
                        ? SettlementSummaryDTO.of(BudgetType.WORK, Collections.emptyList())
                        : summaries.get(0))
                .expenses(settlementMapper.selectExpenseDetails(workationId, userId, BudgetType.WORK))
                .build();
    }

    // =====================================================================================
    // 공통
    // =====================================================================================

    // 카테고리 집계를 예산 유형별로 묶는다
    private List<SettlementSummaryDTO> buildSummaries(Long workationId, Long userId, BudgetType budgetType) {

        List<SettlementCategoryVO> rows =
                settlementMapper.selectSettlementCategories(workationId, userId, budgetType);

        // SQL 에서 budget_type 순으로 정렬했으므로 LinkedHashMap 으로 순서를 유지한다
        Map<BudgetType, List<SettlementCategoryVO>> grouped = rows.stream()
                .collect(Collectors.groupingBy(SettlementCategoryVO::getBudgetType,
                        LinkedHashMap::new, Collectors.toList()));

        List<SettlementSummaryDTO> result = new ArrayList<>();

        for (Map.Entry<BudgetType, List<SettlementCategoryVO>> entry : grouped.entrySet()) {
            result.add(SettlementSummaryDTO.of(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    private SettlementResponseDTO.WorkationInfo toWorkationInfo(WorkationVO workation) {

        return SettlementResponseDTO.WorkationInfo.builder()
                .id(workation.getId())
                .title(workation.getTitle())
                .region(toRegionInfo(workation))
                .startDate(workation.getStartDate())
                .endDate(workation.getEndDate())
                .totalDays(totalDays(workation.getStartDate(), workation.getEndDate()))
                .build();
    }

    // region 은 조인 결과라 조회 경로에 따라 비어 있을 수 있다
    private SettlementResponseDTO.RegionInfo toRegionInfo(WorkationVO workation) {

        Region region = workation.getRegion();

        if (region == null) {
            return null;
        }
        return SettlementResponseDTO.RegionInfo.builder()
                .id(region.getId())
                .name(region.getName())
                .build();
    }

    // 시작일과 종료일을 모두 포함한 일수. 7/1 ~ 7/3 이면 3일
    private int totalDays(LocalDate startDate, LocalDate endDate) {
        return (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    // 확인하지 않은 지출이 있어도 정산을 막지 않고 안내만 한다
    // 자동분류 결과가 맞는 경우도 많아 서버가 단정할 수 없다
    private SettlementResponseDTO.ValidationInfo toValidationInfo(SettlementValidationVO vo) {

        int uncheckedCount = (vo != null && vo.getUncheckedCount() != null) ? vo.getUncheckedCount() : 0;

        return SettlementResponseDTO.ValidationInfo.builder()
                .uncheckedCount(uncheckedCount)
                .canProceed(true)
                .message(buildValidationMessage(uncheckedCount))
                .build();
    }

    private String buildValidationMessage(int uncheckedCount) {

        if (uncheckedCount == 0) {
            return null;
        }
        return String.format(
                "자동 분류한 계정과목을 확인하지 않은 지출이 %d건 있습니다. 계속 진행하시겠어요?",
                uncheckedCount);
    }

    @Override
    public String buildFileName(String prefix, Long workationId, String extension) {
        return String.format("%s_%d_%s.%s",
                prefix, workationId, LocalDate.now().format(FILE_DATE_FORMAT), extension);
    }
}
