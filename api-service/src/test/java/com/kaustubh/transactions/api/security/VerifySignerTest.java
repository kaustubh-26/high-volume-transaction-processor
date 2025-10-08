package com.kaustubh.transactions.api.security;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class VerifySignerTest {

    private final VerifySigner signer = new VerifySigner();

    @Test
    void base64_returnsEmptyForNullOrEmpty() {
        assertThat(signer.base64(null)).isEmpty();
        assertThat(signer.base64(new byte[0])).isEmpty();
    }

    @Test
    void base64_encodesBody() {
        byte[] body = "hello".getBytes(StandardCharsets.UTF_8);
        assertThat(signer.base64(body)).isEqualTo("aGVsbG8=");
    }

    @Test
    void sha256HexUpper_returnsExpectedDigest() {
        String digest = signer.sha256HexUpper("hello");
        assertThat(digest).isEqualTo("2CF24DBA5FB0A30E26E83B2AC5B9E29E1B161E5C1FA7425E73043362938B9824");
    }

    @Test
    void constantTimeEquals_handlesNullsAndMatches() {
        assertThat(signer.constantTimeEquals(null, "value")).isFalse();
        assertThat(signer.constantTimeEquals("value", null)).isFalse();
        assertThat(signer.constantTimeEquals("value", "value")).isTrue();
        assertThat(signer.constantTimeEquals("value", "other")).isFalse();
    }
}
