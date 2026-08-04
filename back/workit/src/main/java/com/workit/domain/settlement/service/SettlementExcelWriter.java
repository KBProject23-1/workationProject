package com.workit.domain.settlement.service;

import com.workit.domain.settlement.util.CardNumberFormatter;
import com.workit.domain.expense.vo.WorkationExpenseVO;
import com.workit.domain.settlement.dto.response.SettlementSummaryDTO;
import com.workit.domain.settlement.exception.SettlementErrorCode;
import com.workit.domain.settlement.vo.SettlementDocumentVO;
import com.workit.domain.workation.vo.WorkationVO;
import com.workit.exception.BusinessException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// 법인 경비 정산 Excel 생성
// 개요 시트와 상세내역 시트 두 장으로 구성한다
@Component
public class SettlementExcelWriter {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final String[] OVERVIEW_HEADERS =
            {"계정과목", "배정 예산", "집행 금액", "잔액", "집행률(%)"};

    private static final String[] DETAIL_HEADERS =
            {"일자", "가맹점", "결제수단", "카드번호", "금액"};

    public byte[] write(SettlementDocumentVO doc) {

        // try-with-resources 로 워크북과 스트림을 닫는다. 닫지 않으면 메모리가 남는다
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Styles s = new Styles(workbook);

            writeOverview(workbook, s, doc);
            writeDetail(workbook, s, doc);

            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new BusinessException(SettlementErrorCode.EXCEL_EXPORT_FAILED);
        }
    }

    // =====================================================================================
    // 개요 시트
    // =====================================================================================

    private void writeOverview(Workbook wb, Styles s, SettlementDocumentVO doc) {

        Sheet sheet = wb.createSheet("개요");
        WorkationVO w = doc.getWorkation();
        int r = 0;

        Row title = sheet.createRow(r++);
        cell(title, 0, "워케이션 경비 정산 개요", s.title);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, OVERVIEW_HEADERS.length - 1));
        r++;

        r = info(sheet, s, r, "워케이션명", w.getTitle());
        r = info(sheet, s, r, "기간",
                w.getStartDate().format(DATE) + " ~ " + w.getEndDate().format(DATE));
        r = info(sheet, s, r, "차액", money(doc.getRemainTotal()));

        // 카드가 여러 장이면 줄을 나눠 모두 표기한다
        List<String> cards = doc.getCardLabels();

        if (cards.isEmpty()) {
            r = info(sheet, s, r, "사용 법인카드", "-");
        } else {
            for (int i = 0; i < cards.size(); i++) {
                r = info(sheet, s, r, i == 0 ? "사용 법인카드" : "", cards.get(i));
            }
        }
        r++;

        Row head = sheet.createRow(r++);
        for (int i = 0; i < OVERVIEW_HEADERS.length; i++) {
            cell(head, i, OVERVIEW_HEADERS[i], s.header);
        }

        for (SettlementSummaryDTO.CategoryItem item : doc.getSummary().getCategories()) {

            Row row = sheet.createRow(r++);
            BigDecimal remain = item.getTargetAmount().subtract(item.getSpentAmount());

            cell(row, 0, item.getCategoryName(), s.text);
            num(row, 1, item.getTargetAmount(), s.money);
            num(row, 2, item.getSpentAmount(), s.money);
            num(row, 3, remain, s.money);
            rate(row, 4, rateOf(item.getSpentAmount(), item.getTargetAmount()), s.rate);
        }

        Row total = sheet.createRow(r);
        cell(total, 0, "합계", s.totalText);
        num(total, 1, doc.getTargetTotal(), s.totalMoney);
        num(total, 2, doc.getSpentTotal(), s.totalMoney);
        num(total, 3, doc.getRemainTotal(), s.totalMoney);
        rate(total, 4, rateOf(doc.getSpentTotal(), doc.getTargetTotal()), s.totalRate);

        width(sheet, 22, 15, 15, 15, 12);
    }

    // =====================================================================================
    // 상세내역 시트
    // =====================================================================================

    private void writeDetail(Workbook wb, Styles s, SettlementDocumentVO doc) {

        Sheet sheet = wb.createSheet("상세내역");
        int r = 0;

        Row title = sheet.createRow(r++);
        cell(title, 0, "계정과목별 지출 상세", s.title);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, DETAIL_HEADERS.length - 1));
        r++;

        // 계정과목 단위로 묶는다. SQL 에서 정렬해 두었으므로 LinkedHashMap 으로 순서를 유지한다
        Map<String, List<WorkationExpenseVO>> grouped = doc.getExpenses().stream()
                .collect(Collectors.groupingBy(WorkationExpenseVO::getDisplayCategoryName,
                        LinkedHashMap::new, Collectors.toList()));

        for (Map.Entry<String, List<WorkationExpenseVO>> entry : grouped.entrySet()) {

            Row section = sheet.createRow(r++);
            cell(section, 0, entry.getKey(), s.section);
            sheet.addMergedRegion(new CellRangeAddress(r - 1, r - 1, 0, DETAIL_HEADERS.length - 1));

            Row head = sheet.createRow(r++);
            for (int i = 0; i < DETAIL_HEADERS.length; i++) {
                cell(head, i, DETAIL_HEADERS[i], s.header);
            }

            BigDecimal sum = BigDecimal.ZERO;

            for (WorkationExpenseVO e : entry.getValue()) {

                Row row = sheet.createRow(r++);

                cell(row, 0, e.getSpentDate().format(DATE), s.center);
                cell(row, 1, e.getMerchantName(), s.text);
                cell(row, 2, method(e), s.center);
                cell(row, 3, maskedCard(e), s.center);
                num(row, 4, e.getAmount(), s.money);

                sum = sum.add(e.getAmount());
            }

            Row sub = sheet.createRow(r++);
            cell(sub, 0, "소계", s.totalText);
            cell(sub, 1, "", s.totalText);
            cell(sub, 2, "", s.totalText);
            cell(sub, 3, "", s.totalText);
            num(sub, 4, sum, s.totalMoney);

            r++;
        }

        Row total = sheet.createRow(r);
        cell(total, 0, "합계", s.totalText);
        cell(total, 1, "", s.totalText);
        cell(total, 2, "", s.totalText);
        cell(total, 3, "", s.totalText);
        num(total, 4, doc.getSpentTotal(), s.totalMoney);

        width(sheet, 14, 28, 14, 22, 15);
    }

    // =====================================================================================
    // 공통
    // =====================================================================================

    // 법인 지출은 등록 시 카드가 필수라 카드 없는 건은 나오지 않는다
    private String method(WorkationExpenseVO e) {

        if (e.getTransactionId() != null) {
            return "앱 결제";
        }
        return e.getCardId() != null ? "현장 결제" : "-";
    }

    private String maskedCard(WorkationExpenseVO e) {

        String formatted = CardNumberFormatter.format(e.getCardNumber());
        return formatted != null ? formatted : "-";
    }

    // 배정 예산이 0이면 나눌 수 없으므로 0으로 둔다
    private double rateOf(BigDecimal spent, BigDecimal target) {

        if (target == null || target.compareTo(BigDecimal.ZERO) == 0) {
            return 0d;
        }
        return spent.multiply(BigDecimal.valueOf(100))
                .divide(target, 1, RoundingMode.HALF_UP).doubleValue();
    }

    private String money(BigDecimal v) {
        return String.format("%,d원", (v != null ? v : BigDecimal.ZERO).longValue());
    }

    private int info(Sheet sheet, Styles s, int r, String label, String value) {

        Row row = sheet.createRow(r);
        cell(row, 0, label, s.infoLabel);
        cell(row, 1, value, s.infoValue);
        return r + 1;
    }

    private void cell(Row row, int col, String value, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value != null ? value : "");
        c.setCellStyle(style);
    }

    private void num(Row row, int col, BigDecimal value, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value != null ? value.doubleValue() : 0d);
        c.setCellStyle(style);
    }

    private void rate(Row row, int col, double value, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value);
        c.setCellStyle(style);
    }

    private void width(Sheet sheet, int... chars) {
        for (int i = 0; i < chars.length; i++) {
            sheet.setColumnWidth(i, chars[i] * 256);
        }
    }

    // 셀 스타일은 워크북당 개수 제한이 있어 미리 만들어 재사용한다
    private static class Styles {

        private final CellStyle title;
        private final CellStyle infoLabel;
        private final CellStyle infoValue;
        private final CellStyle header;
        private final CellStyle section;
        private final CellStyle text;
        private final CellStyle center;
        private final CellStyle money;
        private final CellStyle rate;
        private final CellStyle totalText;
        private final CellStyle totalMoney;
        private final CellStyle totalRate;

        private Styles(Workbook wb) {

            DataFormat fmt = wb.createDataFormat();

            Font big = wb.createFont();
            big.setBold(true);
            big.setFontHeightInPoints((short) 14);

            Font bold = wb.createFont();
            bold.setBold(true);

            title = wb.createCellStyle();
            title.setFont(big);

            infoLabel = wb.createCellStyle();
            infoLabel.setFont(bold);

            infoValue = wb.createCellStyle();

            header = wb.createCellStyle();
            header.setFont(bold);
            header.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            header.setAlignment(HorizontalAlignment.CENTER);
            border(header);

            section = wb.createCellStyle();
            section.setFont(bold);
            section.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
            section.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            border(section);

            text = wb.createCellStyle();
            border(text);

            center = wb.createCellStyle();
            center.setAlignment(HorizontalAlignment.CENTER);
            border(center);

            money = wb.createCellStyle();
            money.setDataFormat(fmt.getFormat("#,##0"));
            border(money);

            rate = wb.createCellStyle();
            rate.setDataFormat(fmt.getFormat("0.0"));
            rate.setAlignment(HorizontalAlignment.CENTER);
            border(rate);

            totalText = wb.createCellStyle();
            totalText.setFont(bold);
            fill(totalText);
            border(totalText);

            totalMoney = wb.createCellStyle();
            totalMoney.setFont(bold);
            totalMoney.setDataFormat(fmt.getFormat("#,##0"));
            fill(totalMoney);
            border(totalMoney);

            totalRate = wb.createCellStyle();
            totalRate.setFont(bold);
            totalRate.setDataFormat(fmt.getFormat("0.0"));
            totalRate.setAlignment(HorizontalAlignment.CENTER);
            fill(totalRate);
            border(totalRate);
        }

        private void fill(CellStyle style) {
            style.setFillForegroundColor(IndexedColors.LEMON_CHIFFON.getIndex());
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }

        private void border(CellStyle style) {
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
        }
    }
}
