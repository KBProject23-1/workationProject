package com.workit.domain.settlement.service;

import com.workit.domain.settlement.dto.response.SettlementResponseDTO;
import com.workit.domain.workation.vo.BudgetType;

public interface SettlementService {

    // 6.1 정산 내역 조회
    SettlementResponseDTO getSettlement(Long userId, Long workationId, BudgetType budgetType);

    // 6.2 정산 내역 Excel 다운로드
    byte[] exportExcel(Long userId, Long workationId, BudgetType budgetType);

    // 6.3 지출 상세내역 PDF 다운로드
    byte[] exportPdf(Long userId, Long workationId, BudgetType budgetType);

    // 다운로드 파일명. Controller 가 Content-Disposition 헤더에 사용한다
    String buildFileName(String prefix, Long workationId, String extension);
}
