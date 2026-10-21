package uk.gov.justice.laa.ia.datastore.config.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import uk.gov.justice.laa.ia.datastore.audit.AuditAction;
import uk.gov.justice.laa.ia.datastore.audit.AuditLogger;
import uk.gov.justice.laa.ia.datastore.config.RateLimitProperties;

/** Tests for the in-memory rate limit interceptor. */
class RateLimitInterceptorTests {

  private final MockHttpServletRequest request = new MockHttpServletRequest();

  private final AuditLogger auditLogger = mock(AuditLogger.class);

  private RateLimitInterceptor interceptor(boolean enabled, int limit) {
    return new RateLimitInterceptor(
        new RateLimitProperties(enabled, limit, Duration.ofMinutes(1)), auditLogger);
  }

  private void authenticateAs(String name) {
    SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(name, "x"));
  }

  private void authenticateAsJwt(String sub, String oid) {
    Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject(sub).claim("oid", oid).build();
    SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void allowsRequestsUpToLimitThenReturns429() throws Exception {
    RateLimitInterceptor interceptor = interceptor(true, 2);
    authenticateAs("caller-a");

    assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), null)).isTrue();
    assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), null)).isTrue();

    MockHttpServletResponse blocked = new MockHttpServletResponse();
    assertThat(interceptor.preHandle(request, blocked, null)).isFalse();
    assertThat(blocked.getStatus()).isEqualTo(429);
    assertThat(blocked.getHeader("Retry-After")).isEqualTo("60");
    assertThat(blocked.getContentType()).startsWith("application/problem+json");
    assertThat(blocked.getContentAsString()).contains("Rate limit exceeded");
    verify(auditLogger)
        .security(
            eq(AuditAction.RATE_LIMIT_EXCEEDED),
            eq("caller-a"),
            eq("endpoint"),
            any(),
            eq("Rate limit exceeded"));
  }

  @Test
  void limitsAreTrackedPerCaller() throws Exception {
    RateLimitInterceptor interceptor = interceptor(true, 1);

    authenticateAs("caller-a");
    assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), null)).isTrue();
    assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), null)).isFalse();

    authenticateAs("caller-b");
    assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), null)).isTrue();
  }

  @Test
  void limitsPerUserEvenWhenTokensShareSubject() throws Exception {
    RateLimitInterceptor interceptor = interceptor(true, 1);

    authenticateAsJwt("shared-service-sub", "user-1");
    assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), null)).isTrue();
    assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), null)).isFalse();

    authenticateAsJwt("shared-service-sub", "user-2");
    assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), null)).isTrue();
  }

  @Test
  void doesNothingWhenDisabled() throws Exception {
    RateLimitInterceptor interceptor = interceptor(false, 1);
    authenticateAs("caller-a");

    for (int i = 0; i < 5; i++) {
      assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), null)).isTrue();
    }
  }
}
