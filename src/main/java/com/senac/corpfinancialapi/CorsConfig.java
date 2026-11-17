package com.senac.corpfinancialapi;

import java.util.stream.Stream;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final ApiRequestPolicy requestPolicy;

    CorsConfig(ApiRequestPolicy requestPolicy) {
        this.requestPolicy = requestPolicy;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "HEAD", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders(HttpHeaders.ACCEPT, HttpHeaders.CONTENT_TYPE, ApiHeaders.API_KEY, ApiHeaders.ACCOUNT_ID, ApiHeaders.VERSION, ApiHeaders.IDEMPOTENCY_KEY)
                .exposedHeaders(HttpHeaders.RETRY_AFTER, ApiHeaders.RATE_LIMIT,
                        ApiHeaders.RATE_REMAINING, ApiHeaders.IDEMPOTENCY_REPLAYED);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        String[] paths = ApiRequestPolicy.COLLECTIONS.stream()
                .flatMap(path -> Stream.of(path, path + "/*"))
                .toArray(String[]::new);
        registry.addInterceptor(requestPolicy).addPathPatterns(paths);
    }
}
