package com.workit.domain.settlement.service;

import com.lowagie.text.Font;
import com.lowagie.text.pdf.BaseFont;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.InputStream;

@Component
@Slf4j
public class PdfFontProvider {

    private static final String REGULAR_PATH = "fonts/Pretendard-Regular.ttf";
    private static final String BOLD_PATH = "fonts/Pretendard-Bold.ttf";

    // OpenPDF 기본 한국어 폰트. 뷰어가 알아서 대체하므로 모양이 환경마다 다를 수 있다
    private static final String FALLBACK_NAME = "HYGoThic-Medium";
    private static final String FALLBACK_ENCODING = "UniKS-UCS2-H";

    private final BaseFont regular;
    private final BaseFont bold;

    public PdfFontProvider() {
        this.regular = load(REGULAR_PATH);
        this.bold = load(BOLD_PATH);
    }

    private BaseFont load(String path) {

        try (InputStream in = new ClassPathResource(path).getInputStream()) {

            byte[] bytes = readAll(in);
            return BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED,
                    BaseFont.CACHED, bytes, null);

        } catch (Exception e) {
            log.warn("PDF 폰트 파일을 찾지 못해 기본 폰트를 사용합니다 - path: {}", path);
            return loadFallback();
        }
    }

    private BaseFont loadFallback() {
        try {
            return BaseFont.createFont(FALLBACK_NAME, FALLBACK_ENCODING, BaseFont.NOT_EMBEDDED);
        } catch (Exception e) {
            throw new IllegalStateException("PDF 한글 폰트를 준비할 수 없습니다.", e);
        }
    }

    private byte[] readAll(InputStream in) throws Exception {

        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int len;

        while ((len = in.read(buffer)) != -1) {
            out.write(buffer, 0, len);
        }
        return out.toByteArray();
    }

    public Font regular(float size, Color color) {
        return new Font(regular, size, Font.NORMAL, color);
    }

    public Font bold(float size, Color color) {
        return new Font(bold, size, Font.NORMAL, color);
    }
}
