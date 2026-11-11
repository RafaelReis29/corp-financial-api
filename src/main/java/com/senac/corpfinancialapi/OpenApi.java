package com.senac.corpfinancialapi;

import java.util.List;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

@Configuration
public class OpenApi {

    private static final String KEY_SCHEME = "ApiKeyAuth";

    @Bean
    public OpenAPI customOpenAPI(@Value("${corp-financial-api.version}") String apiVersion) {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes(KEY_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.HEADER).name(ApiHeaders.API_KEY)
                                .description("Informe sua chave de API. Para explorar os dados fictícios, copie a chave pública da seção Demonstração.")))
                .info(new Info()
                        .title("Corp Financial API")
                        .version(apiVersion)
                        .description("""
                                API de contas a receber para operações B2B. Cadastre empresas associadas à sua conta, vincule contatos, meios de pagamento e contratos e registre faturas pelos contratos.

                                ### Demonstração
                                Em **Authorize**, cole `%s` e faça um GET em `/companies`. A chave Demo consulta apenas registros fictícios, com até 30 requisições por minuto para cada IP e até 300 por minuto somando todos os visitantes.

                                ### Planos por chave
                                Básica (`cf_basic_`) — R$ 49/mês e 60 requisições por minuto.

                                Premium (`cf_premium_`) — R$ 149/mês e 300 requisições por minuto.

                                Os dois planos permitem consultar, criar e atualizar registros na própria conta e excluir contatos, contratos, faturas e meios de pagamento sem dependências. As mensalidades são referências de plano. A API não realiza cobrança.

                                ### Administração
                                O Admin cria contas em `POST /accounts` e emite chaves em `POST /api-keys`. Para acessar registros de uma conta com a chave Admin, use `X-Account-Id` para escolher a conta. A exclusão de uma empresa exige esse cabeçalho e a chave Admin. Nas demais operações, o Admin acessa a conta de demonstração por padrão.
                                """.formatted(ApiKeys.PUBLIC_DEMO_KEY))
                        .termsOfService("/static/terms.html")
                        .license(new License().name("MIT").url("https://mit-license.org/"))
                        .contact(new Contact().name("Rafael Reis")
                                .url("https://www.linkedin.com/in/rafaeldnreis")
                                .email("rafael.dnreis@gmail.com"))
                );
    }

    @Bean
    OpenApiCustomizer documentRequestPolicy() {
        return api -> {
            api.getComponents().addSchemas("ApiProblem", new ObjectSchema()
                    .addProperty("type", new StringSchema().format("uri-reference"))
                    .addProperty("title", new StringSchema())
                    .addProperty("status", new IntegerSchema())
                    .addProperty("detail", new StringSchema())
                    .addProperty("instance", new StringSchema().format("uri-reference"))
                    .addProperty("errors", new ArraySchema().items(new ObjectSchema()
                            .addProperty("field", new StringSchema()).addProperty("message", new StringSchema()))));
            api.getPaths().forEach((path, item) ->
                    item.readOperationsMap().forEach((method, operation) -> documentOperation(path, method.name(), operation)));
        };
    }

    private static void documentOperation(String path, String verb, Operation operation) {
        boolean post = "POST".equals(verb);
        boolean writesBody = post || "PUT".equals(verb);
        if (operation.getParameters() != null) {
            operation.getParameters().removeIf(parameter -> ApiHeaders.VERSION.equals(parameter.getName()) && "header".equals(parameter.getIn()));
        }
        operation.addParametersItem(new HeaderParameter().name(ApiHeaders.VERSION).required(false)
                .description("Versão desta requisição. O servidor usa 2.0.0 quando o cabeçalho não é enviado.")
                .schema(new StringSchema()._default("2.0.0")._enum(List.of("1.0.0", "1.0.1", "2.0.0"))));
        if ("/companies/{id}".equals(path) && "GET".equals(verb)) {
            documentCompanyVersions(operation);
        }
        operation.addSecurityItem(new SecurityRequirement().addList(KEY_SCHEME));
        addProblem(operation, "401", "Chave de API ausente ou inválida")
                .addHeaderObject(HttpHeaders.WWW_AUTHENTICATE, new Header().schema(new StringSchema()));
        boolean management = ApiRequestPolicy.isManagement(path);
        boolean adminOnly = ApiRequestPolicy.requiresAdmin(path, verb);
        boolean delete = "DELETE".equals(verb);
        if (!management) {
            operation.addParametersItem(new HeaderParameter().name(ApiHeaders.ACCOUNT_ID).required(adminOnly)
                    .description(adminOnly ? "ID da conta proprietária. Obrigatório com a chave de administração."
                            : "Conta cujos registros o Admin quer acessar. O padrão do Admin é a demonstração. Chaves de cliente usam a conta da credencial.")
                    .schema(new StringSchema().format("uuid")));
        }
        if (adminOnly) {
            addProblem(operation, "403", "A operação exige a chave de administração");
        } else if (writesBody || delete) {
            addProblem(operation, "403", "A chave de demonstração permite somente consultas");
        }
        if (post) {
            operation.addParametersItem(new HeaderParameter().name(ApiHeaders.IDEMPOTENCY_KEY).required(false)
                    .description("Obrigatório no POST da versão 2.0.0. O Swagger preenche o campo vazio automaticamente. Reutilize a chave com o mesmo corpo nas tentativas por até 24 horas.")
                    .schema(new StringSchema().pattern("[A-Za-z0-9._~-]{1,128}")));
            operation.getResponses().get("201")
                    .addHeaderObject(ApiHeaders.IDEMPOTENCY_REPLAYED, new Header().description("Na versão 2.0.0, indica que a resposta original foi reaproveitada").schema(new StringSchema()._enum(List.of("true"))));
            addProblem(operation, "503", "Armazenamento temporário de idempotência da versão 2.0.0 indisponível");
        }
        if (operation.getResponses().containsKey("201")) {
            operation.getResponses().get("201").addHeaderObject(HttpHeaders.LOCATION,
                    new Header().description("URI do recurso criado").schema(new StringSchema().format("uri")));
        }
        if (writesBody) {
            addProblem(operation, "415", "Formato do corpo não aceito");
        }
        if (writesBody || (delete && !management)) {
            addProblem(operation, "409", post ? "Conflito de dados ou de chave idempotente" : "Conflito de dados");
        }
        addProblem(operation, "400", "Parâmetros, versão, identificador ou corpo inválido");
        if (!management) {
            ApiResponse notFound = addProblem(operation, "404", "Conta indicada pelo Admin não encontrada");
            if (delete) {
                notFound.setDescription("Registro não encontrado para a conta selecionada (corpo vazio) ou conta indicada pelo Admin não encontrada");
            } else if ("GET".equals(verb) && path.endsWith("/{id}")) {
                notFound.setDescription("Registro não encontrado para a conta selecionada ou conta indicada pelo Admin não encontrada");
            }
        } else if (operation.getResponses().containsKey("404")) {
            addProblem(operation, "404", operation.getResponses().get("404").getDescription());
        }
        addProblem(operation, "429", "Limite de requisições excedido")
                .addHeaderObject(HttpHeaders.RETRY_AFTER, integerHeader("Segundos até a próxima tentativa"));
        operation.getResponses().values().forEach(response -> response
                .addHeaderObject(ApiHeaders.RATE_LIMIT, integerHeader("Capacidade por minuto da cota informada na resposta"))
                .addHeaderObject(ApiHeaders.RATE_REMAINING, integerHeader("Requisições disponíveis no momento")));
    }

    private static void documentCompanyVersions(Operation operation) {
        operation.setSummary("Buscar empresa por id");
        operation.setDescription("A versão 2.0.0 agrupa os campos em company. As versões 1.0.0 e 1.0.1 retornam os campos da empresa na raiz.");
        operation.getResponses().get("200").getContent().values().forEach(media -> media.schema(new ComposedSchema()
                .addOneOfItem(schemaRef("EntityModelCompany"))
                .addOneOfItem(schemaRef("EntityModelCompanyProfile"))));
    }

    private static ApiResponse addProblem(Operation operation, String code, String description) {
        ApiResponse response = operation.getResponses().computeIfAbsent(code, status -> new ApiResponse().description(description));
        return response.content(new Content().addMediaType("application/problem+json",
                new MediaType().schema(schemaRef("ApiProblem"))));
    }

    private static Schema<?> schemaRef(String name) {
        return new Schema<>().$ref("#/components/schemas/" + name);
    }

    private static Header integerHeader(String description) {
        return new Header().description(description).schema(new IntegerSchema().format("int64"));
    }
}
