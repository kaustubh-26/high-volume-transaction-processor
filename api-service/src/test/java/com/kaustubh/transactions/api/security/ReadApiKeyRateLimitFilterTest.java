package com.kaustubh.transactions.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaustubh.transactions.api.security.ReadAuthConfigService.ReadAuthConfig;

import jakarta.servlet.FilterChain;

class ReadApiKeyRateLimitFilterTest {

    @Test
    void missingApiKey_returnsUnauthorized() throws Exception {
        ReadAuthConfigService configService = Mockito.mock(ReadAuthConfigService.class);
        StringRedisTemplate redisTemplate = Mockito.mock(StringRedisTemplate.class);
        ReadApiKeyRateLimitFilter filter = new ReadApiKeyRateLimitFilter(
                configService,
                redisTemplate,
                new ObjectMapper()
        );

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/transactions/tx-1/status");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = Mockito.mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void invalidApiKey_returnsUnauthorized() throws Exception {
        ReadAuthConfig config = new ReadAuthConfig(
                java.util.Set.of("key-1"),
                java.util.Map.of(),
                java.util.Map.of("key-1", java.util.Set.of("merchant-1")),
                60,
                60
        );
        ReadAuthConfigService configService = Mockito.mock(ReadAuthConfigService.class);
        when(configService.currentConfig()).thenReturn(config);
        StringRedisTemplate redisTemplate = Mockito.mock(StringRedisTemplate.class);
        ReadApiKeyRateLimitFilter filter = new ReadApiKeyRateLimitFilter(
                configService,
                redisTemplate,
                new ObjectMapper()
        );

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/transactions/tx-1/status");
        request.addHeader(ReadApiKeyRateLimitFilter.HEADER_API_KEY, "bad-key");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = Mockito.mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void rateLimitExceeded_returnsTooManyRequests() throws Exception {
        ReadAuthConfig config = new ReadAuthConfig(
                java.util.Set.of("key-1"),
                java.util.Map.of("key-1", 1),
                java.util.Map.of("key-1", java.util.Set.of("merchant-1")),
                60,
                60
        );
        ReadAuthConfigService configService = Mockito.mock(ReadAuthConfigService.class);
        when(configService.currentConfig()).thenReturn(config);

        StringRedisTemplate redisTemplate = Mockito.mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOps = Mockito.mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(Mockito.anyString())).thenReturn(2L);

        ReadApiKeyRateLimitFilter filter = new ReadApiKeyRateLimitFilter(
                configService,
                redisTemplate,
                new ObjectMapper()
        );

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/transactions/tx-1/status");
        request.addHeader(ReadApiKeyRateLimitFilter.HEADER_API_KEY, "key-1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = Mockito.mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(429);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void validApiKey_underLimit_allowsRequest() throws Exception {
        ReadAuthConfig config = new ReadAuthConfig(
                java.util.Set.of("key-1"),
                java.util.Map.of("key-1", 2),
                java.util.Map.of("key-1", java.util.Set.of("merchant-1")),
                60,
                60
        );
        ReadAuthConfigService configService = Mockito.mock(ReadAuthConfigService.class);
        when(configService.currentConfig()).thenReturn(config);

        StringRedisTemplate redisTemplate = Mockito.mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOps = Mockito.mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(Mockito.anyString())).thenReturn(1L);
        when(redisTemplate.expire(Mockito.anyString(), Mockito.any(Duration.class))).thenReturn(true);

        ReadApiKeyRateLimitFilter filter = new ReadApiKeyRateLimitFilter(
                configService,
                redisTemplate,
                new ObjectMapper()
        );

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/transactions/tx-1/status");
        request.addHeader(ReadApiKeyRateLimitFilter.HEADER_API_KEY, "key-1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = Mockito.mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        verify(chain).doFilter(request, response);
    }

    @Test
    void nonStatusRequests_areBypassed() throws Exception {
        ReadAuthConfigService configService = Mockito.mock(ReadAuthConfigService.class);
        StringRedisTemplate redisTemplate = Mockito.mock(StringRedisTemplate.class);
        ReadApiKeyRateLimitFilter filter = new ReadApiKeyRateLimitFilter(
                configService,
                redisTemplate,
                new ObjectMapper()
        );

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/transactions");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = Mockito.mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(configService, redisTemplate);
    }
}
