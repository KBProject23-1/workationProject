package com.workit.domain.settlement.controller;

import com.workit.domain.settlement.dto.response.SettlementResponseDTO;
import com.workit.domain.settlement.exception.SettlementErrorCode;
import com.workit.domain.settlement.service.SettlementService;
import com.workit.domain.workation.vo.BudgetType;
import com.workit.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

@RestController
@RequestMapping("/api/v1/workations/{workationId}/settlement")
@RequiredArgsConstructor
@Slf4j
public class SettlementController {

    private static final String EXCEL_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final SettlementService settlementService;

    // 6.1 정산 내역 조회
    // budgetType 미지정 시 법인·개인 모두 반환
    @GetMapping
    public ResponseEntity<SettlementResponseDTO> settlementGet(
            @PathVariable("workationId") Long workationId,
            @RequestParam(value = "budgetType", required = false) BudgetType budgetType) {

        // JWT 토큰에서 userId 추출로 교체 필요(추후 삭제)
        Long userId = 1L;

        return ResponseEntity.ok(settlementService.getSettlement(userId, workationId, budgetType));
    }

    // 6.2 정산 내역 Excel 다운로드
    @GetMapping("/excel")
    public void settlementExcelGet(
            @PathVariable("workationId") Long workationId,
            @RequestParam(value = "budgetType", required = false) BudgetType budgetType,
            HttpServletResponse response) {

        // JWT 토큰에서 userId 추출로 교체 필요(추후 삭제)
        Long userId = 1L;

        byte[] file = settlementService.exportExcel(userId, workationId, budgetType);
        String fileName = settlementService.buildFileName(userId, workationId, "정산내역", "xlsx");

        writeFile(response, file, fileName, EXCEL_CONTENT_TYPE);
    }

    // 6.3 지출 상세내역 PDF 다운로드
    @GetMapping("/pdf")
    public void settlementPdfGet(
            @PathVariable("workationId") Long workationId,
            @RequestParam(value = "budgetType", required = false) BudgetType budgetType,
            HttpServletResponse response) {

        // JWT 토큰에서 userId 추출로 교체 필요(추후 삭제)
        Long userId = 1L;

        byte[] file = settlementService.exportPdf(userId, workationId, budgetType);
        String fileName = settlementService.buildFileName(userId, workationId, "증빙자료", "pdf");

        writeFile(response, file, fileName, MediaType.APPLICATION_PDF_VALUE);
    }

    private void writeFile(HttpServletResponse response, byte[] file,
                           String fileName, String contentType) {

        response.setContentType(contentType);
        response.setContentLength(file.length);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(fileName));

        try (OutputStream out = response.getOutputStream()) {
            out.write(file);
            out.flush();
        } catch (IOException e) {
            log.error("파일 응답 실패 - fileName: {}", fileName, e);
            throw new BusinessException(SettlementErrorCode.FILE_WRITE_FAILED);
        }
    }

    // Content-Disposition 헤더를 만든다.
    //
    // filename 에는 ASCII 만 넣을 수 있어 한글 파일명은 filename*=UTF-8'' 로 전달한다(RFC 5987).
    // filename 은 이 형식을 모르는 클라이언트를 위한 대체값이라 한글을 뺀 이름을 넣는다.
    private String contentDisposition(String fileName) {

        String fallback = fileName.replaceAll("[^\\x20-\\x7E]", "_");

        return "attachment; filename=\"" + fallback + "\"; filename*=UTF-8''" + encode(fileName);
    }

    // URL 인코딩 후 공백이 +로 바뀌므로 %20 으로 되돌린다
    private String encode(String fileName) {
        try {
            return URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20");
        } catch (UnsupportedEncodingException e) {
            return fileName;
        }
    }
}
