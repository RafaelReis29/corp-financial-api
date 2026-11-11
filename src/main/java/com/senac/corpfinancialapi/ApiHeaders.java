package com.senac.corpfinancialapi;

final class ApiHeaders {

    static final String API_KEY = "X-API-Key";
    static final String ACCOUNT_ID = "X-Account-Id";
    static final String VERSION = "X-API-Version";
    static final String IDEMPOTENCY_KEY = "X-Idempotency-Key";
    static final String IDEMPOTENCY_REPLAYED = "Idempotency-Replayed";
    static final String RATE_LIMIT = "X-RateLimit-Limit";
    static final String RATE_REMAINING = "X-RateLimit-Remaining";

    private ApiHeaders() {
    }
}
