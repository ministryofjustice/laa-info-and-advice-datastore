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
 * Adds {@code Referrer-Policy: no-referrer} to {@code /api/**} responses (CTRL-057). The other
 * CTRL-057 headers (nosniff, no-store, HSTS) come from Spring Security's defaults and the ingress.
 *
 * <p>Ordered ahead of Spring Security's filter chain (default order -100) so the header is also
 * present on 401/403/429 responses rejected before reaching a controller.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class ReferrerPolicyFilter extends OncePerRequestFilter {

  private static final String API_PATH_PREFIX = "/api/";

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !request.getRequestURI().startsWith(request.getContextPath() + API_PATH_PREFIX);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    response.setHeader("Referrer-Policy", "no-referrer");
    filterChain.doFilter(request, response);
  }
}
