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

/**
 * Injeta o tema visual próprio (verde menta sobre roxo escuro) no
 * Swagger UI, via transformer da página index.
 *
 * <p>O bean abaixo substitui o transformer padrão do springdoc
 * (o bean original é {@code @ConditionalOnMissingBean}), mantendo a
 * transformação original do {@code swagger-initializer.js} e
 * adicionando apenas um {@code <link>} para
 * {@code /static/swagger-theme.css} no {@code index.html}.</p>
 */
@Configuration
public class SwaggerTheme {

    private static final String THEME_LINK = "<link rel=\"stylesheet\" type=\"text/css\" href=\"/static/swagger-theme.css?v=3\">";

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
            boolean isSwaggerIndex = url.contains("swagger-ui") && url.endsWith("/index.html");
            if (isSwaggerIndex) {
                try (InputStream inputStream = transformed.getInputStream()) {
                    String html = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                    if (!html.contains("swagger-theme.css") && html.contains("</head>")) {
                        html = html.replace("</head>", "  " + THEME_LINK + "\n</head>");
                        return new TransformedResource(transformed, html.getBytes(StandardCharsets.UTF_8));
                    }
                }
            }
            return transformed;
        }
    }
}
