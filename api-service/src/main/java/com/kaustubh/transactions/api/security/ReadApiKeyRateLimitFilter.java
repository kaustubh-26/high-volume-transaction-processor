package com.kaustubh.transactions.api.security;

import java.io.IOException;
import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ReadApiKeyRateLimitFilter extends OncePerRequestFilter {

    public static final String HEADER_API_KEY = "X-API-Key";

    private final ReadAuthConfigService readAuthConfigService;
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;
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
        if (!"GET".equalsIgnoreCase(request.getMethod())
                || !request.getRequestURI().startsWith("/api/v1/transactions/")
                || !request.getRequestURI().endsWith("/status")) {
            filterChain.doFilter(request, response);
            return;
        }

        String apiKey = request.getHeader(HEADER_API_KEY);
        if (apiKey == null || apiKey.isBlank()) {
            writeError(response, HttpStatus.UNAUTHORIZED, "Missing API key");
            return;
        }

        ReadAuthConfigService.ReadAuthConfig config = readAuthConfigService.currentConfig();

        if (!config.isAllowedKey(apiKey)) {
            writeError(response, HttpStatus.UNAUTHORIZED, "Invalid API key");
            return;
        }

        int windowSeconds = config.windowSeconds();
        long window = Instant.now().getEpochSecond() / windowSeconds;
        String redisKey = "ratelimit:read:" + apiKey + ":" + window;

        Long count = redisTemplate.opsForValue().increment(redisKey);
        if (count != null && count == 1L) {
            redisTemplate.expire(redisKey, java.time.Duration.ofSeconds(windowSeconds));
        }

        int limit = config.resolveLimit(apiKey);
        if (count != null && count > limit) {
            writeError(response, HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        objectMapper.writeValue(response.getOutputStream(), new ErrorResponse(message));
    }

    

    private record ErrorResponse(String error) {
    }
}
