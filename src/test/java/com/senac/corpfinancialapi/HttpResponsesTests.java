package com.senac.corpfinancialapi;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "corp-financial-api.admin-key=" + HttpResponsesTests.ADMIN_KEY,
        "corp-financial-api.requests-per-minute=1000"
})
@AutoConfigureMockMvc
class HttpResponsesTests {

    static final String ADMIN_KEY = "test-administrator-key-0123456789";
    private static final long MISSING_ID = Long.MAX_VALUE;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private AccountRepository accounts;

    @Test
    void openApiReportsReleaseVersion() throws Exception {
        perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.version").value("2.0.0"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/companies", "/contacts", "/contracts", "/invoices", "/payment-methods"})
    void openApiDocumentsResourceResponses(String path) throws Exception {
        String collection = "$.paths['" + path + "']";
        String item = "$.paths['" + path + "/{id}']";

        perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(collection + ".get.responses['200']").exists())
                .andExpect(jsonPath(collection + ".get.responses['400']").exists())
                .andExpect(jsonPath(collection + ".get.responses['404']").doesNotExist())
                .andExpect(jsonPath(collection + ".post.responses['404']").doesNotExist())
                .andExpect(jsonPath(collection + ".post.responses['201'].headers.Location").doesNotExist())
                .andExpect(jsonPath(item + ".put.responses['404']").doesNotExist())
                .andExpect(jsonPath(item + ".put.responses['201'].headers.Location").doesNotExist())
                .andExpect(jsonPath(item + ".get.responses['400']").exists())
                .andExpect(jsonPath(item + ".get.responses['404'].content['application/problem+json'].schema['$ref']").value("#/components/schemas/ApiProblem"))
                .andExpect(jsonPath(item + ".delete.responses['400']").exists())
                .andExpect(jsonPath(item + ".delete.responses['404']").exists())
                .andExpect(jsonPath(item + ".delete.responses['404'].content").doesNotExist())
                .andExpect(jsonPath(item + ".delete.responses['204']").exists())
                .andExpect(jsonPath(item + ".delete.responses['204'].content").doesNotExist());
    }

    @Test
    void openApiLimitsConflictResponsesToOperationsThatCanConflict() throws Exception {
        String spec = perform(get("/v3/api-docs")).andReturn().getResponse().getContentAsString();
        for (String path : List.of("companies", "contacts", "contracts", "invoices", "payment-methods")) {
            Map<String, Object> collection = JsonPath.read(spec, "$.paths['/" + path + "'].get.responses");
            assertThat(collection.keySet()).containsExactlyInAnyOrder("200", "400", "401", "429");
            Map<String, Object> deletion = JsonPath.read(spec, "$.paths['/" + path + "/{id}'].delete.responses");
            assertThat(deletion.containsKey("409")).isEqualTo(Set.of("companies", "contracts", "payment-methods").contains(path));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"/accounts", "/api-keys", "/companies", "/contacts", "/contracts", "/invoices", "/payment-methods"})
    void openApiIdentifiesResponsesExclusiveToVersionTwo(String path) throws Exception {
        String post = "$.paths['" + path + "'].post.responses";
        var result = perform(get("/v3/api-docs"))
                .andExpect(jsonPath(post + "['503']['x-api-versions'][0]").value("2.0.0"))
                .andExpect(jsonPath(post + "['201'].headers['Idempotency-Replayed']['x-api-versions'][0]").value("2.0.0"));
        if (path.equals("/accounts") || path.equals("/api-keys")) {
            result.andExpect(jsonPath(post + "['409']['x-api-versions'][0]").value("2.0.0"));
        } else {
            result.andExpect(jsonPath(post + "['409']['x-api-versions']").doesNotExist());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"/companies", "/contacts", "/contracts", "/invoices", "/payment-methods"})
    void emptyPageAndMissingItemHaveDifferentStatuses(String path) throws Exception {
        perform(get(path).param("page", "999999").param("sort", "id,desc").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.number").value(999999))
                .andExpect(jsonPath("$._embedded").doesNotExist());

        perform(get(path + "/{id}", MISSING_ID))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        perform(delete(path + "/{id}", MISSING_ID))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1.0.1", "2.0.0"})
    void correctedVersionsReturnEmptyListsAndMissingItemsAsDocumented(String version) throws Exception {
        for (String path : List.of("/companies", "/contacts", "/contracts", "/invoices", "/payment-methods")) {
            perform(get(path).header(ApiHeaders.VERSION, version).param("page", "999999"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded").doesNotExist());
            perform(get(path + "/{id}", MISSING_ID).header(ApiHeaders.VERSION, version))
                    .andExpect(status().isNotFound());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"/companies", "/contacts", "/contracts", "/invoices", "/payment-methods"})
    void malformedRequestsReturnBadRequest(String path) throws Exception {
        perform(get(path).param("sort", "unknownField,asc"))
                .andExpect(status().isBadRequest());

        perform(get(path).param("sort", "class,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        perform(get(path + "/invalid"))
                .andExpect(status().isBadRequest());

        perform(delete(path + "/invalid"))
                .andExpect(status().isBadRequest());

        perform(post(path).contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());

        perform(put(path + "/1").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            /companies | {"tradeName":"Test Company","docNumber":"TEST-001"}
            /contacts | {"companyId":1,"name":"Test Contact"}
            /contracts | {"companyId":1,"title":"Test Contract","totalValue":100.00,"currency":"BRL","startDate":"2026-11-01","status":"ATIVO"}
            /invoices | {"contractId":1,"description":"Test Invoice","amount":10.00,"dueDate":"2026-11-30","status":"PENDENTE"}
            /payment-methods | {"companyId":1,"type":"PIX","label":"Test Account"}
            """)
    void creationUpdateAndDeletionReturnDocumentedStatuses(String path, String body) throws Exception {
        String createdBody = perform(post(path).header(ApiHeaders.IDEMPOTENCY_KEY, UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Location"))
                .andReturn().getResponse().getContentAsString();
        URI item = URI.create(JsonPath.read(createdBody, "$._links.self.href"));

        perform(get(item))
                .andExpect(status().isOk());

        perform(put(item).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());

        perform(delete(item))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        String createdByPut = perform(put(path + "/{id}", MISSING_ID).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Location"))
                .andReturn().getResponse().getContentAsString();
        URI createdItem = URI.create(JsonPath.read(createdByPut, "$._links.self.href"));
        assertThat(createdItem.getPath()).startsWith(path + "/").doesNotEndWith("/" + MISSING_ID);
        perform(get(createdItem)).andExpect(status().isOk());
        perform(delete(createdItem)).andExpect(status().isNoContent());
    }

    private ResultActions perform(MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request.header(ApiHeaders.API_KEY, ADMIN_KEY)
                .header(ApiHeaders.ACCOUNT_ID, accounts.findByDemoTrue().orElseThrow().id()));
    }
}
