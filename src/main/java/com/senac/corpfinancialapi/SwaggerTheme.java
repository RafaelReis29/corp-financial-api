package com.senac.corpfinancialapi;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletRequest;

import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springdoc.webmvc.ui.SwaggerIndexPageTransformer;
import org.springdoc.webmvc.ui.SwaggerIndexTransformer;
import org.springdoc.webmvc.ui.SwaggerWelcomeCommon;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.resource.ResourceTransformerChain;
import org.springframework.web.servlet.resource.TransformedResource;

@Configuration
public class SwaggerTheme {

    @Bean
    SwaggerIndexTransformer themedSwaggerIndexTransformer(
            SwaggerUiConfigProperties swaggerUiConfig,
            SwaggerUiOAuthProperties swaggerUiOAuthProperties,
            SwaggerWelcomeCommon swaggerWelcomeCommon,
            ObjectMapperProvider objectMapperProvider) {
        return new ThemeInjectingTransformer(swaggerUiConfig, swaggerUiOAuthProperties, swaggerWelcomeCommon, objectMapperProvider);
    }

    static class ThemeInjectingTransformer extends SwaggerIndexPageTransformer {

        ThemeInjectingTransformer(SwaggerUiConfigProperties swaggerUiConfig,
                SwaggerUiOAuthProperties swaggerUiOAuthProperties,
                SwaggerWelcomeCommon swaggerWelcomeCommon,
                ObjectMapperProvider objectMapperProvider) {
            super(swaggerUiConfig, swaggerUiOAuthProperties, swaggerWelcomeCommon, objectMapperProvider);
        }

        @Override
        public Resource transform(HttpServletRequest request, Resource resource,
                ResourceTransformerChain transformerChain) throws IOException {
            Resource transformed = super.transform(request, resource, transformerChain);
            String url = resource.getURL().toString();
            if (url.contains("swagger-ui") && url.endsWith("/index.html")) {
                String assetsPath = request.getContextPath() + "/static/";
                String assets = """
                          <link rel="stylesheet" href="%sswagger-theme.css?v=8">
                          <script src="%sswagger-idempotency.js?v=2"></script>
                        </head>""".formatted(assetsPath, assetsPath);
                return replace(transformed, "</head>", assets);
            }
            if (url.contains("swagger-ui") && url.endsWith("/swagger-initializer.js")) {
                Resource initializer = replace(transformed, "SwaggerUIBundle.plugins.DownloadUrl",
                        "SwaggerUIBundle.plugins.DownloadUrl,\n      window.corpIdempotencyPlugin");
                return replace(initializer, "plugins: [", "requestInterceptor: window.corpSwaggerRequestInterceptor,\n    plugins: [");
            }
            return transformed;
        }

        private Resource replace(Resource resource, String original, String replacement) throws IOException {
            try (InputStream inputStream = resource.getInputStream()) {
                String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                return new TransformedResource(resource, content.replace(original, replacement).getBytes(StandardCharsets.UTF_8));
            }
        }
    }
}
