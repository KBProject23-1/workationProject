package com.workit.global.util;

import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;

public class UploadFiles {
    public static String upload(String baseDir, MultipartFile part) throws IOException {

        // 상대 경로가 Servlet 임시 경로에 다시 결합되지 않도록 절대 경로를 사용한다.
        Path basePath = Paths.get(baseDir).toAbsolutePath().normalize();
        Files.createDirectories(basePath);
        String fileName = part.getOriginalFilename();
        Path destination = basePath.resolve(UploadFileName.getUniqueName(fileName)).normalize();
        if (!destination.startsWith(basePath)) {
            throw new IOException("허용되지 않은 파일 저장 경로입니다.");
        }
        part.transferTo(destination.toFile());
        return destination.toString();
    }

    public static String getFormatSize(Long size) {
        if (size <= 0)
            return "0";
        final String[] units = new String[]{"Bytes", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }


    public static void download(HttpServletResponse response, File file, String orgName) throws Exception {

        response.setContentType("application/download");
        response.setContentLength((int) file.length());

        String filename = URLEncoder.encode(orgName, StandardCharsets.UTF_8);    // 한글 파일명인 경우 인코딩 필수
        response.setHeader("Content-disposition", "attachment;filename=\"" + filename + "\"");

        try (OutputStream os = response.getOutputStream();
             BufferedOutputStream bos = new BufferedOutputStream(os)) {
            Files.copy(Paths.get(file.getPath()), bos);
        }
    }

    public static void downloadImage(HttpServletResponse response, File file) {
        try {
            Path path = Path.of(file.getPath());
            String mimeType = Files.probeContentType(path);
            response.setContentType(mimeType);
            response.setContentLength((int) file.length());

            try (OutputStream os = response.getOutputStream();
                 BufferedOutputStream bos = new BufferedOutputStream(os)) {
                Files.copy(path, bos);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
