package com.senac.corpfinancialapi;

import java.net.URI;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class HttpResponsesTests {

    private static final long MISSING_ID = Long.MAX_VALUE;

    @Autowired
    private MockMvc mvc;

    @Test
    void openApiReportsReleaseVersion() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.version").value("1.0.1"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/companies", "/contacts", "/contracts", "/invoices", "/payment-methods"})
    void openApiOnlyDocumentsNotFoundForOperationsOnExistingResources(String path) throws Exception {
        String collection = "$.paths['" + path + "']";
        String item = "$.paths['" + path + "/{id}']";

        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(collection + ".get.responses['200']").exists())
                .andExpect(jsonPath(collection + ".get.responses['400']").exists())
                .andExpect(jsonPath(collection + ".get.responses['404']").doesNotExist())
                .andExpect(jsonPath(collection + ".post.responses['404']").doesNotExist())
                .andExpect(jsonPath(collection + ".post.responses['201'].headers.Location").exists())
                .andExpect(jsonPath(item + ".put.responses['404']").doesNotExist())
                .andExpect(jsonPath(item + ".put.responses['201'].headers.Location").exists())
                .andExpect(jsonPath(item + ".get.responses['400']").exists())
                .andExpect(jsonPath(item + ".get.responses['404'].content['text/plain'].schema.type").value("string"))
                .andExpect(jsonPath(item + ".delete.responses['400']").exists())
                .andExpect(jsonPath(item + ".delete.responses['404']").exists())
                .andExpect(jsonPath(item + ".delete.responses['404'].content").doesNotExist())
                .andExpect(jsonPath(item + ".delete.responses['204']").exists())
                .andExpect(jsonPath(item + ".delete.responses['204'].content").doesNotExist());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/companies", "/contacts", "/contracts", "/invoices", "/payment-methods"})
    void emptyPageAndMissingItemHaveDifferentStatuses(String path) throws Exception {
        mvc.perform(get(path).param("page", "999999").param("sort", "id,desc").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.number").value(999999))
                .andExpect(jsonPath("$._embedded").doesNotExist());

        mvc.perform(get(path + "/{id}", MISSING_ID))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN));

        mvc.perform(delete(path + "/{id}", MISSING_ID))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/companies", "/contacts", "/contracts", "/invoices", "/payment-methods"})
    void malformedRequestsReturnBadRequest(String path) throws Exception {
        mvc.perform(get(path).param("sort", "unknownField,asc"))
                .andExpect(status().isBadRequest());

        mvc.perform(get(path + "/invalid"))
                .andExpect(status().isBadRequest());

        mvc.perform(delete(path + "/invalid"))
                .andExpect(status().isBadRequest());

        mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());

        mvc.perform(put(path + "/1").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            /companies | {"tradeName":"Test Company","docNumber":"TEST-001"}
            /contacts | {"companyId":1,"name":"Test Contact"}
            /contracts | {"companyId":1,"title":"Test Contract"}
            /invoices | {"contractId":1,"description":"Test Invoice"}
            /payment-methods | {"companyId":1,"type":"PIX","label":"Test Account"}
            """)
    void creationUpdateAndDeletionReturnDocumentedStatuses(String path, String body) throws Exception {
        String location = mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn().getResponse().getHeader("Location");
        URI item = URI.create(location);

        mvc.perform(get(item))
                .andExpect(status().isOk());

        mvc.perform(put(item).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());

        mvc.perform(delete(item))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        String createdByPut = mvc.perform(put(path + "/{id}", MISSING_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn().getResponse().getHeader("Location");
        mvc.perform(delete(URI.create(createdByPut)))
                .andExpect(status().isNoContent());
    }
}
