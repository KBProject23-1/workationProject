package com.workit.domain.tourism.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class TourismRestTemplateConfig {

    @Bean("tourismRestTemplate")
    public RestTemplate tourismRestTemplate(
            @Value("${tourism.api.connect-timeout-ms:5000}") int connectTimeout,
            @Value("${tourism.api.read-timeout-ms:15000}") int readTimeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        return new RestTemplate(factory);
    }
}
