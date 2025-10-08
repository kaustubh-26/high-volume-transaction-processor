package com.kaustubh.transactions.api.security;

import java.time.Duration;

public interface NonceReplayService {
    boolean tryAccept(String merchantId, String nonce, Duration ttl);
}
