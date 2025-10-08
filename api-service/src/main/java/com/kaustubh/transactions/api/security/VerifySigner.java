package com.kaustubh.transactions.api.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

import org.springframework.stereotype.Component;

@Component
public class VerifySigner {

    public String base64(byte[] body) {
        if (body == null || body.length == 0) {
            return "";
        }
        return Base64.getEncoder().encodeToString(body);
    }

    public String sha256HexUpper(String payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).toUpperCase(Locale.ROOT);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to compute SHA-256 digest", ex);
        }
    }

    public boolean constantTimeEquals(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        return MessageDigest.isEqual(
                left.getBytes(StandardCharsets.UTF_8),
                right.getBytes(StandardCharsets.UTF_8)
        );
    }
}
