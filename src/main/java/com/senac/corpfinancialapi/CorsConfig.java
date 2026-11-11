package com.senac.corpfinancialapi;

import java.util.Arrays;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final ApiRequestPolicy requestPolicy;
    private final String[] origins;

    CorsConfig(ApiRequestPolicy requestPolicy, @Value("${corp-financial-api.cors-origins}") String allowedOrigins) {
        this.requestPolicy = requestPolicy;
        this.origins = Arrays.stream(allowedOrigins.split(",")).map(String::strip).filter(origin -> !origin.isEmpty()).toArray(String[]::new);
        if (Arrays.asList(origins).contains("*")) {
            throw new IllegalArgumentException("CORP_API_CORS_ORIGINS must contain explicit origins");
        }
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(origins)
                .allowedMethods("GET", "HEAD", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders(HttpHeaders.ACCEPT, HttpHeaders.CONTENT_TYPE, ApiHeaders.API_KEY, ApiHeaders.ACCOUNT_ID, ApiHeaders.VERSION, ApiHeaders.IDEMPOTENCY_KEY)
                .exposedHeaders(HttpHeaders.LOCATION, HttpHeaders.RETRY_AFTER, ApiHeaders.RATE_LIMIT,
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
