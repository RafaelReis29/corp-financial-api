package com.senac.corpfinancialapi;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "corp-financial-api.requests-per-minute=2")
@AutoConfigureMockMvc
class ApiRateLimitingTests {

    @Autowired
    private MockMvc mvc;

    @Test
    void demoIpAndSharedBudgetsAreIndependent() {
        var limiter = new ApiRateLimiter(120);

        var ipLimit = limiter.consume(ApiRateLimiter.Quota.DEMO_IP, "192.0.2.1");
        assertThat(ipLimit.capacity()).isEqualTo(30);
        assertThat(ipLimit.probe().getRemainingTokens()).isEqualTo(29);
        assertThat(limiter.consume(ApiRateLimiter.Quota.DEMO_IP, "192.0.2.2").probe().getRemainingTokens()).isEqualTo(29);

        var sharedLimit = limiter.consume(ApiRateLimiter.Quota.DEMO, "public-key");
        var nextSharedLimit = limiter.consume(ApiRateLimiter.Quota.DEMO, "public-key");
        assertThat(sharedLimit.capacity()).isEqualTo(300);
        assertThat(sharedLimit.probe().getRemainingTokens()).isEqualTo(299);
        assertThat(nextSharedLimit.probe().getRemainingTokens()).isEqualTo(298);
    }

    @Test
    void exhaustedClientReceivesRetryAfterAndAnotherIpKeepsItsBudget() throws Exception {
        mvc.perform(get("/companies"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(ApiHeaders.RATE_LIMIT, "2"))
                .andExpect(header().string(ApiHeaders.RATE_REMAINING, "1"));
        mvc.perform(get("/companies"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(ApiHeaders.RATE_REMAINING, "0"));
        mvc.perform(get("/companies").header("X-Forwarded-For", "192.0.2.2"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
                .andExpect(jsonPath("$.status").value(429));
        mvc.perform(get("/companies").with(request -> {
                    request.setRemoteAddr("192.0.2.2");
                    return request;
                }))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(ApiHeaders.RATE_REMAINING, "1"));
    }
}
