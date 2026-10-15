package uk.gov.justice.laa.ia.datastore.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Adds a {@code Content-Security-Policy} header with a {@code report-uri} directive so violations
 * (e.g. on the Swagger UI page) are reported to Sentry. Disabled when {@code laa.security
 * .csp-report-uri} is blank.
 */
@Component
public class ContentSecurityPolicyFilter extends OncePerRequestFilter {

  private final String cspReportUri;

  public ContentSecurityPolicyFilter(
      @Value("${laa.security.csp-report-uri:}") String cspReportUri) {
    this.cspReportUri = cspReportUri;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    if (cspReportUri != null && !cspReportUri.isBlank()) {
      response.setHeader(
          "Content-Security-Policy", "default-src 'self'; report-uri " + cspReportUri);
    }
    filterChain.doFilter(request, response);
  }
}
