package com.kaustubh.transactions.api.security;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.kaustubh.transactions.api.config.ReadAuthProperties;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReadAuthConfigService {

    private static final String FIELD_API_KEYS = "api_keys";
    private static final String FIELD_RATE_LIMITS = "rate_limits";
    private static final String FIELD_RATE_WINDOW_SECONDS = "rate_window_seconds";
    private static final String FIELD_DEFAULT_LIMIT = "default_limit_per_window";
    private static final String FIELD_API_KEY_MERCHANTS = "api_key_merchants";

    private final ReadAuthProperties readAuthProperties;
    private final StringRedisTemplate redisTemplate;

    public ReadAuthConfig currentConfig() {
        String redisKey = readAuthProperties.getRedisConfigKey();
        HashOperations<String, Object, Object> ops = redisTemplate.opsForHash();
        Map<Object, Object> values;
        try {
            values = ops.entries(redisKey);
        } catch (Exception ex) {
            values = Collections.emptyMap();
        }

        String apiKeys = stringOrDefault(values.get(FIELD_API_KEYS), readAuthProperties.getApiKeys());
        String rateLimits = stringOrDefault(values.get(FIELD_RATE_LIMITS), readAuthProperties.getRateLimits());
        String apiKeyMerchants = stringOrDefault(values.get(FIELD_API_KEY_MERCHANTS), readAuthProperties.getApiKeyMerchants());

        int windowSeconds = intOrDefault(values.get(FIELD_RATE_WINDOW_SECONDS), readAuthProperties.getRateWindowSeconds());
        int defaultLimit = intOrDefault(values.get(FIELD_DEFAULT_LIMIT), readAuthProperties.getDefaultLimitPerWindow());

        return new ReadAuthConfig(
                parseKeys(apiKeys),
                parseLimits(rateLimits),
                parseMerchants(apiKeyMerchants),
                Math.max(1, windowSeconds),
                Math.max(1, defaultLimit)
        );
    }

    private String stringOrDefault(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String text = String.valueOf(value).trim();
        return text.isBlank() ? fallback : text;
    }

    private int intOrDefault(Object value, int fallback) {
        if (value == null) {
            return fallback;
        }
        String text = String.valueOf(value).trim();
        if (text.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private Set<String> parseKeys(String raw) {
        return parseDelimitedSet(raw, ",");
    }

    private Map<String, Integer> parseLimits(String raw) {
        Map<String, Integer> limits = new HashMap<>();
        forEachKeyValueEntry(raw, (key, value) -> {
            Integer parsed = parseInteger(value);
            if (parsed != null) {
                limits.put(key, parsed);
            }
        });
        return limits;
    }

    private Map<String, Set<String>> parseMerchants(String raw) {
        Map<String, Set<String>> merchants = new HashMap<>();
        forEachKeyValueEntry(raw, (key, value) -> {
            Set<String> ids = parseDelimitedSet(value, "\\|");
            if (!ids.isEmpty()) {
                merchants.put(key, ids);
            }
        });
        return merchants;
    }

    private void forEachKeyValueEntry(String raw, BiConsumer<String, String> consumer) {
        if (raw == null || raw.isBlank()) {
            return;
        }

        for (String entry : raw.split(",")) {
            KeyValueEntry parsedEntry = parseKeyValueEntry(entry);
            if (parsedEntry != null) {
                consumer.accept(parsedEntry.key(), parsedEntry.value());
            }
        }
    }

    private KeyValueEntry parseKeyValueEntry(String entry) {
        String trimmed = entry.trim();
        if (trimmed.isBlank()) {
            return null;
        }

        String[] parts = trimmed.split(":", 2);
        if (parts.length != 2) {
            return null;
        }

        String key = parts[0].trim();
        String value = parts[1].trim();
        if (key.isBlank() || value.isBlank()) {
            return null;
        }

        return new KeyValueEntry(key, value);
    }

    private Set<String> parseDelimitedSet(String raw, String delimiter) {
        if (raw == null || raw.isBlank()) {
            return Collections.emptySet();
        }

        return Stream.of(raw.split(delimiter))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toCollection(HashSet::new));
    }

    private Integer parseInteger(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private record KeyValueEntry(String key, String value) {
    }

    public record ReadAuthConfig(
            Set<String> allowedKeys,
            Map<String, Integer> perKeyLimits,
            Map<String, Set<String>> keyMerchants,
            int windowSeconds,
            int defaultLimit
    ) {
        public int resolveLimit(String apiKey) {
            return perKeyLimits.getOrDefault(apiKey, defaultLimit);
        }

        public boolean isAllowedKey(String apiKey) {
            return allowedKeys.contains(apiKey);
        }

        public boolean isMerchantAllowed(String apiKey, String merchantId) {
            Set<String> merchants = keyMerchants.get(apiKey);
            return merchants != null && merchants.contains(merchantId);
        }
    }
}
