package uk.gov.justice.laa.ia.datastore.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import uk.gov.justice.laa.ia.datastore.audit.AuditAction;
import uk.gov.justice.laa.ia.datastore.audit.AuditLogger;

/**
 * Rejects {@code /api/**} requests whose {@code Authorization} or {@code X-Authorization} header is
 * duplicated, comma-joined or otherwise not exactly one {@code Bearer <token>} value. {@code
 * HttpServletRequest.getHeader()} silently returns only the first value of a repeated header, so
 * without this check a second, malformed or injected header value is never seen by {@code
 * UserContextInterceptor} or Spring Security's JWT filter. The rejection response never reveals
 * which of the two headers (or which check) failed. Requests presenting neither header at all are
 * plain unauthenticated requests, not ambiguous ones, so they pass through unchanged and fall to
 * Spring Security's normal 401 handling.
 *
 * <p>Also wraps valid requests to hide inbound identity headers ({@code X-Forwarded-User}, {@code
 * X-Remote-User}, {@code X-User}, {@code X-Auth-Request-*}) from downstream code.
 *
 * <p>Ordered ahead of Spring Security's filter chain (default order -100), following {@link
 * CorrelationIdFilter} and {@link RequestLoggingFilter}, so malformed headers never reach Spring
 * Security's JWT validation or a controller.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "feature.disable-security",
    havingValue = "false",
    matchIfMissing = true)
public class AuthHeaderValidationFilter extends OncePerRequestFilter {

  private static final String API_PATH_PREFIX = "/api/";

  private static final List<String> BEARER_HEADER_NAMES =
      List.of("Authorization", "X-Authorization");

  private static final Pattern SINGLE_BEARER_TOKEN = Pattern.compile("^Bearer [^\\s,]+$");

  private static final String PROBLEM_JSON =
      "{\"type\":\"about:blank\",\"title\":\"Bad Request\",\"status\":400,"
          + "\"detail\":\"Invalid authentication header\"}";

  private final AuditLogger auditLogger;

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !request.getRequestURI().startsWith(request.getContextPath() + API_PATH_PREFIX);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    if (!hasOnlyValidBearerHeaders(request)) {
      auditLogger.security(
          AuditAction.AMBIGUOUS_AUTH_HEADERS,
          AuditLogger.currentActor(),
          "endpoint",
          request.getMethod() + " " + request.getRequestURI(),
          "Rejected: ambiguous or malformed authentication header");
      response.setStatus(HttpStatus.BAD_REQUEST.value());
      response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
      response.getWriter().write(PROBLEM_JSON);
      return;
    }
    filterChain.doFilter(new IdentityHeaderStrippingRequestWrapper(request), response);
  }

  private boolean hasOnlyValidBearerHeaders(HttpServletRequest request) {
    List<List<String>> headerValues =
        BEARER_HEADER_NAMES.stream()
            .<List<String>>map(name -> Collections.list(request.getHeaders(name)))
            .toList();

    // No auth headers at all is a plain unauthenticated request - let Spring Security return 401.
    if (headerValues.stream().allMatch(List::isEmpty)) {
      return true;
    }

    return headerValues.stream().allMatch(AuthHeaderValidationFilter::isExactlyOneValidBearerValue);
  }

  private static boolean isExactlyOneValidBearerValue(List<String> values) {
    return values.size() == 1 && SINGLE_BEARER_TOKEN.matcher(values.get(0)).matches();
  }
}
