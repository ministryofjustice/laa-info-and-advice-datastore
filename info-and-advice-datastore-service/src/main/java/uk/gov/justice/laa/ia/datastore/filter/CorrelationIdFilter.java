package uk.gov.justice.laa.ia.datastore.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter that reads the {@code X-Correlation-ID} header set by the NGINX ingress controller and
 * places it in the SLF4J MDC so it appears in every structured log entry for the request. If no
 * header is present, a UUID is generated. The header is also echoed back in the response.
 */
@Component
@Order(1)
public class CorrelationIdFilter extends OncePerRequestFilter {

  static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
  static final String CORRELATION_ID_MDC_KEY = "correlationId";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String correlationId = request.getHeader(CORRELATION_ID_HEADER);
    if (correlationId == null || correlationId.isBlank()) {
      correlationId = UUID.randomUUID().toString();
    }

    MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
    response.setHeader(CORRELATION_ID_HEADER, correlationId);

    try {
      filterChain.doFilter(request, response);
    } finally {
      MDC.remove(CORRELATION_ID_MDC_KEY);
    }
  }
}
