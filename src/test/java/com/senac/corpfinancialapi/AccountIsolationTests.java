package com.senac.corpfinancialapi;

import java.net.URI;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import static com.senac.corpfinancialapi.HttpResponsesTests.ADMIN_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "corp-financial-api.admin-key=" + ADMIN_KEY,
        "corp-financial-api.requests-per-minute=1000"
})
@AutoConfigureMockMvc
class AccountIsolationTests {

    @Autowired
    private MockMvc mvc;

    @Test
    void planMustBeSpecifiedByName() throws Exception {
        String account = createAccount("Named key plans");
        for (String type : new String[]{"0", "1", "\"1\""}) {
            mvc.perform(post("/api-keys").header(ApiHeaders.API_KEY, ADMIN_KEY)
                            .header(ApiHeaders.IDEMPOTENCY_KEY, UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"label":"Integração","accountId":"%s","type":%s}
                                    """.formatted(account, type)))
                    .andExpect(status().isBadRequest());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"1.0.0", "1.0.1", "2.0.0"})
    void bothPlansCanWriteTheirAccountRecordsInEveryVersion(String version) throws Exception {
        String account = createAccount("CRUD " + version);
        String basic = issue(account, "BASIC");
        String premium = issue(account, "PREMIUM");
        String doc = "CRUD-" + version;
        String companyBody = """
                {"tradeName":"Company from PUT","docNumber":"%s"}
                """.formatted(doc);
        String company = upsertMissing("/companies", basic, version, companyBody);
        long companyId = pathId(company);
        String contactBody = """
                {"companyId":%d,"name":"Contact from PUT"}
                """.formatted(companyId);
        String contractBody = """
                {"companyId":%d,"title":"Contract from PUT","totalValue":100,"currency":"BRL",
                 "startDate":"2026-01-01","status":"ATIVO"}
                """.formatted(companyId);
        String paymentBody = """
                {"companyId":%d,"type":"PIX","label":"Payment method from PUT"}
                """.formatted(companyId);
        String contact = upsertMissing("/contacts", premium, version, contactBody);
        String contract = upsertMissing("/contracts", basic, version, contractBody);
        String payment = upsertMissing("/payment-methods", premium, version, paymentBody);
        String invoiceBody = """
                {"contractId":%d,"paymentMethodId":%d,"amount":100,"dueDate":"2026-12-01","status":"PENDENTE"}
                """.formatted(pathId(contract), pathId(payment));
        String invoice = upsertMissing("/invoices", basic, version, invoiceBody);

        mvc.perform(put(invoice).header(ApiHeaders.API_KEY, premium).header(ApiHeaders.VERSION, version)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invoiceBody.replace("\"status\":\"PENDENTE\"", "\"status\":\"PAGO\",\"paidAt\":\"2026-12-01T12:00:00\"")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAGO"));

        for (String path : new String[]{invoice, payment, contract, contact, company}) {
            mvc.perform(get(path).header(ApiHeaders.API_KEY, basic).header(ApiHeaders.VERSION, version))
                    .andExpect(status().isOk());
            mvc.perform(get(path).header(ApiHeaders.API_KEY, premium).header(ApiHeaders.VERSION, version))
                    .andExpect(status().isOk());
            mvc.perform(get(path).header(ApiHeaders.API_KEY, ADMIN_KEY).header(ApiHeaders.VERSION, version)
                            .header(ApiHeaders.ACCOUNT_ID, account))
                    .andExpect(status().isOk());
            mvc.perform(get(path).header(ApiHeaders.API_KEY, ApiKeys.PUBLIC_DEMO_KEY)
                            .header(ApiHeaders.ACCOUNT_ID, account).header(ApiHeaders.VERSION, version))
                    .andExpect(status().isNotFound());
            if (!path.equals(company)) {
                mvc.perform(delete(path).header(ApiHeaders.API_KEY, premium).header(ApiHeaders.VERSION, version))
                        .andExpect(status().isNoContent());
            }
        }
        mvc.perform(delete(company).header(ApiHeaders.API_KEY, ADMIN_KEY).header(ApiHeaders.VERSION, version)
                        .header(ApiHeaders.ACCOUNT_ID, account))
                .andExpect(status().isNoContent());
    }

    private String upsertMissing(String collection, String key, String version, String body) throws Exception {
        String missing = collection + "/" + Long.MAX_VALUE;
        var created = mvc.perform(put(missing).header(ApiHeaders.API_KEY, key).header(ApiHeaders.VERSION, version)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        String location = location(created);
        assertThat(location).startsWith(collection + "/").isNotEqualTo(missing);
        return location;
    }

    @Test
    void paidKeysAccessIndependentAccountRecordsAndCompanyDeletionRequiresAdmin() throws Exception {
        String basic = issue(createAccount("Basic account"), "BASIC");
        String premiumAccount = createAccount("Premium account");
        String premium = issue(premiumAccount, "PREMIUM");
        assertThat(basic).startsWith("cf_basic_");
        assertThat(premium).startsWith("cf_premium_");
        String company = """
                {"tradeName":"First copy","legalName":"Example Company","docNumber":"SHARED-DOC"}
                """;
        MvcResult first = create("/companies", basic, company)
                .andExpect(header().string(ApiHeaders.RATE_LIMIT, "60"))
                .andExpect(header().string(ApiHeaders.RATE_REMAINING, "59"))
                .andReturn();
        String firstCompany = location(first);
        create("/companies", premium, company.replace("First copy", "Second copy"))
                .andExpect(header().string(ApiHeaders.RATE_LIMIT, "300"))
                .andExpect(header().string(ApiHeaders.RATE_REMAINING, "299"));
        mvc.perform(get("/companies").header(ApiHeaders.API_KEY, premium))
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$._embedded.companyList[0].tradeName").value("Second copy"));
        mvc.perform(get(firstCompany).header(ApiHeaders.API_KEY, premium)
                        .header(ApiHeaders.ACCOUNT_ID, "ignored-for-client"))
                .andExpect(status().isNotFound());
        mvc.perform(put(firstCompany).header(ApiHeaders.API_KEY, premium).contentType(MediaType.APPLICATION_JSON)
                        .content(company.replace("SHARED-DOC", "NEW-DOC")))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION));
        mvc.perform(get(firstCompany).header(ApiHeaders.API_KEY, basic))
                .andExpect(jsonPath("$.company.tradeName").value("First copy"));

        String companyToDelete = location(create("/companies", premium,
                company.replace("SHARED-DOC", "DELETE-DOC")).andReturn());
        mvc.perform(delete(companyToDelete).header(ApiHeaders.API_KEY, premium))
                .andExpect(status().isForbidden());
        mvc.perform(delete(companyToDelete).header(ApiHeaders.API_KEY, ADMIN_KEY))
                .andExpect(status().isBadRequest());
        mvc.perform(delete(companyToDelete).header(ApiHeaders.API_KEY, ADMIN_KEY)
                        .header(ApiHeaders.ACCOUNT_ID, premiumAccount))
                .andExpect(status().isNoContent());
    }

    @Test
    void referencesAndDependentDeletesAreScopedToTheKeyAccount() throws Exception {
        String owner = issue(createAccount("Owner"), "BASIC");
        String other = issue(createAccount("Other"), "PREMIUM");
        long company = resourceId(create("/companies", owner,
                "{\"tradeName\":\"Owner Company\",\"legalName\":\"Owner Company Ltda\",\"docNumber\":\"OWNER-DOC\"}").andReturn());
        long otherCompany = resourceId(create("/companies", other,
                "{\"tradeName\":\"Other Company\",\"legalName\":\"Other Company Ltda\",\"docNumber\":\"OTHER-DOC\"}").andReturn());
        String contact = "{\"companyId\":%d,\"name\":\"Contact Owner\"}".formatted(company);
        String contactPath = location(create("/contacts", owner, contact).andReturn());
        String contract = location(create("/contracts", owner, """
                {"companyId":%d,"title":"Contrato","totalValue":100,"currency":"BRL","startDate":"2026-01-01","status":"ATIVO"}
                """.formatted(company)).andReturn());
        String payment = location(create("/payment-methods", owner, """
                {"companyId":%d,"type":"PIX","label":"Conta principal"}
                """.formatted(company)).andReturn());
        long otherPayment = resourceId(create("/payment-methods", other, """
                {"companyId":%d,"type":"PIX","label":"Conta externa"}
                """.formatted(otherCompany)).andReturn());
        String invoice = """
                {"contractId":%d,"amount":100,"dueDate":"2026-12-01","status":"PENDENTE","paymentMethodId":%d}
                """;
        String invoicePath = location(create("/invoices", owner, invoice.formatted(pathId(contract), pathId(payment))).andReturn());

        for (String path : new String[]{contactPath, contract, payment, invoicePath}) {
            mvc.perform(get(path).header(ApiHeaders.API_KEY, other)).andExpect(status().isNotFound());
            mvc.perform(delete(path).header(ApiHeaders.API_KEY, other)).andExpect(status().isNotFound());
        }
        mvc.perform(post("/contacts").header(ApiHeaders.API_KEY, other).header(ApiHeaders.IDEMPOTENCY_KEY, UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON).content(contact))
                .andExpect(status().isConflict());
        mvc.perform(put(contactPath).header(ApiHeaders.API_KEY, owner).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyId\":%d,\"name\":\"Moved Contact\"}".formatted(otherCompany)))
                .andExpect(status().isConflict());
        mvc.perform(put(invoicePath).header(ApiHeaders.API_KEY, owner).contentType(MediaType.APPLICATION_JSON)
                        .content(invoice.formatted(pathId(contract), otherPayment)))
                .andExpect(status().isConflict());
        mvc.perform(post("/invoices").header(ApiHeaders.API_KEY, other).header(ApiHeaders.IDEMPOTENCY_KEY, UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
                        .content(invoice.formatted(pathId(contract), otherPayment)))
                .andExpect(status().isConflict());
        mvc.perform(delete(contract).header(ApiHeaders.API_KEY, owner)).andExpect(status().isConflict());
        for (String path : new String[]{invoicePath, payment, contract, contactPath}) {
            mvc.perform(delete(path).header(ApiHeaders.API_KEY, owner)).andExpect(status().isNoContent());
        }
    }

    @Test
    void demoOnlyReadsSeededRecordsAndCannotBeSelectedByPaidCredentials() throws Exception {
        String account = createAccount("Demo isolation");
        String customer = issue(account, "BASIC");
        String company = location(create("/companies", customer,
                "{\"tradeName\":\"Private Company\",\"legalName\":\"Private Company Ltda\",\"docNumber\":\"PRIVATE-DOC\"}").andReturn());
        String demo = ApiKeys.PUBLIC_DEMO_KEY;
        mvc.perform(get("/companies/1").header(ApiHeaders.API_KEY, demo))
                .andExpect(status().isOk())
                .andExpect(header().string(ApiHeaders.RATE_LIMIT, "30"));
        mvc.perform(get(company).header(ApiHeaders.API_KEY, demo).header(ApiHeaders.ACCOUNT_ID, account))
                .andExpect(status().isNotFound());
        mvc.perform(get("/companies/1").header(ApiHeaders.API_KEY, customer))
                .andExpect(status().isNotFound());
        mvc.perform(post("/companies").header(ApiHeaders.API_KEY, demo)).andExpect(status().isForbidden());
        mvc.perform(delete("/contacts/1").header(ApiHeaders.API_KEY, demo)).andExpect(status().isForbidden());
    }

    private String createAccount(String name) throws Exception {
        MvcResult result = create("/accounts", ADMIN_KEY, "{\"name\":\"%s\"}".formatted(name)).andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private String issue(String accountId, String type) throws Exception {
        MvcResult result = create("/api-keys", ADMIN_KEY, """
                {"label":"Integração","accountId":"%s","type":"%s"}
                """.formatted(accountId, type))
                .andExpect(jsonPath("$.metadata.type").value(type))
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.apiKey");
    }

    private ResultActions create(String path, String key, String body) throws Exception {
        return mvc.perform(post(path).header(ApiHeaders.API_KEY, key).header(ApiHeaders.IDEMPOTENCY_KEY, UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    private static String location(MvcResult result) {
        return URI.create(result.getResponse().getHeader(HttpHeaders.LOCATION)).getPath();
    }

    private static long resourceId(MvcResult result) {
        return pathId(location(result));
    }

    private static long pathId(String path) {
        return Long.parseLong(path.substring(path.lastIndexOf('/') + 1));
    }
}
