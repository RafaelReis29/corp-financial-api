package com.senac.corpfinancialapi;

enum ApiKeyType {
    BASIC("cf_basic_", ApiRateLimiter.Quota.BASIC),
    PREMIUM("cf_premium_", ApiRateLimiter.Quota.PREMIUM),
    DEMO("cf_demo_", ApiRateLimiter.Quota.DEMO);

    private final String prefix;
    private final ApiRateLimiter.Quota quota;

    ApiKeyType(String prefix, ApiRateLimiter.Quota quota) {
        this.prefix = prefix;
        this.quota = quota;
    }

    String prefix() {
        return prefix;
    }

    ApiRateLimiter.Quota quota() {
        return quota;
    }
}
