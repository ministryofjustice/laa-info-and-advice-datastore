package uk.gov.justice.laa.ia.datastore.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** Unit tests for {@link ReferrerPolicyFilter}. */
class ReferrerPolicyFilterTest {

  private final ReferrerPolicyFilter filter = new ReferrerPolicyFilter();

  @Test
  void shouldAddReferrerPolicyToApiRequests() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v0/applications");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);

    filter.doFilter(request, response, chain);

    assertThat(response.getHeader("Referrer-Policy")).isEqualTo("no-referrer");
    verify(chain).doFilter(request, response);
  }

  @Test
  void shouldNotAddReferrerPolicyToNonApiRequests() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/swagger-ui/index.html");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);

    filter.doFilter(request, response, chain);

    assertThat(response.getHeader("Referrer-Policy")).isNull();
    verify(chain).doFilter(request, response);
  }
}
