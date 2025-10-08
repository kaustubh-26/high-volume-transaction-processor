package com.kaustubh.transactions.api.security;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class InMemoryNonceReplayServiceTest {

    @Test
    void tryAccept_rejectsDuplicatesWithinTtl() {
        InMemoryNonceReplayService service = new InMemoryNonceReplayService();
        Duration ttl = Duration.ofSeconds(30);

        assertThat(service.tryAccept("merchant-1", "nonce-1", ttl)).isTrue();
        assertThat(service.tryAccept("merchant-1", "nonce-1", ttl)).isFalse();
    }

    @Test
    void tryAccept_allowsExpiredNonceAfterCleanup() {
        InMemoryNonceReplayService service = new InMemoryNonceReplayService();
        Duration expired = Duration.ofSeconds(-1);

        assertThat(service.tryAccept("merchant-1", "stale", expired)).isTrue();

        for (int i = 0; i < 999; i++) {
            service.tryAccept("merchant-1", "nonce-" + i, expired);
        }

        assertThat(service.tryAccept("merchant-1", "stale", Duration.ofSeconds(30))).isTrue();
    }
}
