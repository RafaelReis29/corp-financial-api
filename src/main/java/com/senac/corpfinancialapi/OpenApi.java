package com.senac.corpfinancialapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApi {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Corp Financial API")
                        .version("1.0")
                        .description("API de um sistema financeiro corporativo B2B para gestão de empresas, contatos, contratos, meios de pagamento e faturas. A base inicial contém empresas e personagens da cultura pop para facilitar os testes. Fluxo sugerido: crie uma empresa, crie um contato e um meio de pagamento com o companyId retornado, crie um contrato e por fim crie faturas com o contractId retornado.")
                        .termsOfService("/static/terms.html")
                        .license(new License().name("MIT").url("https://mit-license.org/"))
                        .contact(new Contact().name("Rafael Reis")
                                .url("https://www.linkedin.com/in/rafaeldnreis")
                                .email("rafael.dnreis@gmail.com"))
                );
    }
}
