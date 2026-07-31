package uk.gov.justice.laa.ia.datastore.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter to log incoming HTTP requests with structured MDC context. Successful (2xx) requests are
 * logged at DEBUG to avoid noise in production. Client errors (4xx) are logged at WARN and server
 * errors (5xx) at ERROR.
 */
@Component
@Order(2)
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

  private static final List<String> REQUEST_MDC_KEYS =
      List.of("method", "path", "statusCode", "durationMs", "clientIp");

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String ipAddress = request.getRemoteAddr();
    String forwardedFor = request.getHeader("X-Forwarded-For");
    if (forwardedFor != null && !forwardedFor.isEmpty()) {
      ipAddress = forwardedFor.split(",")[0].trim();
    }

    String path = request.getRequestURI();
    String method = request.getMethod();

    long startTime = System.currentTimeMillis();
    try {
      filterChain.doFilter(request, response);
    } finally {
      long durationMs = System.currentTimeMillis() - startTime;
      logRequest(method, path, ipAddress, response.getStatus(), durationMs);
    }
  }

  private void logRequest(
      String method, String path, String clientIp, int status, long durationMs) {
    MDC.put("method", method);
    MDC.put("path", path);
    MDC.put("statusCode", String.valueOf(status));
    MDC.put("durationMs", String.valueOf(durationMs));
    MDC.put("clientIp", clientIp);
    try {
      if (status >= 500) {
        log.error("api.request");
      } else if (status >= 400) {
        log.warn("api.request");
      } else {
        log.debug("api.request");
      }
    } finally {
      REQUEST_MDC_KEYS.forEach(MDC::remove);
    }
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return path.startsWith("/actuator")
        || path.startsWith("/swagger-ui")
        || path.startsWith("/v3/api-docs");
  }
}
