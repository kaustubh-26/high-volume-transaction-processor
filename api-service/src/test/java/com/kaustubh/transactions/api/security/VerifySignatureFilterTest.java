package com.kaustubh.transactions.api.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaustubh.transactions.api.config.MerchantProperties;

import jakarta.servlet.FilterChain;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class VerifySignatureFilterTest {

    @Mock
    private MerchantProperties merchantProperties;

    @Mock
    private VerifySigner verifySigner;

    @Mock
    private NonceReplayService nonceReplayService;

    private ObjectMapper objectMapper;
    private VerifySignatureFilter filter;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        filter = new VerifySignatureFilter(
                merchantProperties,
                verifySigner,
                nonceReplayService,
                objectMapper
        );
    }

    @Test
    void doFilter_skipsNonProtectedRequests() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/transactions");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(merchantProperties, verifySigner, nonceReplayService);
    }

    @Test
    void doFilter_returnsUnauthorized_whenHeadersMissing() throws Exception {
        MockHttpServletRequest request = transactionRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertUnauthorized(response, "Missing required auth headers");
        verify(chain, never()).doFilter(any(), any());
        verifyNoInteractions(merchantProperties, verifySigner, nonceReplayService);
    }

    @Test
    void doFilter_returnsUnauthorized_whenSignatureInvalid() throws Exception {
        MockHttpServletRequest request = transactionRequest();
        addAuthHeaders(
                request,
                "merchant-1",
                Instant.now().toString(),
                "nonce-1",
                "WRONG###1",
                "idem-1"
        );

        stubKnownMerchant();
        when(merchantProperties.getAllowedClockSkew()).thenReturn(Duration.ofMinutes(5));
        when(merchantProperties.isNonceEnforced()).thenReturn(false);
        when(verifySigner.base64(any(byte[].class))).thenReturn("BODY64");
        when(verifySigner.sha256HexUpper(anyString())).thenReturn("HASH");
        when(verifySigner.constantTimeEquals("HASH###1", "WRONG###1")).thenReturn(false);

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertUnauthorized(response, "Invalid signature");
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void doFilter_allowsRequest_whenSignatureValid() throws Exception {
        MockHttpServletRequest request = transactionRequest();
        addAuthHeaders(
                request,
                "merchant-1",
                Instant.now().toString(),
                "nonce-1",
                "HASH###1",
                "idem-1"
        );

        stubKnownMerchant();
        when(merchantProperties.getAllowedClockSkew()).thenReturn(Duration.ofMinutes(5));
        when(merchantProperties.isNonceEnforced()).thenReturn(true);
        when(merchantProperties.getNonceTtl()).thenReturn(Duration.ofMinutes(2));
        when(nonceReplayService.tryAccept("merchant-1", "nonce-1", Duration.ofMinutes(2))).thenReturn(true);
        when(verifySigner.base64(any(byte[].class))).thenReturn("BODY64");
        when(verifySigner.sha256HexUpper(anyString())).thenReturn("HASH");
        when(verifySigner.constantTimeEquals("HASH###1", "HASH###1")).thenReturn(true);

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isNotEqualTo(401);
        verify(chain).doFilter(any(CachedBodyHttpServletRequest.class), eq(response));
    }

    private MockHttpServletRequest transactionRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/transactions");
        request.setContentType("application/json");
        request.setContent("""
                {"amount":100,"currency":"INR"}
                """.getBytes(StandardCharsets.UTF_8));
        return request;
    }

    private void addAuthHeaders(
            MockHttpServletRequest request,
            String merchantId,
            String timestamp,
            String nonce,
            String verify,
            String idempotencyKey
    ) {
        request.addHeader(VerifySignatureFilter.HEADER_MERCHANT_ID, merchantId);
        request.addHeader(VerifySignatureFilter.HEADER_TIMESTAMP, timestamp);
        request.addHeader(VerifySignatureFilter.HEADER_NONCE, nonce);
        request.addHeader(VerifySignatureFilter.HEADER_VERIFY, verify);
        request.addHeader(VerifySignatureFilter.HEADER_IDEMPOTENCY, idempotencyKey);
    }

    private void stubKnownMerchant() {
        MerchantProperties.Merchant merchant = mock(MerchantProperties.Merchant.class);
        when(merchant.getSaltKey()).thenReturn("salt-key");
        when(merchant.getSaltIndex()).thenReturn("1");
        when(merchantProperties.getMerchants()).thenReturn(Map.of("merchant-1", merchant));
    }

    private void assertUnauthorized(MockHttpServletResponse response, String expectedMessage) throws Exception {
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).isEqualTo("application/json");

        JsonNode json = objectMapper.readTree(response.getContentAsString());
        assertThat(json.get("error").asText()).isEqualTo(expectedMessage);
    }
}
