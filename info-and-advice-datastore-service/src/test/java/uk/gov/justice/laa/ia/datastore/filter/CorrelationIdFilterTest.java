package uk.gov.justice.laa.ia.datastore.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** Unit tests for {@link CorrelationIdFilter}. */
class CorrelationIdFilterTest {

  private final CorrelationIdFilter filter = new CorrelationIdFilter();

  @AfterEach
  void cleanUpMdc() {
    MDC.clear();
  }

  @Test
  void shouldUseCorrelationIdFromRequestHeader() throws ServletException, IOException {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "test-correlation-id");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);

    filter.doFilterInternal(request, response, filterChain);

    assertThat(response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER))
        .isEqualTo("test-correlation-id");
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void shouldGenerateCorrelationIdWhenHeaderAbsent() throws ServletException, IOException {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);

    filter.doFilterInternal(request, response, filterChain);

    String correlationId = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
    assertThat(correlationId).isNotBlank();
    assertThat(correlationId).matches("[0-9a-f-]{36}"); // UUID format
  }

  @Test
  void shouldGenerateCorrelationIdWhenHeaderIsBlank() throws ServletException, IOException {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "   ");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);

    filter.doFilterInternal(request, response, filterChain);

    String correlationId = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
    assertThat(correlationId).isNotBlank();
    assertThat(correlationId).matches("[0-9a-f-]{36}");
  }

  @Test
  void shouldClearCorrelationIdFromMdcAfterRequest() throws ServletException, IOException {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "test-correlation-id");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);

    filter.doFilterInternal(request, response, filterChain);

    assertThat(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY)).isNull();
  }
}
