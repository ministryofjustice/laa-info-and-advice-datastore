package uk.gov.justice.laa.ia.datastore.config.interceptor;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import uk.gov.justice.laa.ia.datastore.config.RateLimitProperties;

/**
 * Applies an in-memory Resilience4j rate limit per authenticated caller. State is local to this
 * container, so with multiple containers the effective limit is multiplied accordingly.
 */
@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

  private static final String ANONYMOUS_KEY = "anonymous";
  private static final String PROBLEM_JSON =
      "{\"type\":\"about:blank\",\"title\":\"Too Many Requests\",\"status\":429,"
          + "\"detail\":\"Rate limit exceeded\"}";

  private final RateLimitProperties properties;
  private final RateLimiterRegistry registry;

  /**
   * Creates the interceptor.
   *
   * @param properties rate limit settings
   */
  public RateLimitInterceptor(RateLimitProperties properties) {
    this.properties = properties;
    this.registry =
        RateLimiterRegistry.of(
            RateLimiterConfig.custom()
                .limitForPeriod(properties.limitForPeriod())
                .limitRefreshPeriod(properties.refreshPeriod())
                .timeoutDuration(Duration.ZERO)
                .build());
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws IOException {
    if (!properties.enabled()) {
      return true;
    }
    final String key = resolveKey();
    final RateLimiter limiter = registry.rateLimiter(key);
    if (limiter.acquirePermission()) {
      return true;
    }
    final long retryAfterSeconds =
        Math.max(1, (long) Math.ceil(properties.refreshPeriod().toMillis() / 1000.0));
    log.warn("Rate limit exceeded for caller {}", key);
    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response.getWriter().write(PROBLEM_JSON);
    return false;
  }

  private String resolveKey() {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    // Traffic arrives via one upstream service, so limit per end user (oid), not per caller service
    if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
      final String oid = jwtAuthentication.getToken().getClaimAsString("oid");
      if (oid != null && !oid.isBlank()) {
        return oid;
      }
    }
    if (authentication == null || authentication.getName() == null) {
      return ANONYMOUS_KEY;
    }
    return authentication.getName();
  }
}
