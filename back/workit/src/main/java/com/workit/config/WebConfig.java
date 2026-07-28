package com.workit.config;

import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletRegistration;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class WebConfig extends AbstractAnnotationConfigDispatcherServletInitializer {

    private final String uploadDir = loadUploadDir();

    final long MAX_FILE_SIZE = 1024 * 1024 * 10L;
    final long MAX_REQUEST_SIZE = 1024 * 1024 * 20L;
    final int FILE_SIZE_THRESHOLD = 1024 * 1024 * 5;

    private String loadUploadDir() {
        Properties props = new Properties();
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (input != null) {
                props.load(input);
            }
        } catch (IOException e) {
            throw new RuntimeException("application.properties 로드 실패", e);
        }
        return props.getProperty("file.upload-dir", "./uploads"); // 기본값 fallback
    }


    @Override
    protected Class<?>[] getRootConfigClasses() {
        return new Class[]{RootConfig.class};
    }

    @Override
    protected Class<?>[] getServletConfigClasses() {
        return new Class[]{ServletConfig.class};
    }

    @Override
    protected String[] getServletMappings() {
        return new String[]{"/"};
    }

    @Override
    protected void customizeRegistration(ServletRegistration.Dynamic registration) {
        registration.setInitParameter("throwExceptionIfNoHandlerFound", "true");
        MultipartConfigElement multipartConfig =
                new MultipartConfigElement(
                        uploadDir,
                        MAX_FILE_SIZE,
                        MAX_REQUEST_SIZE,
                        FILE_SIZE_THRESHOLD
                );
        registration.setMultipartConfig(multipartConfig);
    }
}