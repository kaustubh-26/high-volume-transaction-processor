package com.kaustubh.transactions.api.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

@Component
public class InMemoryNonceReplayService implements NonceReplayService {

    private static final long CLEANUP_EVERY = 1000;

    private final Map<String, Instant> usedNonces = new ConcurrentHashMap<>();
    private final AtomicLong requestCounter = new AtomicLong();

    @Override
    public boolean tryAccept(String merchantId, String nonce, Duration ttl) {
        Instant now = Instant.now();

        if (requestCounter.incrementAndGet() % CLEANUP_EVERY == 0) {
            cleanupExpired(now);
        }

        String key = merchantId + ":" + nonce;
        Instant expiresAt = now.plus(ttl);
        return usedNonces.putIfAbsent(key, expiresAt) == null;
    }

    private void cleanupExpired(Instant now) {
        usedNonces.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
    }
}
