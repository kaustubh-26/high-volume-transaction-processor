package com.kaustubh.transactions.api.security;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaustubh.transactions.api.config.MerchantProperties;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class VerifySignatureFilter extends OncePerRequestFilter {

    public static final String HEADER_MERCHANT_ID = "X-Merchant-Id";
    public static final String HEADER_VERIFY = "X-Verify";
    public static final String HEADER_TIMESTAMP = "X-Timestamp";
    public static final String HEADER_NONCE = "X-Nonce";
    public static final String HEADER_IDEMPOTENCY = "Idempotency-Key";

    private final MerchantProperties merchantProperties;
    private final VerifySigner verifySigner;
    private final NonceReplayService nonceReplayService;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        if (!"POST".equalsIgnoreCase(request.getMethod())
                || !request.getRequestURI().startsWith("/api/v1/transactions")) {
            filterChain.doFilter(request, response);
            return;
        }

        String merchantId = request.getHeader(HEADER_MERCHANT_ID);
        String timestampHeader = request.getHeader(HEADER_TIMESTAMP);
        String nonce = request.getHeader(HEADER_NONCE);
        String verify = request.getHeader(HEADER_VERIFY);
        String idempotencyKey = request.getHeader(HEADER_IDEMPOTENCY);

        if (isBlank(merchantId) || isBlank(timestampHeader) || isBlank(nonce) || isBlank(verify) || isBlank(idempotencyKey)) {
            writeUnauthorized(response, "Missing required auth headers");
            return;
        }

        MerchantProperties.Merchant merchant = merchantProperties.getMerchants().get(merchantId);
        if (merchant == null || isBlank(merchant.getSaltKey()) || isBlank(merchant.getSaltIndex())) {
            writeUnauthorized(response, "Unknown merchant");
            return;
        }

        Instant requestTime;
        try {
            requestTime = Instant.parse(timestampHeader);
        } catch (Exception ex) {
            writeUnauthorized(response, "Invalid timestamp format");
            return;
        }

        Duration clockSkew = merchantProperties.getAllowedClockSkew();
        long skewSeconds = Math.abs(Instant.now().getEpochSecond() - requestTime.getEpochSecond());
        if (clockSkew != null && skewSeconds > clockSkew.toSeconds()) {
            writeUnauthorized(response, "Request timestamp outside allowed skew");
            return;
        }

        if (merchantProperties.isNonceEnforced()) {
            Duration nonceTtl = merchantProperties.getNonceTtl();
            Duration ttl = nonceTtl == null ? Duration.ofMinutes(2) : nonceTtl;
            if (!nonceReplayService.tryAccept(merchantId, nonce, ttl)) {
                writeUnauthorized(response, "Replay detected");
                return;
            }
        }

        CachedBodyHttpServletRequest wrappedRequest = new CachedBodyHttpServletRequest(request);
        String bodyBase64 = verifySigner.base64(wrappedRequest.getCachedBody());

        String canonicalPayload = bodyBase64
                + request.getRequestURI()
                + timestampHeader
                + nonce
                + idempotencyKey
                + merchant.getSaltKey();

        String expectedVerify = verifySigner.sha256HexUpper(canonicalPayload)
                + "###"
                + merchant.getSaltIndex();

        if (!verifySigner.constantTimeEquals(expectedVerify, verify)) {
            writeUnauthorized(response, "Invalid signature");
            return;
        }

        filterChain.doFilter(wrappedRequest, response);
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        objectMapper.writeValue(response.getOutputStream(), new ErrorResponse(message));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record ErrorResponse(String error) {
    }
}
