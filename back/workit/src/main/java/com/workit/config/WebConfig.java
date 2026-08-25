package com.workit.config;

import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;
import org.springframework.web.filter.DelegatingFilterProxy;
import org.springframework.web.filter.CharacterEncodingFilter;

import javax.servlet.Filter;
import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletRegistration;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        String configuredUploadDir = props.getProperty("file.upload-dir", "./uploads");
        Path uploadPath = Paths.get(configuredUploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadPath);
        } catch (IOException e) {
            throw new IllegalStateException("파일 업로드 디렉터리를 생성할 수 없습니다.", e);
        }
        return uploadPath.toString();
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

    /**
     * Spring Security 필터 체인 배선
     * - springSecurityFilterChain 빈은 ROOT 컨텍스트(RootConfig → SecurityConfig)에서 생성된다
     * - DelegatingFilterProxy 는 ROOT WebApplicationContext 에서 동일 이름의 빈을 찾아 위임한다
     */
    @Override
    protected Filter[] getServletFilters() {
        // multipart/form-data의 한글 일반 필드도 UTF-8로 해석되도록 가장 먼저 적용한다.
        CharacterEncodingFilter encodingFilter = new CharacterEncodingFilter();
        encodingFilter.setEncoding("UTF-8");
        encodingFilter.setForceEncoding(true);
        return new Filter[]{
                encodingFilter,
                new DelegatingFilterProxy("springSecurityFilterChain")
        };
    }

    @Override
    protected void customizeRegistration(ServletRegistration.Dynamic registration) {
        // 서블릿을 시작 시점에 초기화 — NoHandlerRequestMatcher 가 DispatcherServlet 컨텍스트 속성을
        // 참조하므로, 첫 요청(지연 초기화)에 컨텍스트 속성이 없어 404 대신 401로 응답하는 것을 방지한다
        registration.setLoadOnStartup(1);
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
