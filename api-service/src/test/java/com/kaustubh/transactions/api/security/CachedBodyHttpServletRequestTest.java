package com.kaustubh.transactions.api.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

@Tag("unit")
class CachedBodyHttpServletRequestTest {

    @Test
    void cachedBody_isReusableAcrossReaders() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent("hello".getBytes(StandardCharsets.UTF_8));

        CachedBodyHttpServletRequest wrapped = new CachedBodyHttpServletRequest(request);

        assertThat(wrapped.getCachedBody()).containsExactly("hello".getBytes(StandardCharsets.UTF_8));

        String inputStreamBody = new String(wrapped.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertThat(inputStreamBody).isEqualTo("hello");

        String readerBody = wrapped.getReader().readLine();
        assertThat(readerBody).isEqualTo("hello");
    }
}
