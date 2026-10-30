package uk.gov.justice.laa.ia.datastore.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Adds security response headers to {@code /api/**} responses (CTRL-057): {@code
 * X-Content-Type-Options}, {@code Referrer-Policy}, {@code Strict-Transport-Security} and {@code
 * Cache-Control: no-store} so authenticated data is never cached. Swagger UI and actuator responses
 * are left alone.
 *
 * <p>Ordered ahead of Spring Security's filter chain (default order -100) so the headers are also
 * present on 401/403/429 responses rejected before reaching a controller.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class SecurityHeadersFilter extends OncePerRequestFilter {

  private static final String API_PATH_PREFIX = "/api/";

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !request.getRequestURI().startsWith(request.getContextPath() + API_PATH_PREFIX);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    response.setHeader("X-Content-Type-Options", "nosniff");
    response.setHeader("Referrer-Policy", "no-referrer");
    response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
    response.setHeader("Cache-Control", "no-store");
    response.setHeader("Pragma", "no-cache");
    filterChain.doFilter(request, response);
  }
}
