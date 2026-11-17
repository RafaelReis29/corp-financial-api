package com.senac.corpfinancialapi;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static com.senac.corpfinancialapi.HttpResponsesTests.ADMIN_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "corp-financial-api.admin-key=" + ADMIN_KEY)
@AutoConfigureMockMvc
class AdvancedApiTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private CompanyRepository companies;

    @Autowired
    private ContactRepository contacts;

    @Autowired
    private AccountRepository accounts;

    @Test
    void adminIssuesListsAndRevokesCredentialsWithoutExposingStoredSecrets() throws Exception {
        mvc.perform(get("/invoices"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists(HttpHeaders.WWW_AUTHENTICATE))
                .andExpect(jsonPath("$.status").value(401));
        mvc.perform(post("/api-keys").contentType(MediaType.APPLICATION_JSON).content("{\"label\":\"Example\"}"))
                .andExpect(status().isUnauthorized());

        UUID accountId = accounts.save(new Account("Finance account", false)).id();
        MvcResult creation = mvc.perform(post("/api-keys").header(ApiHeaders.API_KEY, ADMIN_KEY)
                        .header(ApiHeaders.IDEMPOTENCY_KEY, UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"label":"Finance team","accountId":"%s","type":"BASIC"}
                                """.formatted(accountId)))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(header().doesNotExist(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.metadata.label").value("Finance team"))
                .andReturn();
        String credential = JsonPath.read(creation.getResponse().getContentAsString(), "$.apiKey");
        String keyId = JsonPath.read(creation.getResponse().getContentAsString(), "$.metadata.id");
        String keyPath = "/api-keys/" + keyId;

        mvc.perform(get("/invoices").header(ApiHeaders.API_KEY, credential))
                .andExpect(status().isOk());
        mvc.perform(get("/api-keys").header(ApiHeaders.API_KEY, credential))
                .andExpect(status().isForbidden());
        mvc.perform(get(keyPath).header(ApiHeaders.API_KEY, ADMIN_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.label").value("Finance team"))
                .andExpect(jsonPath("$.apiKey").doesNotExist())
                .andExpect(jsonPath("$.secretHash").doesNotExist());
        mvc.perform(get("/api-keys").header(ApiHeaders.API_KEY, ADMIN_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].label").value("Finance team"))
                .andExpect(jsonPath("$[0].apiKey").doesNotExist());
        mvc.perform(delete(keyPath).header(ApiHeaders.API_KEY, ADMIN_KEY))
                .andExpect(status().isNoContent());
        mvc.perform(get("/invoices").header(ApiHeaders.API_KEY, credential))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void idempotencyReplaysCreationAndRejectsDifferentData() throws Exception {
        String key = UUID.randomUUID().toString();
        String body = companyBody("TEST-REPLAY");
        long count = companies.count();
        MvcResult first = create("/companies", key, body);
        assertThat(first.getResponse().getStatus()).isEqualTo(201);
        MvcResult replay = create("/companies", key, body);
        assertThat(replay.getResponse().getStatus()).isEqualTo(201);
        assertThat(replay.getResponse().getHeader(ApiHeaders.IDEMPOTENCY_REPLAYED)).isEqualTo("true");
        assertThat(first.getResponse().getHeader(HttpHeaders.LOCATION)).isNull();
        assertThat(replay.getResponse().getHeader(HttpHeaders.LOCATION)).isNull();
        assertThat(replay.getResponse().getContentAsString()).isEqualTo(first.getResponse().getContentAsString());
        assertThat(create("/companies", key, companyBody("TEST-CHANGED")).getResponse().getStatus()).isEqualTo(409);
        assertThat(companies.count()).isEqualTo(count + 1);
    }

    @Test
    void concurrentRetriesCreateOnlyOneRecord() throws Exception {
        String key = UUID.randomUUID().toString();
        String body = "{\"companyId\":1,\"name\":\"Concurrent Contact\"}";
        long count = contacts.count();
        var executor = Executors.newFixedThreadPool(2);
        try {
            var start = new CountDownLatch(1);
            var first = executor.submit(() -> {
                start.await();
                return create("/contacts", key, body).getResponse();
            });
            var second = executor.submit(() -> {
                start.await();
                return create("/contacts", key, body).getResponse();
            });
            start.countDown();
            var responses = List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
            assertThat(responses).anyMatch(response -> response.getStatus() == 201);
            assertThat(responses).allMatch(response -> response.getStatus() == 201 || response.getStatus() == 409);
            assertThat(contacts.count()).isEqualTo(count + 1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void failedCreationDoesNotReserveTheIdempotencyKey() throws Exception {
        String key = UUID.randomUUID().toString();
        assertThat(create("/companies", key, companyBody("10001001000101")).getResponse().getStatus()).isEqualTo(409);
        assertThat(create("/companies", key, companyBody("TEST-CORRECTED")).getResponse().getStatus()).isEqualTo(201);
    }

    @Test
    void invalidInputAndMissingReferencesReturnProblemDetails() throws Exception {
        mvc.perform(post("/invoices").header(ApiHeaders.API_KEY, ADMIN_KEY)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[?(@.field == 'amount')]").isNotEmpty())
                .andExpect(jsonPath("$.errors[?(@.field == 'contractId')]").isNotEmpty());
        mvc.perform(post("/contacts").header(ApiHeaders.API_KEY, ADMIN_KEY)
                        .header(ApiHeaders.IDEMPOTENCY_KEY, UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"companyId\":999999,\"name\":\"Contact Example\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
        mvc.perform(delete("/companies/1").header(ApiHeaders.API_KEY, ADMIN_KEY)
                        .header(ApiHeaders.ACCOUNT_ID, accounts.findByDemoTrue().orElseThrow().id()))
                .andExpect(status().isConflict());
        mvc.perform(get("/companies/0").header(ApiHeaders.API_KEY, ADMIN_KEY))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/companies").header(ApiHeaders.API_KEY, ADMIN_KEY).param("page", "-1").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(2));
        mvc.perform(get("/companies").header(ApiHeaders.API_KEY, ADMIN_KEY).param("size", "invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("size")));
    }

    @Test
    void companyDetailSelectsTheRepresentationByVersion() throws Exception {
        mvc.perform(get("/companies/1").header(ApiHeaders.API_KEY, ADMIN_KEY))
                .andExpect(status().isOk())
                .andExpect(header().stringValues(HttpHeaders.VARY, hasItem(ApiHeaders.VERSION)))
                .andExpect(jsonPath("$.company.tradeName").exists())
                .andExpect(jsonPath("$.tradeName").doesNotExist());
        mvc.perform(get("/companies/1").header(ApiHeaders.API_KEY, ADMIN_KEY).header(ApiHeaders.VERSION, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tradeName").exists());
        mvc.perform(get("/companies/1").header(ApiHeaders.API_KEY, ADMIN_KEY).header(ApiHeaders.VERSION, "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.company.tradeName").exists())
                .andExpect(jsonPath("$.tradeName").doesNotExist());
        mvc.perform(get("/companies/1").header(ApiHeaders.API_KEY, ADMIN_KEY).header(ApiHeaders.VERSION, "9"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void bothCompanyReadsRequireAuthentication() throws Exception {
        mvc.perform(get("/companies")).andExpect(status().isUnauthorized());
        mvc.perform(get("/companies/1").header(ApiHeaders.API_KEY, "invalid"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/companies").header(ApiHeaders.API_KEY, ADMIN_KEY))
                .andExpect(status().isOk());
    }

    @Test
    void collectionsAndWritesRejectUnsupportedVersions() throws Exception {
        mvc.perform(get("/contacts").header(ApiHeaders.API_KEY, ADMIN_KEY).header(ApiHeaders.VERSION, "1"))
                .andExpect(status().isOk());
        mvc.perform(get("/api-keys").header(ApiHeaders.API_KEY, ADMIN_KEY).header(ApiHeaders.VERSION, "2"))
                .andExpect(status().isOk());
        var requests = List.of(get("/contacts"), post("/companies"), put("/invoices/1"), delete("/payment-methods/1"));
        for (var request : requests) {
            mvc.perform(request.header(ApiHeaders.API_KEY, ADMIN_KEY).header(ApiHeaders.VERSION, "9"))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.detail").value(containsString("version")));
        }
        mvc.perform(get("/contracts").header(ApiHeaders.API_KEY, ADMIN_KEY).header(ApiHeaders.VERSION, "unknown"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void onlyVersionTwoRequiresAndUsesTheIdempotencyKey() throws Exception {
        String body = "{\"companyId\":1,\"name\":\"Unkeyed Contact\"}";
        long count = contacts.count();
        mvc.perform(post("/contacts").header(ApiHeaders.API_KEY, ADMIN_KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString(ApiHeaders.IDEMPOTENCY_KEY)));
        assertThat(contacts.count()).isEqualTo(count);

        for (String version : List.of("1.0.0", "1.0.1")) {
            mvc.perform(get("/companies/1").header(ApiHeaders.API_KEY, ADMIN_KEY).header(ApiHeaders.VERSION, version))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tradeName").exists());
            mvc.perform(post("/contacts").header(ApiHeaders.API_KEY, ADMIN_KEY).header(ApiHeaders.VERSION, version)
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isCreated());
            mvc.perform(post("/contacts").header(ApiHeaders.API_KEY, ADMIN_KEY).header(ApiHeaders.VERSION, version)
                            .header(ApiHeaders.IDEMPOTENCY_KEY, "ignored-in-v1")
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isCreated())
                    .andExpect(header().doesNotExist(ApiHeaders.IDEMPOTENCY_REPLAYED));
        }
        assertThat(contacts.count()).isEqualTo(count + 4);
    }

    @Test
    void corsAllowsAnyOriginAndCustomHeaders() throws Exception {
        mvc.perform(options("/companies")
                        .header(HttpHeaders.ORIGIN, "https://rafaelreis29.github.io")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS,
                                "Content-Type,X-API-Key,X-API-Version,X-Account-Id,X-Idempotency-Key"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, containsString(ApiHeaders.ACCOUNT_ID)))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, containsString(ApiHeaders.IDEMPOTENCY_KEY)));
        mvc.perform(get("/companies")
                        .header(HttpHeaders.ORIGIN, "https://rafaelreis29.github.io")
                        .header(ApiHeaders.API_KEY, ApiKeys.PUBLIC_DEMO_KEY)
                        .header(ApiHeaders.VERSION, "2.0.0"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*"));
        mvc.perform(get("/accounts")
                        .header(HttpHeaders.ORIGIN, "https://rafaelreis29.github.io")
                        .header(ApiHeaders.API_KEY, "invalid"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*"));
        mvc.perform(options("/invoices")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type,X-API-Key,X-Idempotency-Key,X-API-Version"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, containsString(ApiHeaders.API_KEY)));
        mvc.perform(options("/accounts")
                        .header(HttpHeaders.ORIGIN, "http://127.0.0.1:3000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "X-API-Key,X-API-Version"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*"));
        mvc.perform(options("/invoices")
                        .header(HttpHeaders.ORIGIN, "https://unlisted.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*"));
    }

    @Test
    void rejectsSortsOnFieldsOutsideTheResource() throws Exception {
        mvc.perform(get("/companies").header(ApiHeaders.API_KEY, ADMIN_KEY).param("sort", "account.name,asc"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/companies").header(ApiHeaders.API_KEY, ADMIN_KEY).param("sort", "tradeName,desc"))
                .andExpect(status().isOk());
    }

    @Test
    void swaggerExposesSecurityHeadersAndBothVersions() throws Exception {
        mvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/static/swagger-idempotency.js")));
        mvc.perform(get("/swagger-ui/swagger-initializer.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("window.corpIdempotencyPlugin")));
        mvc.perform(get("/static/swagger-idempotency.js"))
                .andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.ApiKeyAuth.name").value(ApiHeaders.API_KEY))
                .andExpect(jsonPath("$.info.description").value(containsString(ApiKeys.PUBLIC_DEMO_KEY)))
                .andExpect(jsonPath("$.paths['/accounts'].post.responses['403']").exists())
                .andExpect(jsonPath("$.paths['/api-keys'].post.security[0].ApiKeyAuth").exists())
                .andExpect(jsonPath("$.paths['/companies/{id}'].delete.parameters[?(@.name == 'X-Account-Id')].required")
                        .value(hasItem(true)))
                .andExpect(jsonPath("$.paths['/contacts/{id}'].delete.responses['403']").exists())
                .andExpect(jsonPath("$.paths['/companies'].get.security[0].ApiKeyAuth").exists())
                .andExpect(jsonPath("$.paths['/invoices'].post.parameters[?(@.name == 'X-Idempotency-Key')]").isNotEmpty())
                .andExpect(jsonPath("$.paths['/invoices'].post.responses['429'].headers['Retry-After']").exists())
                .andExpect(jsonPath("$.paths['/companies/{id}'].get.parameters[?(@.name == 'X-API-Version')]").isNotEmpty())
                .andExpect(jsonPath("$.paths['/contacts'].post.parameters[?(@.name == 'X-API-Version')].schema.default").value(hasItem("2.0.0")))
                .andExpect(jsonPath("$.paths['/companies/{id}'].get.responses['200'].content['*/*'].schema.oneOf.length()").value(2))
                .andExpect(jsonPath("$.components.schemas.EntityModelCompanyProfile.properties.company").exists())
                .andExpect(jsonPath("$.components.schemas.EntityModelCompanyProfile.required[0]").value("company"))
                .andExpect(jsonPath("$.components.schemas.ApiProblem.properties.errors").exists());
    }

    private MvcResult create(String path, String key, String body) throws Exception {
        return mvc.perform(post(path).header(ApiHeaders.API_KEY, ADMIN_KEY)
                        .header(ApiHeaders.IDEMPOTENCY_KEY, key)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn();
    }

    private static String companyBody(String docNumber) {
        return "{\"tradeName\":\"Test Company\",\"docNumber\":\"" + docNumber + "\"}";
    }
}
