package com.senac.corpfinancialapi;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.accept.ApiVersionHolder;
import org.springframework.web.accept.SemanticApiVersionParser;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerMapping;
import tools.jackson.databind.ObjectMapper;

@Service
class IdempotentPosts {

    private static final Pattern KEY_FORMAT = Pattern.compile("[A-Za-z0-9._~-]{1,128}");
    private static final long RETENTION_NANOS = Duration.ofHours(24).toNanos();
    private static final int MAX_ENTRIES = 10_000;

    private final Map<Scope, Entry> entries = new HashMap<>();
    private final HttpServletRequest request;
    private final ObjectMapper mapper;
    private final TransactionTemplate transaction;
    private long lastCleanup = System.nanoTime();

    IdempotentPosts(HttpServletRequest request, ObjectMapper mapper, PlatformTransactionManager transactionManager) {
        this.request = request;
        this.mapper = mapper;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    <T> ResponseEntity<T> execute(Object payload, Supplier<ResponseEntity<T>> operation) {
        var version = (ApiVersionHolder) request.getAttribute(HandlerMapping.API_VERSION_ATTRIBUTE);
        if (version.getVersion() instanceof SemanticApiVersionParser.Version semanticVersion
                && semanticVersion.getMajor() == 1) {
            return operation.get();
        }

        var headers = request.getHeaders(ApiHeaders.IDEMPOTENCY_KEY);
        String key = headers.hasMoreElements() ? headers.nextElement() : "";
        if (headers.hasMoreElements() || !KEY_FORMAT.matcher(key).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "POST na versão 2.0.0 exige um X-Idempotency-Key com 1 a 128 caracteres seguros para URL.");
        }

        Account account = (Account) request.getAttribute(AccountScope.REQUEST_ACCOUNT);
        Scope scope = new Scope((String) request.getAttribute(ApiRequestPolicy.CLIENT_ID),
                account == null ? null : account.id(),
                String.valueOf(request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE)), key);
        byte[] fingerprint = ApiKeys.hash(mapper.writeValueAsBytes(payload));
        Entry entry;
        synchronized (entries) {
            long now = System.nanoTime();
            entry = entries.get(scope);
            if (entry != null && !entry.expired(now)) {
                return replay(entry, fingerprint);
            }
            if (entries.size() >= MAX_ENTRIES || now - lastCleanup >= Duration.ofMinutes(1).toNanos()) {
                entries.values().removeIf(current -> current.expired(now));
                lastCleanup = now;
            }
            if (!entries.containsKey(scope) && entries.size() >= MAX_ENTRIES) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Idempotency storage is temporarily full.");
            }
            entry = new Entry(fingerprint);
            entries.put(scope, entry);
        }

        try {
            ResponseEntity<T> response = transaction.execute(status -> operation.get());
            synchronized (entries) {
                entry.response = response;
                entry.completedAt = System.nanoTime();
            }
            return response;
        } catch (RuntimeException | Error exception) {
            synchronized (entries) {
                entries.remove(scope, entry);
            }
            throw exception;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> ResponseEntity<T> replay(Entry entry, byte[] fingerprint) {
        if (!Arrays.equals(entry.fingerprint, fingerprint)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This X-Idempotency-Key was already used with different data.");
        }
        if (entry.response == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The request with this X-Idempotency-Key is still in progress.");
        }
        return ResponseEntity.status(entry.response.getStatusCode()).headers(entry.response.getHeaders())
                .header(ApiHeaders.IDEMPOTENCY_REPLAYED, "true").body((T) entry.response.getBody());
    }

    private record Scope(String clientId, UUID accountId, String operation, String key) {
    }

    private static class Entry {

        private final byte[] fingerprint;
        private ResponseEntity<?> response;
        private long completedAt;

        Entry(byte[] fingerprint) {
            this.fingerprint = fingerprint;
        }

        boolean expired(long now) {
            return response != null && now - completedAt >= RETENTION_NANOS;
        }
    }
}
