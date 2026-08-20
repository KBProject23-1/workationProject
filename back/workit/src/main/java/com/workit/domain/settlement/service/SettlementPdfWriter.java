package com.workit.domain.settlement.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.workit.domain.settlement.util.CardNumberFormatter;
import com.workit.domain.expense.vo.WorkationExpenseVO;
import com.workit.domain.settlement.dto.response.SettlementSummaryDTO;
import com.workit.domain.settlement.exception.SettlementErrorCode;
import com.workit.domain.settlement.vo.SettlementDocumentVO;
import com.workit.domain.workation.vo.WorkationVO;
import com.workit.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

// 법인 경비 정산 PDF 생성
//
// 페이지 구성
//   1p          기본정보 + 경비 사용 요약 + 계정과목별 사용 개요 + 증빙 현황
//   2p 이후     증빙자료(매출전표). 계정과목마다 새 페이지에서 시작하고 한 페이지에 4장
//   마지막 1p   현장 결제 건 (매출전표 없음)
@Component
@RequiredArgsConstructor
public class SettlementPdfWriter {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss");
    private static final NumberFormat MONEY = NumberFormat.getInstance(Locale.KOREA);

    private static final int RECEIPTS_PER_PAGE = 4;

    // 매출전표 안 값 칸의 실제 너비(pt).
    // A4(595) - 좌우 여백(88) = 507 을 2열로 나누고, 바깥 여백과 안쪽 여백을 뺀 뒤
    // 항목:값 = 1 : 1.7 로 갈랐을 때의 값 칸 너비다.
    private static final float RECEIPT_VALUE_WIDTH = 136f;

    // 전표 한 장의 최소 높이.
    // 주소가 한 줄인 전표와 두 줄인 전표가 나란히 놓여도 카드 크기가 같아 보이게 한다.
    private static final float RECEIPT_HEIGHT = 300f;

    // rows() 에서 이 표시가 붙은 칸만 줄바꿈을 허용한다
    private static final String WRAP = "WRAP";

    // 앱 화면과 같은 색을 써서 문서와 서비스가 같은 제품으로 보이게 한다
    private static final Color BLUE = new Color(37, 99, 235);
    private static final Color INK = new Color(26, 26, 26);
    private static final Color SUB = new Color(122, 130, 142);
    private static final Color LINE = new Color(226, 232, 240);
    private static final Color BAND = new Color(244, 247, 251);
    private static final Color WHITE = Color.WHITE;

    private final PdfFontProvider fonts;

