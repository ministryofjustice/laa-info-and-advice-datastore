package uk.gov.justice.laa.ia.datastore.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import uk.gov.justice.laa.ia.datastore.audit.AuditAction;
import uk.gov.justice.laa.ia.datastore.audit.AuditLogger;

/** Unit tests for {@link AuthHeaderValidationFilter}. */
class AuthHeaderValidationFilterTest {

  private final AuditLogger auditLogger = mock(AuditLogger.class);
  private final AuthHeaderValidationFilter filter = new AuthHeaderValidationFilter(auditLogger);

  @Test
  void shouldNotFilter_shouldReturnTrue_forNonApiPaths() {
    assertThat(filter.shouldNotFilter(createRequestWithUri("/actuator/health"))).isTrue();
    assertThat(filter.shouldNotFilter(createRequestWithUri("/swagger-ui/index.html"))).isTrue();
  }

  @Test
  void shouldNotFilter_shouldReturnFalse_forApiPaths() {
    assertThat(filter.shouldNotFilter(createRequestWithUri("/api/v0/applications"))).isFalse();
  }

  @Test
  void doFilterInternal_shouldAllowSingleValidBearerTokenOnBothHeaders() throws Exception {
    MockHttpServletRequest request = createRequestWithUri("/api/v0/applications");
    request.addHeader("Authorization", "Bearer service-token");
    request.addHeader("X-Authorization", "Bearer user-token");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(any(), eq(response));
    verify(auditLogger, never()).security(any(), any(), any(), any(), any());
    assertThat(response.getStatus()).isEqualTo(200);
  }

  @Test
  void doFilterInternal_shouldRejectDuplicateAuthorizationHeaders() throws Exception {
    MockHttpServletRequest request = createRequestWithUri("/api/v0/applications");
    request.addHeader("Authorization", "Bearer token-one");
    request.addHeader("Authorization", "Bearer token-two");
    request.addHeader("X-Authorization", "Bearer user-token");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);

    filter.doFilterInternal(request, response, filterChain);

    assertRejected(response, filterChain);
  }

  @Test
  void doFilterInternal_shouldRejectDuplicate_xAuthorizationHeaders() throws Exception {
    MockHttpServletRequest request = createRequestWithUri("/api/v0/applications");
    request.addHeader("Authorization", "Bearer service-token");
    request.addHeader("X-Authorization", "Bearer token-one");
    request.addHeader("X-Authorization", "Bearer token-two");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);

    filter.doFilterInternal(request, response, filterChain);

    assertRejected(response, filterChain);
  }

  @Test
  void doFilterInternal_shouldRejectCommaJoinedAuthorizationHeader() throws Exception {
    MockHttpServletRequest request = createRequestWithUri("/api/v0/applications");
    request.addHeader("Authorization", "Bearer token-one, Bearer token-two");
    request.addHeader("X-Authorization", "Bearer user-token");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);

    filter.doFilterInternal(request, response, filterChain);

    assertRejected(response, filterChain);
  }

  @Test
  void doFilterInternal_shouldRejectMultiTokenWhitespaceValue() throws Exception {
    MockHttpServletRequest request = createRequestWithUri("/api/v0/applications");
    request.addHeader("Authorization", "Bearer service-token");
    request.addHeader("X-Authorization", "Bearer token-one token-two");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);

    filter.doFilterInternal(request, response, filterChain);

    assertRejected(response, filterChain);
  }

  @Test
  void doFilterInternal_shouldRejectMixedCaseDuplicateHeaderName() throws Exception {
    MockHttpServletRequest request = createRequestWithUri("/api/v0/applications");
    request.addHeader("authorization", "Bearer token-one");
    request.addHeader("AUTHORIZATION", "Bearer token-two");
    request.addHeader("X-Authorization", "Bearer user-token");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);

    filter.doFilterInternal(request, response, filterChain);

    assertRejected(response, filterChain);
  }

  @Test
  void doFilterInternal_shouldAllowRequestWithNoAuthHeadersAtAll() throws Exception {
    MockHttpServletRequest request = createRequestWithUri("/api/v0/applications");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(any(), eq(response));
    verify(auditLogger, never()).security(any(), any(), any(), any(), any());
  }

  @Test
  void doFilterInternal_shouldRejectMissingHeader() throws Exception {
    MockHttpServletRequest request = createRequestWithUri("/api/v0/applications");
    request.addHeader("Authorization", "Bearer service-token");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);

    filter.doFilterInternal(request, response, filterChain);

    assertRejected(response, filterChain);
  }

  @Test
  void doFilterInternal_shouldNotRevealWhichHeaderFailed() throws Exception {
    MockHttpServletRequest request = createRequestWithUri("/api/v0/applications");
    request.addHeader("Authorization", "Bearer token-one");
    request.addHeader("Authorization", "Bearer token-two");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);

    filter.doFilterInternal(request, response, filterChain);

    assertThat(response.getContentAsString())
        .doesNotContain("Authorization")
        .doesNotContain("X-Authorization")
        .doesNotContain("token-one");
  }

  @Test
  void doFilterInternal_shouldStripIdentityHeadersFromWrappedRequest() throws Exception {
    MockHttpServletRequest request = createRequestWithUri("/api/v0/applications");
    request.addHeader("Authorization", "Bearer service-token");
    request.addHeader("X-Authorization", "Bearer user-token");
    request.addHeader("X-Forwarded-User", "someone");
    MockHttpServletResponse response = new MockHttpServletResponse();
    String[] seenForwardedUser = new String[1];
    FilterChain filterChain =
        (req, res) ->
            seenForwardedUser[0] = ((HttpServletRequest) req).getHeader("X-Forwarded-User");

    filter.doFilterInternal(request, response, filterChain);

    assertThat(seenForwardedUser[0]).isNull();
  }

  private void assertRejected(MockHttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    assertThat(response.getStatus()).isEqualTo(400);
    assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    assertThat(response.getContentAsString()).contains("\"status\":400");
    verify(filterChain, never()).doFilter(any(), any());
    verify(auditLogger)
        .security(eq(AuditAction.AMBIGUOUS_AUTH_HEADERS), any(), eq("endpoint"), any(), any());
  }

  private MockHttpServletRequest createRequestWithUri(String uri) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI(uri);
    return request;
  }
}
