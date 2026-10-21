package uk.gov.justice.laa.ia.datastore.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import uk.gov.justice.laa.ia.datastore.audit.AuditAction;
import uk.gov.justice.laa.ia.datastore.audit.AuditLogger;

/** Unit tests for {@link RequestLoggingFilter}. */
class RequestLoggingFilterTest {

  private final AuditLogger auditLogger = mock(AuditLogger.class);
  private final RequestLoggingFilter filter = new RequestLoggingFilter(auditLogger);

  @Test
  void shouldNotFilter_shouldReturnTrue_forExcludePaths() {
    assertThat(filter.shouldNotFilter(createRequestWithUri("/actuator/health"))).isTrue();
    assertThat(filter.shouldNotFilter(createRequestWithUri("/actuator/info"))).isTrue();
    assertThat(filter.shouldNotFilter(createRequestWithUri("/swagger-ui/index.html"))).isTrue();
    assertThat(filter.shouldNotFilter(createRequestWithUri("/v3/api-docs"))).isTrue();
  }

  @Test
  void shouldNotFilter_shouldReturnFalse_forApiPaths() {
    assertThat(filter.shouldNotFilter(createRequestWithUri("/api/v0/applications"))).isFalse();
    assertThat(filter.shouldNotFilter(createRequestWithUri("/api/v0/applications/123"))).isFalse();
  }

  @Test
  void doFilterInternal_shouldExecuteFilterChain() throws ServletException, java.io.IOException {
    // Arrange
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v0/applications");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);

    // Act
    filter.doFilterInternal(request, response, filterChain);

    // Assert
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void doFilterInternal_shouldHandleForwardedForHeader()
      throws ServletException, java.io.IOException {
    // Arrange
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v0/applications");
    request.addHeader("X-Forwarded-For", "203.0.113.195, 70.41.3.18, 150.172.238.178");
    request.setRemoteAddr("10.0.0.1");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);

    // Act
    filter.doFilterInternal(request, response, filterChain);

    // Assert
    verify(filterChain).doFilter(request, response);
  }

  /**
   * Helper to create a request with a URI path.
   *
   * @param uri the request URI path
   * @return MockHttpServletRequest
   */
  private MockHttpServletRequest createRequestWithUri(String uri) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI(uri);
    return request;
  }

  @Test
  void doFilterInternal_shouldAuditUnauditedUnauthorizedResponse() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v0/applications");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilterInternal(request, response, (req, res) -> response.setStatus(401));

    verify(auditLogger)
        .security(
            eq(AuditAction.TOKEN_VERIFICATION_FAILED),
            any(),
            eq("endpoint"),
            eq("GET /api/v0/applications"),
            any());
  }

  @Test
  void doFilterInternal_shouldNotDoubleAuditWhenAlreadyAudited() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v0/applications");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilterInternal(
        request,
        response,
        (req, res) -> {
          req.setAttribute(AuditLogger.SECURITY_AUDITED_ATTRIBUTE, Boolean.TRUE);
          response.setStatus(403);
        });

    verify(auditLogger, never()).security(any(), any(), any(), any(), any());
  }
}
