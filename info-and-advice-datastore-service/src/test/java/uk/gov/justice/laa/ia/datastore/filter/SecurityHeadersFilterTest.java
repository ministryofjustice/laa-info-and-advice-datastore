package uk.gov.justice.laa.ia.datastore.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** Unit tests for {@link SecurityHeadersFilter}. */
class SecurityHeadersFilterTest {

  private final SecurityHeadersFilter filter = new SecurityHeadersFilter();

  @Test
  void shouldAddSecurityHeadersToApiRequests() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v0/applications");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);

    filter.doFilter(request, response, chain);

    assertThat(response.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
    assertThat(response.getHeader("Referrer-Policy")).isEqualTo("no-referrer");
    assertThat(response.getHeader("Strict-Transport-Security"))
        .isEqualTo("max-age=31536000; includeSubDomains");
    assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
    assertThat(response.getHeader("Pragma")).isEqualTo("no-cache");
    verify(chain).doFilter(request, response);
  }

  @Test
  void shouldNotAddHeadersToNonApiRequests() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/swagger-ui/index.html");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);

    filter.doFilter(request, response, chain);

    assertThat(response.getHeader("Cache-Control")).isNull();
    assertThat(response.getHeader("Referrer-Policy")).isNull();
    verify(chain).doFilter(request, response);
  }
}
