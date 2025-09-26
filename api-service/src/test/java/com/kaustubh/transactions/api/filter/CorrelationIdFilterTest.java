package com.kaustubh.transactions.api.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void doFilterInternal_usesProvidedCorrelationId() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "corr-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> correlationIdInChain = new AtomicReference<>();
        FilterChain filterChain = (servletRequest, servletResponse) ->
                correlationIdInChain.set(MDC.get(CorrelationIdFilter.MDC_KEY));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(correlationIdInChain.get()).isEqualTo("corr-123");
        assertThat(response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).isEqualTo("corr-123");
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void doFilterInternal_generatesCorrelationIdWhenHeaderBlank() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "   ");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> correlationIdInChain = new AtomicReference<>();
        FilterChain filterChain = (servletRequest, servletResponse) ->
                correlationIdInChain.set(MDC.get(CorrelationIdFilter.MDC_KEY));

        filter.doFilterInternal(request, response, filterChain);

        String generatedCorrelationId = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);

        assertThat(generatedCorrelationId).isNotBlank();
        assertThat(UUID.fromString(generatedCorrelationId)).isNotNull();
        assertThat(correlationIdInChain.get()).isEqualTo(generatedCorrelationId);
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void doFilterInternal_generatesCorrelationIdWhenHeaderMissing() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> correlationIdInChain = new AtomicReference<>();
        FilterChain filterChain = (servletRequest, servletResponse) ->
                correlationIdInChain.set(MDC.get(CorrelationIdFilter.MDC_KEY));

        filter.doFilterInternal(request, response, filterChain);

        String generatedCorrelationId = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);

        assertThat(generatedCorrelationId).isNotBlank();
        assertThat(UUID.fromString(generatedCorrelationId)).isNotNull();
        assertThat(correlationIdInChain.get()).isEqualTo(generatedCorrelationId);
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }
}
