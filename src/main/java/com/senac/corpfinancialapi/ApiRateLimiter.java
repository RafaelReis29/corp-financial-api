package com.senac.corpfinancialapi;

import java.time.Duration;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
class ApiRateLimiter {

    private static final Duration WINDOW = Duration.ofMinutes(1);
    private static final long WINDOW_NANOS = WINDOW.toNanos();
    private static final int MAX_CLIENTS_PER_QUOTA = 10_000;

    enum Quota {
        ANONYMOUS, ADMIN, BASIC, PREMIUM, DEMO, DEMO_IP
    }

    private final Map<Quota, QuotaBuckets> quotas = new EnumMap<>(Quota.class);
    private long lastCleanup = System.nanoTime();

    ApiRateLimiter(@Value("${corp-financial-api.requests-per-minute}") long defaultCapacity) {
        quotas.put(Quota.ANONYMOUS, new QuotaBuckets(defaultCapacity));
        quotas.put(Quota.ADMIN, new QuotaBuckets(defaultCapacity));
        quotas.put(Quota.BASIC, new QuotaBuckets(60));
        quotas.put(Quota.PREMIUM, new QuotaBuckets(300));
        quotas.put(Quota.DEMO, new QuotaBuckets(300));
        quotas.put(Quota.DEMO_IP, new QuotaBuckets(30));
    }

    synchronized LimitResult consume(Quota quota, String clientId) {
        long now = System.nanoTime();
        if (now - lastCleanup >= WINDOW_NANOS) {
            quotas.values().forEach(group -> group.clients.values()
                    .removeIf(entry -> now - entry.lastRequest >= WINDOW_NANOS));
            lastCleanup = now;
        }

        QuotaBuckets group = quotas.get(quota);
        ClientBucket entry = group.clients.get(clientId);
        if (entry == null && group.clients.size() >= MAX_CLIENTS_PER_QUOTA) {
            return new LimitResult(group.capacity, group.overflow.tryConsumeAndReturnRemaining(1));
        }
        if (entry == null) {
            entry = new ClientBucket(newBucket(group.capacity));
            group.clients.put(clientId, entry);
        }
        entry.lastRequest = now;
        return new LimitResult(group.capacity, entry.bucket.tryConsumeAndReturnRemaining(1));
    }

    private static Bucket newBucket(long capacity) {
        return Bucket.builder().addLimit(limit -> limit.capacity(capacity).refillGreedy(capacity, WINDOW)).build();
    }

    record LimitResult(long capacity, ConsumptionProbe probe) {
    }

    private static class QuotaBuckets {
        private final long capacity;
        private final Bucket overflow;
        private final Map<String, ClientBucket> clients = new HashMap<>();

        QuotaBuckets(long capacity) {
            this.capacity = capacity;
            this.overflow = newBucket(capacity);
        }
    }

    private static class ClientBucket {
        private final Bucket bucket;
        private long lastRequest;

        ClientBucket(Bucket bucket) {
            this.bucket = bucket;
        }
    }
}
