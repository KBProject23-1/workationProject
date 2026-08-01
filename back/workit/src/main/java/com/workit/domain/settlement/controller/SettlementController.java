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
        String fileName = settlementService.buildFileName("settlement", workationId, "xlsx");

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
        String fileName = settlementService.buildFileName("expenses", workationId, "pdf");

        writeFile(response, file, fileName, MediaType.APPLICATION_PDF_VALUE);
    }

    // 파일 바이트를 응답 스트림에 직접 쓴다.
    // ServletConfig 가 메시지 컨버터를 Jackson 하나로 교체해 byte[] 를 처리할 컨버터가 없다.
    // ResponseEntity<byte[]> 로 반환하면 HttpMessageNotWritableException 이 발생한다.
    private void writeFile(HttpServletResponse response, byte[] file,
                           String fileName, String contentType) {

        response.setContentType(contentType);
        response.setContentLength(file.length);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + encode(fileName) + "\"");

        try (OutputStream out = response.getOutputStream()) {
            out.write(file);
            out.flush();
        } catch (IOException e) {
            log.error("파일 응답 실패 - fileName: {}", fileName, e);
            throw new BusinessException(SettlementErrorCode.FILE_WRITE_FAILED);
        }
    }

    // 파일명에 한글이 들어가면 브라우저가 깨뜨리므로 URL 인코딩한다
    // 인코딩 후 공백이 +로 바뀌므로 %20 으로 되돌린다
    private String encode(String fileName) {
        try {
            return URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20");
        } catch (UnsupportedEncodingException e) {
            return fileName;
        }
    }
}