    public byte[] write(SettlementDocumentVO doc) {

        Document document = new Document(PageSize.A4, 44, 44, 46, 46);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PdfWriter.getInstance(document, out);
            document.open();

            writeCoverPage(document, doc);
            writeReceiptPages(document, doc);
            writeCardRecordPage(document, doc);

            document.close();
            return out.toByteArray();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(SettlementErrorCode.PDF_EXPORT_FAILED);
        }
    }

    // =====================================================================================
    // 1p 표지
    // =====================================================================================

    private void writeCoverPage(Document d, SettlementDocumentVO doc) throws DocumentException {

        WorkationVO w = doc.getWorkation();

        d.add(docTitle("워케이션 경비 정산서"));
        d.add(docSubtitle(w.getTitle() + "  ·  "
                + w.getStartDate().format(DATE) + " ~ " + w.getEndDate().format(DATE)));
        d.add(rule());

        d.add(sectionTitle("기본정보"));
        d.add(basicInfoTable(doc));

        d.add(sectionTitle("경비 사용 요약"));
        d.add(usageSummary(doc));

        d.add(sectionTitle("계정과목별 사용 개요"));
        d.add(categoryTable(doc));

        d.add(sectionTitle("증빙 현황"));
        d.add(proofTable(doc));
    }

    private PdfPTable basicInfoTable(SettlementDocumentVO doc) throws DocumentException {

        WorkationVO w = doc.getWorkation();

        PdfPTable t = new PdfPTable(4);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{1.1f, 2.4f, 1.1f, 2.4f});
        t.setSpacingAfter(20f);

        info(t, "회사명", doc.getCompanyName());
        info(t, "성명", doc.getUserName());
        info(t, "워케이션명", w.getTitle());
        info(t, "지역", w.getRegion() != null ? w.getRegion().getName() : "-");
        info(t, "기간", w.getStartDate().format(DATE) + " ~ " + w.getEndDate().format(DATE));

        // 라벨을 "사용 법인카드" 로 두면 안 된다.
        // 법인카드가 없는 사용자는 개인카드로 업무 지출을 내고, 그 카드가 여기 찍힌다.
        //
        // 여러 장이면 줄바꿈 대신 가운뎃점으로 이어 붙이고, 칸을 넘치면 남은 장수로 줄인다.
        info(t, "사용 카드", cardLabel(doc.getCardLabels()));
        info(t, "정산 완료일",
                w.getSettledAt() != null ? w.getSettledAt().format(DATE) : "미정산");

        return t;
    }

    // 기본정보 표의 값 칸 너비. A4 폭에서 여백과 표 비율로 계산한 값이다
    private static final float INFO_VALUE_WIDTH = 158f;

    private String cardLabel(List<String> cards) {

        if (cards == null || cards.isEmpty()) {
            return "-";
        }
        if (cards.size() == 1) {
            return fit(cards.get(0), fonts.regular(9f, INK), INFO_VALUE_WIDTH);
        }

        String joined = String.join("  ·  ", cards);
        Font font = fonts.regular(9f, INK);

        if (width(joined, font) <= INFO_VALUE_WIDTH) {
            return joined;
        }
        // 다 못 넣으면 첫 장만 쓰고 나머지는 장수로 줄인다
        String rest = "  외 " + (cards.size() - 1) + "장";
        return fit(cards.get(0), font, INFO_VALUE_WIDTH - width(rest, font)) + rest;
    }

    // 큰 숫자 네 개를 나란히 보여준다. 결재자가 첫 화면에서 총액과 차액만 봐도 되게 한다
    private PdfPTable usageSummary(SettlementDocumentVO doc) throws DocumentException {

        PdfPTable t = new PdfPTable(4);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{1f, 1f, 1f, 1f});
        t.setSpacingAfter(20f);

        metric(t, "배정 예산", money(doc.getTargetTotal()), INK);
        metric(t, "집행 금액", money(doc.getSpentTotal()), BLUE);
        metric(t, "차액", money(doc.getRemainTotal()), INK);
        metric(t, "집행률", rate(doc.getSpentTotal(), doc.getTargetTotal()) + "%", INK);

        return t;
    }

    private PdfPTable categoryTable(SettlementDocumentVO doc) throws DocumentException {

        PdfPTable t = new PdfPTable(5);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{2.2f, 1.5f, 1.5f, 1.5f, 1f});
        t.setSpacingAfter(20f);

        head(t, "계정과목", Element.ALIGN_LEFT);
        head(t, "배정 예산", Element.ALIGN_RIGHT);
        head(t, "집행 금액", Element.ALIGN_RIGHT);
        head(t, "잔액", Element.ALIGN_RIGHT);
        head(t, "집행률", Element.ALIGN_CENTER);

        for (SettlementSummaryDTO.CategoryItem c : doc.getSummary().getCategories()) {

            BigDecimal remain = c.getTargetAmount().subtract(c.getSpentAmount());

            body(t, c.getCategoryName(), Element.ALIGN_LEFT, INK);
            body(t, money(c.getTargetAmount()), Element.ALIGN_RIGHT, SUB);
            body(t, money(c.getSpentAmount()), Element.ALIGN_RIGHT, INK);
            body(t, money(remain), Element.ALIGN_RIGHT, SUB);
            body(t, rate(c.getSpentAmount(), c.getTargetAmount()) + "%", Element.ALIGN_CENTER, SUB);
        }

        total(t, "합계", Element.ALIGN_LEFT);
        total(t, money(doc.getTargetTotal()), Element.ALIGN_RIGHT);
        total(t, money(doc.getSpentTotal()), Element.ALIGN_RIGHT);
        total(t, money(doc.getRemainTotal()), Element.ALIGN_RIGHT);
        total(t, rate(doc.getSpentTotal(), doc.getTargetTotal()) + "%", Element.ALIGN_CENTER);

        return t;
    }

    // 현장 결제 건은 매출전표가 없어 사용자가 종이 영수증을 따로 붙여 제출해야 한다
    private PdfPTable proofTable(SettlementDocumentVO doc) throws DocumentException {

        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{1f, 1f});

        proofCell(t, "앱 결제", doc.getAppPaymentCount() + "건",
                "매출전표를 이 문서에 첨부했습니다");
        proofCell(t, "현장 결제", doc.getCardRecordCount() + "건",
                "실물 영수증을 별도로 첨부해 주세요");

        return t;
    }

    private void proofCell(PdfPTable t, String label, String value, String note) {

        PdfPTable inner = new PdfPTable(1);
        inner.setWidthPercentage(100);

        inner.addCell(plain(label, fonts.regular(8.5f, SUB), Element.ALIGN_LEFT, 0f));
        inner.addCell(plain(value, fonts.bold(15f, INK), Element.ALIGN_LEFT, 3f));
        inner.addCell(plain(note, fonts.regular(8f, SUB), Element.ALIGN_LEFT, 3f));

        PdfPCell wrap = new PdfPCell(inner);
        wrap.setBackgroundColor(BAND);
        wrap.setBorder(Rectangle.NO_BORDER);
        wrap.setPadding(12f);
        wrap.setPaddingLeft(14f);
        t.addCell(wrap);
    }

    // =====================================================================================
    // 2p 이후 증빙자료
    // =====================================================================================

    private void writeReceiptPages(Document d, SettlementDocumentVO doc) throws DocumentException {

        // 매출전표는 앱 내 결제 건에서만 생성된다
        List<WorkationExpenseVO> targets = doc.getExpenses().stream()
                .filter(e -> e.getTransactionId() != null)
                .collect(Collectors.toList());

        if (targets.isEmpty()) {
            return;
        }

        Map<String, List<WorkationExpenseVO>> grouped = targets.stream()
                .collect(Collectors.groupingBy(WorkationExpenseVO::getDisplayCategoryName,
                        LinkedHashMap::new, Collectors.toList()));

        for (Map.Entry<String, List<WorkationExpenseVO>> entry : grouped.entrySet()) {

            List<WorkationExpenseVO> list = entry.getValue();
            int lastPage = (list.size() + RECEIPTS_PER_PAGE - 1) / RECEIPTS_PER_PAGE;

            // 계정과목이 바뀌면 새 페이지에서 시작한다. 회사에서 항목별로 분리해 보관하기 쉽다
            for (int from = 0; from < list.size(); from += RECEIPTS_PER_PAGE) {

                d.newPage();

                int to = Math.min(from + RECEIPTS_PER_PAGE, list.size());
                int page = from / RECEIPTS_PER_PAGE + 1;

                d.add(docTitle("증빙자료"));
                d.add(docSubtitle(lastPage > 1
                        ? entry.getKey() + "  ·  " + page + " / " + lastPage
                        : entry.getKey()));
                d.add(rule());

                d.add(receiptGrid(list.subList(from, to)));
            }
        }
    }

    // 영수증 2열 x 2행. 한 페이지에 4장이 들어간다
    private PdfPTable receiptGrid(List<WorkationExpenseVO> list) throws DocumentException {

        PdfPTable grid = new PdfPTable(2);
        grid.setWidthPercentage(100);
        grid.setWidths(new float[]{1f, 1f});
        grid.setSpacingBefore(6f);

        for (WorkationExpenseVO e : list) {
            grid.addCell(receiptCell(e));
        }
        // 빈 칸을 채워 마지막 행 높이가 늘어나지 않게 한다
        for (int i = list.size(); i < RECEIPTS_PER_PAGE; i++) {
            PdfPCell blank = new PdfPCell();
            blank.setBorder(Rectangle.NO_BORDER);
            blank.setFixedHeight(RECEIPT_HEIGHT);
            grid.addCell(blank);
        }
        return grid;
    }

    // 결제 파트 영수증 화면(ReceiptResponse)과 같은 항목을 담는다
    private PdfPCell receiptCell(WorkationExpenseVO e) throws DocumentException {

        PdfPTable r = new PdfPTable(1);
        r.setWidthPercentage(100);

        r.addCell(receiptHeader());

        r.addCell(groupLabel("가맹점 정보"));
        // 주소는 잘라내면 증빙으로서 의미가 없다. 이 칸만 두 줄까지 허용한다
        r.addCell(rows(new String[][]{
                {"가맹점명", e.getMerchantName()},
                {"사업자등록번호", nvl(e.getMerchantTaxpayerNumber())},
                {"주소", nvl(e.getMerchantAddress()), WRAP},
                {"전화번호", nvl(e.getMerchantPhoneNumber())}
        }));

        r.addCell(groupLabel("거래 정보"));
        r.addCell(rows(new String[][]{
                {"결제수단", paymentMethod(e)},
                {"승인상태", approvalStatus(e)},
                {"거래일시", e.getApprovedAt() != null
                        ? e.getApprovedAt().format(DATETIME)
                        : e.getSpentDate().format(DATE)},
                {"승인번호", nvl(e.getApprovedNumber())}
        }));

        r.addCell(groupLabel("결제 금액"));

        BigDecimal total = e.getAmount();
        BigDecimal supply = total.divide(BigDecimal.valueOf(1.1), 0, RoundingMode.DOWN);
        BigDecimal vat = total.subtract(supply);

        r.addCell(rows(new String[][]{
                {"공급가액", money(supply)},
                {"부가세", money(vat)}
        }));
        r.addCell(totalRow(money(total)));

        PdfPCell card = new PdfPCell(r);
        card.setBorderColor(LINE);
        card.setBorderWidth(0.7f);
        card.setPadding(0f);
        card.setMinimumHeight(RECEIPT_HEIGHT);

        PdfPCell wrap = new PdfPCell(wrapTable(card));
        wrap.setBorder(Rectangle.NO_BORDER);
        wrap.setPadding(5f);
        return wrap;
    }

    private PdfPTable wrapTable(PdfPCell cell) {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.addCell(cell);
        return t;
    }

    private PdfPCell receiptHeader() {

        PdfPCell c = new PdfPCell(new Phrase("매출전표", fonts.bold(10.5f, WHITE)));
        c.setBackgroundColor(BLUE);
        c.setBorder(Rectangle.NO_BORDER);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setPadding(8f);
        return c;
    }

    private PdfPCell groupLabel(String text) {

        PdfPCell c = new PdfPCell(new Phrase(text, fonts.bold(8.5f, BLUE)));
        c.setBorder(Rectangle.NO_BORDER);
        c.setPaddingTop(11f);
        c.setPaddingBottom(4f);
        c.setPaddingLeft(12f);
        return c;
    }

    private PdfPCell rows(String[][] data) throws DocumentException {

        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        // 항목 이름은 "사업자등록번호" 가 한 줄에 들어가는 만큼만 주고 나머지를 값에 넘긴다
        t.setWidths(new float[]{1f, 1.7f});

        Font labelFont = fonts.regular(7.5f, SUB);
        Font valueFont = fonts.regular(7.5f, INK);

        for (String[] row : data) {

            t.addCell(kv(row[0], labelFont, Element.ALIGN_LEFT));

            // WRAP 이 붙은 칸만 줄을 내린다.
            // 나머지는 한 줄만 차지해야 전표 높이가 서로 어긋나지 않는다
            boolean wrap = row.length > 2 && WRAP.equals(row[2]);
            String value = wrap ? nvl(row[1]) : fit(row[1], valueFont, RECEIPT_VALUE_WIDTH);

            t.addCell(kv(value, valueFont, Element.ALIGN_RIGHT));
        }

        PdfPCell wrap = new PdfPCell(t);
        wrap.setBorder(Rectangle.NO_BORDER);
        wrap.setPaddingLeft(12f);
        wrap.setPaddingRight(12f);
        return wrap;
    }

    private PdfPCell kv(String text, Font font, int align) {

        PdfPCell c = new PdfPCell(new Phrase(text != null ? text : "-", font));
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColorBottom(LINE);
        c.setBorderWidthBottom(0.4f);
        c.setHorizontalAlignment(align);
        c.setPaddingTop(5f);
        c.setPaddingBottom(5f);
        return c;
    }

    private PdfPCell totalRow(String amount) throws DocumentException {

        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{1f, 1.6f});

        PdfPCell l = new PdfPCell(new Phrase("합계", fonts.bold(10f, INK)));
        l.setBorder(Rectangle.NO_BORDER);
        l.setPaddingTop(8f);
        t.addCell(l);

        PdfPCell v = new PdfPCell(new Phrase(amount, fonts.bold(13f, INK)));
        v.setBorder(Rectangle.NO_BORDER);
        v.setHorizontalAlignment(Element.ALIGN_RIGHT);
        v.setPaddingTop(8f);
        t.addCell(v);

        PdfPCell wrap = new PdfPCell(t);
        wrap.setBorder(Rectangle.NO_BORDER);
        wrap.setPaddingLeft(12f);
        wrap.setPaddingRight(12f);
        wrap.setPaddingBottom(12f);
        return wrap;
    }

    // =====================================================================================
    // 마지막 페이지 현장 결제 건
    // =====================================================================================

    private void writeCardRecordPage(Document d, SettlementDocumentVO doc) throws DocumentException {

        List<WorkationExpenseVO> list = doc.getExpenses().stream()
                .filter(e -> e.getTransactionId() == null)
                .collect(Collectors.toList());

        if (list.isEmpty()) {
            return;
        }

        d.newPage();
        d.add(docTitle("현장 결제 건"));
        d.add(docSubtitle("매출전표 없음  ·  실물 영수증 별도 제출 대상"));
        d.add(rule());

        Paragraph note = new Paragraph(
                "앱을 거치지 않은 결제로 매출전표가 생성되지 않았습니다. "
                        + "아래 건은 실물 영수증을 따로 챙겨 이 문서와 함께 제출해 주세요.",
                fonts.regular(8.5f, SUB));
        note.setSpacingAfter(14f);
        d.add(note);

        PdfPTable t = new PdfPTable(5);
        t.setWidthPercentage(100);
        // 카드번호는 1111-****-****-4444 형태로 길이가 고정이라 줄일 수 없다.
        // 이 칸이 한 줄에 들어가도록 폭을 먼저 확보하고 나머지를 나눈다
        t.setWidths(new float[]{1.2f, 2.2f, 1.6f, 2.2f, 1.5f});

        head(t, "일자", Element.ALIGN_CENTER);
        head(t, "가맹점", Element.ALIGN_LEFT);
        head(t, "계정과목", Element.ALIGN_CENTER);
        head(t, "카드번호", Element.ALIGN_CENTER);
        head(t, "금액", Element.ALIGN_RIGHT);

        BigDecimal sum = BigDecimal.ZERO;

        for (WorkationExpenseVO e : list) {

            body(t, e.getSpentDate().format(DATE), Element.ALIGN_CENTER, SUB);
            // 가맹점 이름이 길면 그 행만 두 줄이 되어 표가 어긋난다
            body(t, fit(e.getMerchantName(), fonts.regular(9f, INK), 114f), Element.ALIGN_LEFT, INK);
            body(t, fit(e.getDisplayCategoryName(), fonts.regular(9f, SUB), 79f), Element.ALIGN_CENTER, SUB);
            body(t, maskedCard(e), Element.ALIGN_CENTER, SUB);
            body(t, money(e.getAmount()), Element.ALIGN_RIGHT, INK);

            sum = sum.add(e.getAmount());
        }

        PdfPCell label = new PdfPCell(new Phrase("합계", fonts.bold(9f, INK)));
        label.setColspan(4);
        label.setHorizontalAlignment(Element.ALIGN_RIGHT);
        label.setBackgroundColor(BAND);
        label.setBorder(Rectangle.NO_BORDER);
        label.setPadding(8f);
        t.addCell(label);
        total(t, money(sum), Element.ALIGN_RIGHT);

        d.add(t);
    }

    // =====================================================================================
    // 공통 요소
    // =====================================================================================

    private Paragraph docTitle(String text) {
        Paragraph p = new Paragraph(text, fonts.bold(19f, INK));
        p.setSpacingAfter(4f);
        return p;
    }

    private Paragraph docSubtitle(String text) {
        Paragraph p = new Paragraph(text, fonts.regular(9.5f, SUB));
        p.setSpacingAfter(10f);
        return p;
    }

    // 제목 아래 파란 선. 문서 전체의 기준선 역할을 한다
    private PdfPTable rule() {

        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingAfter(22f);

        PdfPCell c = new PdfPCell();
        c.setFixedHeight(2.4f);
        c.setBackgroundColor(BLUE);
        c.setBorder(Rectangle.NO_BORDER);
        t.addCell(c);
        return t;
    }

    private Paragraph sectionTitle(String text) {
        Paragraph p = new Paragraph(text, fonts.bold(11.5f, INK));
        p.setSpacingBefore(2f);
        p.setSpacingAfter(8f);
        return p;
    }

    private void info(PdfPTable t, String label, String value) {

        PdfPCell l = new PdfPCell(new Phrase(label, fonts.regular(8.5f, SUB)));
        l.setBorder(Rectangle.BOTTOM);
        l.setBorderColorBottom(LINE);
        l.setBorderWidthBottom(0.5f);
        l.setPadding(7f);
        t.addCell(l);

        Font valueFont = fonts.regular(9f, INK);
        PdfPCell v = new PdfPCell(new Phrase(fit(value, valueFont, INFO_VALUE_WIDTH), valueFont));
        v.setBorder(Rectangle.BOTTOM);
        v.setBorderColorBottom(LINE);
        v.setBorderWidthBottom(0.5f);
        v.setPadding(7f);
        t.addCell(v);
    }

    private void metric(PdfPTable t, String label, String value, Color valueColor) {

        PdfPTable inner = new PdfPTable(1);
        inner.setWidthPercentage(100);
        inner.addCell(plain(label, fonts.regular(8.5f, SUB), Element.ALIGN_LEFT, 0f));
        inner.addCell(plain(value, fonts.bold(14f, valueColor), Element.ALIGN_LEFT, 4f));

        PdfPCell wrap = new PdfPCell(inner);
        wrap.setBackgroundColor(BAND);
        wrap.setBorder(Rectangle.NO_BORDER);
        wrap.setPadding(12f);
        wrap.setPaddingLeft(14f);
        t.addCell(wrap);
    }

    private PdfPCell plain(String text, Font font, int align, float paddingTop) {

        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setBorder(Rectangle.NO_BORDER);
        c.setHorizontalAlignment(align);
        c.setPaddingTop(paddingTop);
        c.setPaddingBottom(0f);
        c.setPaddingLeft(0f);
        return c;
    }

    // 표 머리글. 세로줄 없이 위아래 선만 둬서 가볍게 보이게 한다
    private void head(PdfPTable t, String text, int align) {

        PdfPCell c = new PdfPCell(new Phrase(text, fonts.bold(8.5f, SUB)));
        c.setBorder(Rectangle.TOP | Rectangle.BOTTOM);
        c.setBorderColor(LINE);
        c.setBorderWidthTop(1.2f);
        c.setBorderWidthBottom(0.5f);
        c.setBorderColorTop(INK);
        c.setHorizontalAlignment(align);
        c.setPadding(8f);
        t.addCell(c);
    }

    private void body(PdfPTable t, String text, int align, Color color) {

        PdfPCell c = new PdfPCell(new Phrase(text != null ? text : "-", fonts.regular(9f, color)));
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColorBottom(LINE);
        c.setBorderWidthBottom(0.5f);
        c.setHorizontalAlignment(align);
        c.setPadding(7f);
        t.addCell(c);
    }

    private void total(PdfPTable t, String text, int align) {

        PdfPCell c = new PdfPCell(new Phrase(text, fonts.bold(9.5f, INK)));
        c.setBorder(Rectangle.NO_BORDER);
        c.setBackgroundColor(BAND);
        c.setHorizontalAlignment(align);
        c.setPadding(8f);
        t.addCell(c);
    }

    // =====================================================================================
    // 값 변환
    // =====================================================================================

    // 카드 별명이 길면 "별명 + 마스킹번호" 가 값 칸을 넘겨 줄이 내려간다.
    // 마스킹번호는 증빙에 반드시 남아야 하므로 폭이 모자라면 별명 쪽을 줄인다.
    private String paymentMethod(WorkationExpenseVO e) {

        if ("WALLET".equals(e.getPaymentSourceType())) {
            return "지갑결제";
        }
        if (e.getCardName() == null) {
            return "카드결제";
        }

        Font font = fonts.regular(7.5f, INK);
        String number = maskedCard(e);
        float rest = RECEIPT_VALUE_WIDTH - width(" " + number, font);

        return fit(e.getCardName(), font, rest) + " " + number;
    }

    // 지갑결제는 승인 절차가 없고, 카드결제는 매입까지 완료된 것으로 본다
    private String approvalStatus(WorkationExpenseVO e) {
        return "WALLET".equals(e.getPaymentSourceType()) ? "승인 없음" : "매입";
    }

    private String maskedCard(WorkationExpenseVO e) {

        String formatted = CardNumberFormatter.format(e.getCardNumber());
        return formatted != null ? formatted : "-";
    }

    private String rate(BigDecimal spent, BigDecimal target) {

        if (target == null || target.compareTo(BigDecimal.ZERO) == 0) {
            return "0.0";
        }
        return spent.multiply(BigDecimal.valueOf(100))
                .divide(target, 1, RoundingMode.HALF_UP).toPlainString();
    }

    private String money(BigDecimal amount) {
        return MONEY.format(amount != null ? amount : BigDecimal.ZERO) + "원";
    }

    private String nvl(String value) {
        return (value == null || value.trim().isEmpty()) ? "-" : value;
    }

    // =====================================================================================
    // 줄바꿈 막기
    //
    // PdfPCell 은 폭을 넘치면 알아서 줄을 내린다.
    // 표에서 한 칸만 두 줄이 되면 그 행만 높아져 문서 전체가 흐트러지므로,
    // 넣기 전에 폰트 실제 폭으로 재서 넘치는 만큼 잘라낸다.
    // =====================================================================================

    private float width(String text, Font font) {
        return font.getBaseFont().getWidthPoint(text, font.getSize());
    }

    private String fit(String text, Font font, float maxWidth) {

        if (text == null || text.trim().isEmpty()) {
            return "-";
        }
        if (maxWidth <= 0 || width(text, font) <= maxWidth) {
            return text;
        }

        float ellipsis = width("…", font);
        int end = text.length();

        while (end > 1 && width(text.substring(0, end), font) + ellipsis > maxWidth) {
            end--;
        }
        return text.substring(0, end).trim() + "…";
    }
}
