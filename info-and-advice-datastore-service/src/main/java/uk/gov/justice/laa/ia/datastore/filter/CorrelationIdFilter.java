package uk.gov.justice.laa.ia.datastore.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter that reads the {@code X-Correlation-ID} header set by the NGINX ingress controller and
 * places it in the SLF4J MDC so it appears in every structured log entry for the request. If no
 * header is present, a UUID is generated. The header is also echoed back in the response.
 *
 * <p>If a valid W3C {@code traceparent} header is present, its trace and parent span IDs are placed
 * in the MDC as {@code trace.id}/{@code span.id} (nested by the ECS formatter as the standard ECS
 * trace fields) so audit entries can be joined to distributed traces.
 *
 * <p>Ordered ahead of Spring Security's filter chain (default order -100) so the correlation ID is
 * present in logs even when a request is rejected by security before reaching a controller.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

  static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
  static final String CORRELATION_ID_MDC_KEY = "correlationId";
  static final String SERVICE_NAME_HEADER = "X-Service-Name";
  static final String TRACEPARENT_HEADER = "traceparent";
  static final String TRACE_ID_MDC_KEY = "trace.id";
  static final String SPAN_ID_MDC_KEY = "span.id";

  private static final Pattern TRACEPARENT =
      Pattern.compile("^[0-9a-f]{2}-([0-9a-f]{32})-([0-9a-f]{16})-[0-9a-f]{2}$");

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String correlationId = request.getHeader(CORRELATION_ID_HEADER);
    if (correlationId == null || correlationId.isBlank()) {
      correlationId = UUID.randomUUID().toString();
    }
    String serviceName = request.getHeader(SERVICE_NAME_HEADER);
    if (serviceName == null || serviceName.isBlank()) {
      serviceName = "unknown-service";
    }
    MDC.put(SERVICE_NAME_HEADER, serviceName);
    MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
    response.setHeader(CORRELATION_ID_HEADER, correlationId);

    Matcher trace = TRACEPARENT.matcher(String.valueOf(request.getHeader(TRACEPARENT_HEADER)));
    if (trace.matches() && !trace.group(1).matches("0+") && !trace.group(2).matches("0+")) {
      MDC.put(TRACE_ID_MDC_KEY, trace.group(1));
      MDC.put(SPAN_ID_MDC_KEY, trace.group(2));
    }

    try {
      filterChain.doFilter(request, response);
    } finally {
      MDC.remove(CORRELATION_ID_MDC_KEY);
      MDC.remove(SERVICE_NAME_HEADER);
      MDC.remove(TRACE_ID_MDC_KEY);
      MDC.remove(SPAN_ID_MDC_KEY);
    }
  }
}
