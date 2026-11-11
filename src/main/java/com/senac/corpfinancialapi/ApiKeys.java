package com.senac.corpfinancialapi;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
class ApiKeys {

    private static final Pattern CLIENT_KEY = Pattern.compile("cf_(?:basic|premium|demo)_[A-Za-z0-9_-]{43}");
    static final String PUBLIC_DEMO_KEY = "cf_demo_0123456789abcdefghijklmnopqrstuvwxyzABCDEFG";
    private final SecureRandom random = new SecureRandom();
    private final ApiCredentialRepository repository;
    private final byte[] adminHash;

    ApiKeys(ApiCredentialRepository repository, @Value("${corp-financial-api.admin-key}") String adminKey) {
        if (PUBLIC_DEMO_KEY.equals(adminKey)) {
            throw new IllegalArgumentException("CORP_API_ADMIN_KEY must differ from the public demo key");
        }
        if (!adminKey.isEmpty() && (adminKey.length() < 32 || adminKey.length() > 256 || !adminKey.matches("[!-~]+"))) {
            throw new IllegalArgumentException("CORP_API_ADMIN_KEY must contain 32 to 256 visible ASCII characters");
        }
        this.repository = repository;
        this.adminHash = adminKey.isEmpty() ? null : hash(adminKey.getBytes(StandardCharsets.US_ASCII));
    }

    Identity authenticate(String key) {
        if (key == null || key.length() > 256 || !key.matches("[!-~]+")) {
            return null;
        }
        byte[] hash = hash(key.getBytes(StandardCharsets.US_ASCII));
        if (adminHash != null && MessageDigest.isEqual(adminHash, hash)) {
            return new Identity("admin", null, null);
        }
        if (!CLIENT_KEY.matcher(key).matches()) {
            return null;
        }
        return repository.findBySecretHash(HexFormat.of().formatHex(hash))
                .filter(credential -> key.startsWith(credential.type().prefix()))
                .map(credential -> new Identity(credential.id().toString(), credential.account(), credential.type()))
                .orElse(null);
    }

    IssuedKey issue(String label, Account account, ApiKeyType type) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String secret = type.prefix() + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return new IssuedKey(store(label, secret, account, type), secret);
    }

    void registerDemoKey(Account account) {
        store("Demonstração pública", PUBLIC_DEMO_KEY, account, ApiKeyType.DEMO);
    }

    private ApiCredential store(String label, String secret, Account account, ApiKeyType type) {
        String secretHash = HexFormat.of().formatHex(hash(secret.getBytes(StandardCharsets.US_ASCII)));
        return repository.save(new ApiCredential(UUID.randomUUID(), label, secretHash, Instant.now(), account, type));
    }

    static byte[] hash(byte[] value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    record Identity(String id, Account account, ApiKeyType type) {
        boolean admin() {
            return type == null;
        }
    }

    record IssuedKey(ApiCredential credential, String secret) {
        @Override
        public String toString() {
            return "IssuedKey[" + credential.id() + "]";
        }
    }
}
