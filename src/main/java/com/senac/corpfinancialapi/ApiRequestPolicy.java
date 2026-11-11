package com.senac.corpfinancialapi;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.senac.corpfinancialapi.ApiRateLimiter.LimitResult;
import com.senac.corpfinancialapi.ApiRateLimiter.Quota;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

@Component
class ApiRequestPolicy implements HandlerInterceptor {

    static final List<String> COLLECTIONS = List.of(
            "/companies", "/contacts", "/contracts", "/invoices", "/payment-methods", "/api-keys", "/accounts");
    static final String CLIENT_ID = ApiRequestPolicy.class.getName() + ".clientId";

    private final ApiKeys keys;
    private final ApiRateLimiter rateLimiter;
    private final Validator validator;
    private final AccountRepository accounts;

    ApiRequestPolicy(ApiKeys keys, ApiRateLimiter rateLimiter, Validator validator, AccountRepository accounts) {
        this.keys = keys;
        this.rateLimiter = rateLimiter;
        this.validator = validator;
        this.accounts = accounts;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (CorsUtils.isPreFlightRequest(request)) {
            return true;
        }
        String path = String.valueOf(request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE));
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.addHeader(HttpHeaders.VARY, ApiHeaders.VERSION);
        response.addHeader(HttpHeaders.VARY, ApiHeaders.ACCOUNT_ID);

        var headers = request.getHeaders(ApiHeaders.API_KEY);
        String key = headers.hasMoreElements() ? headers.nextElement() : null;
        ApiKeys.Identity identity = headers.hasMoreElements() ? null : keys.authenticate(key);
        String ip = request.getRemoteAddr();
        if (identity == null) {
            applyLimit(rateLimiter.consume(Quota.ANONYMOUS, ip), response);
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "ApiKey realm=\"corp-financial-api\"");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Provide a valid X-API-Key.");
        }
        request.setAttribute(CLIENT_ID, identity.id());
        if (identity.admin()) {
            applyLimit(rateLimiter.consume(Quota.ADMIN, identity.id()), response);
        } else if (identity.type() == ApiKeyType.DEMO) {
            applyLimit(rateLimiter.consume(Quota.DEMO_IP, ip), response);
            LimitResult shared = rateLimiter.consume(identity.type().quota(), identity.id());
            if (!shared.probe().isConsumed()) {
                applyLimit(shared, response);
            }
        } else {
            applyLimit(rateLimiter.consume(identity.type().quota(), identity.id()), response);
        }

        String method = request.getMethod();
        if (requiresAdmin(path, method) && !identity.admin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esta operação exige a chave de administração.");
        }
        if (identity.type() == ApiKeyType.DEMO && !"GET".equals(method)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "A chave de demonstração permite somente consultas.");
        }
        if (!isManagement(path)) {
            Account account = identity.admin() ? selectAdminAccount(request, path, method) : identity.account();
            request.setAttribute(AccountScope.REQUEST_ACCOUNT, account);
            if (isRead(method) && COLLECTIONS.contains(path)) {
                validatePagination(request);
            }
        }
        return true;
    }

    private static void applyLimit(LimitResult result, HttpServletResponse response) {
        var probe = result.probe();
        response.setHeader(ApiHeaders.RATE_LIMIT, Long.toString(result.capacity()));
        response.setHeader(ApiHeaders.RATE_REMAINING, Long.toString(probe.getRemainingTokens()));
        if (!probe.isConsumed()) {
            long seconds = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill() - 1) + 1);
            response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(seconds));
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded. Retry after the indicated interval.");
        }
    }

    private Account selectAdminAccount(HttpServletRequest request, String path, String method) {
        var values = request.getHeaders(ApiHeaders.ACCOUNT_ID);
        if (!values.hasMoreElements() && !requiresAdmin(path, method)) {
            return accounts.findByDemoTrue().orElseThrow();
        }
        String value = values.hasMoreElements() ? values.nextElement() : "";
        UUID id;
        try {
            id = UUID.fromString(value);
            if (values.hasMoreElements() || !id.toString().equalsIgnoreCase(value)) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe um UUID no cabeçalho X-Account-Id.");
        }
        return accounts.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta não encontrada."));
    }

    private void validatePagination(HttpServletRequest request) {
        var violations = validator.validate(new Pagination(integerParameter(request, "page"), integerParameter(request, "size")));
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    private static Integer integerParameter(HttpServletRequest request, String name) {
        String[] values = request.getParameterValues(name);
        if (values == null) {
            return null;
        }
        if (values.length == 1) {
            try {
                return Integer.valueOf(values[0]);
            } catch (NumberFormatException exception) {
                throw invalidPagination(name);
            }
        }
        throw invalidPagination(name);
    }

    private static ResponseStatusException invalidPagination(String name) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide one integer value for " + name + ".");
    }

    static boolean isManagement(String path) {
        return path.equals("/api-keys") || path.startsWith("/api-keys/")
                || path.equals("/accounts") || path.startsWith("/accounts/");
    }

    static boolean requiresAdmin(String path, String method) {
        return isManagement(path) || (path.equals("/companies/{id}") && "DELETE".equals(method));
    }

    private static boolean isRead(String method) {
        return "GET".equals(method) || "HEAD".equals(method);
    }

    private record Pagination(@PositiveOrZero Integer page, @Positive Integer size) {
    }
}
